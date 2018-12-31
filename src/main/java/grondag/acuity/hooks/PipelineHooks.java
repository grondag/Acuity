/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.hooks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import grondag.acuity.Acuity;
import grondag.acuity.Configurator;
import grondag.acuity.api.BlockVertexProvider;
import grondag.acuity.api.PipelineManagerImpl;
import grondag.acuity.api.RenderPipelineImpl;
import grondag.acuity.buffering.DrawableChunk.Solid;
import grondag.acuity.buffering.DrawableChunk.Translucent;
import grondag.acuity.core.CompoundBufferBuilder;
import grondag.acuity.core.CompoundVertexLighter;
import grondag.acuity.core.FluidBuilder;
import grondag.acuity.core.VanillaQuadWrapper;
import grondag.acuity.mixin.extension.ChunkRendererExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;

@Environment(EnvType.CLIENT)
public class PipelineHooks
{
    // these have to be somewhere other than the static initialize for EnumFacing/mixins thereof
    public static final Direction[] HORIZONTAL_FACES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public static final Direction[] VERTICAL_FACES = {Direction.UP, Direction.DOWN};
    
    private static ThreadLocal<CompoundVertexLighter> lighters;
    private static ThreadLocal<FluidBuilder> fluidBuilders;
    
    static
    {
        createLighters();
    }
    
    private static void createLighters()
    {
        lighters = new ThreadLocal<CompoundVertexLighter>()
        {
            @Override
            protected CompoundVertexLighter initialValue()
            {
                return Configurator.lightingModel.createLighter();
            }
        };
        
        fluidBuilders = new ThreadLocal<FluidBuilder>()
        {
            @Override
            protected FluidBuilder initialValue()
            {
                return new FluidBuilder();
            }
        };
    }
    
    private static final ThreadLocal<VanillaQuadWrapper> quadWrappers = new ThreadLocal<VanillaQuadWrapper>()
    {
        @Override
        protected VanillaQuadWrapper initialValue()
        {
            return new VanillaQuadWrapper();
        }
    };
    
    public static void forceReload()
    {
        createLighters(); 
    }
    
    private static boolean didWarnUnhandledFluid = false;
    
    public static void linkBuilders(BlockLayeredBufferBuilder cache)
    {
        linkBuildersInner(cache, BlockRenderLayer.SOLID);
        linkBuildersInner(cache, BlockRenderLayer.CUTOUT);
        linkBuildersInner(cache, BlockRenderLayer.MIPPED_CUTOUT);
        linkBuildersInner(cache, BlockRenderLayer.TRANSLUCENT);
    }
    
    private static void linkBuildersInner(BlockLayeredBufferBuilder cache, BlockRenderLayer layer)
    {
        CompoundBufferBuilder builder = (CompoundBufferBuilder) cache.get(layer);
        builder.setupLinks(cache, layer);
    }
    
    /**
     * When mod is enabled, cutout layers are packed into solid layer, but the
     * chunk render dispatcher doesn't know this and sets flags in the compiled chunk
     * as if the cutout buffers were populated.  We use this hook to correct that
     * so that uploader and rendering work in subsequent operations.<p>
     * 
     * Called from the rebuildChunk method in RenderChunk, via a redirect on the call to
     * {@link CompiledChunk#setVisibility(net.minecraft.client.renderer.chunk.SetVisibility)}
     * which is reliably called after the chunks are built in render chunk.<p>
     */
    public static void mergeRenderLayers(ChunkRenderData compiledChunk)
    {
        if(Acuity.isModEnabled())
        {
            mergeLayerFlags(compiledChunk.layersStarted);
            mergeLayerFlags(compiledChunk.layersUsed);
        }
    }
    
    private static void mergeLayerFlags(boolean[] layerFlags)
    {
        layerFlags[0]  = layerFlags[0] || layerFlags[1] || layerFlags[2];
        layerFlags[1] = false;
        layerFlags[2] = false;
    }
    
    private static AtomicInteger fluidModelCount = new AtomicInteger();
    private static LongAccumulator fluidModelNanos = new LongAccumulator((l, r) -> l + r, 0);
    
    /**
     * Performance counting version of {@link #renderFluid(BlockFluidRenderer, IBlockAccess, IBlockState, BlockPos, BufferBuilder)}
     */
    public static boolean renderFluidDebug(FluidRenderer fluidRenderer, ExtendedBlockView blockAccess, FluidState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
    {
        final long start = System.nanoTime();
        final boolean result = renderFluid(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
        fluidModelNanos.accumulate(System.nanoTime() - start);
        if(fluidModelCount.incrementAndGet() == 50000)
        {
            // could misalign one or two samples but close enough
            long total = fluidModelNanos.getThenReset();
            fluidModelCount.set(0);
            
            Acuity.INSTANCE.getLog().info(String.format("Average ns per fluid model rebuild (Acuity %s) = %f", 
                    Acuity.isModEnabled() ? "Enabled" : "Disabled",
                    total / 50000.0));
        }
        return result;
    }
    
    /**
     * Handles vanilla special-case rendering for lava and water.
     * Forge fluids should come as block models instead.
     */
    public static boolean renderFluid(FluidRenderer fluidRenderer, ExtendedBlockView blockAccess, FluidState fluidStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
    {
        if(Acuity.isModEnabled())
        {
            RenderPipelineImpl target;
            if(blockStateIn.getMaterial() == Material.LAVA)
            {
                target = PipelineManagerImpl.INSTANCE.getLavaPipeline();
            }
            else
            {
                if(!didWarnUnhandledFluid && blockStateIn.getMaterial() != Material.WATER)
                {
                    Acuity.INSTANCE.getLog().warn(I18n.translateToLocal("misc.warn_unknown_fluid_render"));
                    didWarnUnhandledFluid = true;
                }
                target = PipelineManagerImpl.INSTANCE.getWaterPipeline();
            }
            final CompoundVertexLighter lighter = lighters.get();
            lighter.prepare((CompoundBufferBuilder)bufferBuilderIn, MinecraftForgeClient.getRenderLayer(), blockAccess, blockStateIn, blockPosIn, false);
            return fluidRenderer.renderFluid(blockAccess, blockStateIn, blockPosIn, fluidBuilders.get().prepare(target, lighter));
        }
        else
            return fluidRenderer.renderFluid(blockAccess, blockPosIn, bufferBuilderIn, fluidStateIn);
    }

    private static AtomicInteger blockModelCount = new AtomicInteger();
    private static LongAccumulator blockModelNanos = new LongAccumulator((l, r) -> l + r, 0);
    
    /**
     * Performance counting version of #
     */
    public static boolean renderModelDebug(BlockModelRenderer blockModelRenderer, ExtendedBlockView blockAccess, BakedModel model, 
            BlockState state, BlockPos pos, BufferBuilder bufferBuilderIn, boolean checkSides)
    {
        final long start = System.nanoTime();
        final boolean result = renderModel(blockModelRenderer, blockAccess, model, state, pos, bufferBuilderIn, checkSides);
        blockModelNanos.accumulate(System.nanoTime() - start);
        if(blockModelCount.incrementAndGet() == 500000)
        {
            // could misalign one or two samples but close enough
            long total = blockModelNanos.getThenReset();
            blockModelCount.set(0);
            
            Acuity.INSTANCE.getLog().info(String.format("Average ns per block model rebuild (Acuity %s) = %f", 
                    Acuity.isModEnabled() ? "Enabled" : "Disabled",
                    total / 500000.0));
        }
        return result;
    }
    
    public static boolean renderModel(BlockModelRenderer blockModelRenderer, ExtendedBlockView blockAccess, BakedModel model, BlockState state, BlockPos pos,
            BufferBuilder bufferBuilderIn, boolean checkSides)
    {
        if(Acuity.isModEnabled())
        {
            if(model instanceof BlockVertexProvider)
                return renderModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
            else
                return renderVanillaModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
        }
        else
            return blockModelRenderer.renderModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
    }
    
    private static boolean renderModel(ExtendedBlockView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, BufferBuilder bufferIn, boolean checkSides)
    {
        try
        {
            final BlockVertexProvider model = (BlockVertexProvider)modelIn;
            final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            if(!model.mightRenderInLayer(layer)) 
                return false;

            final CompoundVertexLighter lighter = lighters.get();
            lighter.prepare((CompoundBufferBuilder)bufferIn, layer, worldIn, (IExtendedBlockState) stateIn, posIn, checkSides);
            model.produceQuads(lighter);
            return lighter.didOutput();
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block model (Acuity pipelined render)");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block model being tesselated");
            CrashReportCategory.addBlockInfo(crashreportcategory, posIn, stateIn);
            throw new ReportedException(crashreport);
        }
    }

    private static boolean renderVanillaModel(ExtendedBlockView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, BufferBuilder bufferIn, boolean checkSides)
    {
        try
        {
            final CompoundVertexLighter lighter = lighters.get();
            final VanillaQuadWrapper wrapper = quadWrappers.get();
            final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            
            lighter.prepare((CompoundBufferBuilder)bufferIn, layer, worldIn, stateIn, posIn, checkSides);
            wrapper.prepare(layer, modelIn.isAmbientOcclusion() && stateIn.getLightValue(worldIn, posIn) == 0);
            
            renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, null);
            
            if(checkSides)
            {
                if(stateIn.shouldSideBeRendered(worldIn, posIn, Direction.DOWN))
                        renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.DOWN);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, Direction.UP))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.UP);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, Direction.EAST))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.EAST);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, Direction.WEST))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.WEST);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, Direction.NORTH))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.NORTH);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, Direction.SOUTH))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.SOUTH);
            }
            else
            {
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.DOWN);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.UP);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.EAST);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.WEST);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.NORTH);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, Direction.SOUTH);
            }
            
            return lighter.didOutput();
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block model (Acuity pipelined vanilla render)");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block model being tesselated");
            CrashReportCategory.addBlockInfo(crashreportcategory, posIn, stateIn);
            throw new ReportedException(crashreport);
        }
    }
    
    private static void renderVanillaModelInner(BakedModel modelIn, BlockState stateIn, CompoundVertexLighter lighter, VanillaQuadWrapper wrapper, Direction face)
    {
        List<BakedQuad> quads = modelIn.getQuads(stateIn, face, lighter.positionRandom());
        final int limit = quads.size();
        
        if(limit == 0)
            return;
        
        for(int i = 0; i < limit; i ++)
            wrapper.wrapAndLight(lighter, quads.get(i));
    }
    
    public static boolean isFirstOrUV(int index, VertexFormatElement.Type usage)
    {
        // has to apply even when mod is disabled so that our formats can be instantiated
        return index == 0 || usage == VertexFormatElement.Type.UV || usage == VertexFormatElement.Type.GENERIC;
    }

    /**
     * When Acuity is enabled the per-chunk matrix is never used, so is wasteful to update when frustum moves.
     * Matters more when lots of block updates or other high-throughput because adds to contention.
     */
    public static void renderChunkInitModelViewMatrix(ChunkRenderer renderChunk)
    {
        if(Acuity.isModEnabled())
        {
            // this is called right after setting chunk position because it was moved in the frustum
            // let buffers in the chunk know they are no longer valid and can be released.
            ((ChunkRendererExt)renderChunk).releaseDrawables();
        }
        else
            renderChunk.applyMatrix();
    }

    public static boolean shouldUploadLayer(ChunkRenderData compiledchunk, BlockRenderLayer blockrenderlayer)
    {
        return Acuity.isModEnabled()
            ? compiledchunk.isInitialized(blockrenderlayer) && !compiledchunk.isEmpty(blockrenderlayer)
            : compiledchunk.isInitialized(blockrenderlayer);
    }
    
    public static int renderBlockLayer(RenderGlobal renderGlobal, BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn)
    {
        if(Acuity.isModEnabled())
        {
            switch(blockLayerIn)
            {
                case SOLID:
                case TRANSLUCENT:
                    return renderGlobal.renderBlockLayer(blockLayerIn, partialTicks, pass, entityIn);
                    
                case CUTOUT:
                case MIPPED_CUTOUT:
                default: 
                    return 0;
            }
        }
        else
            return renderGlobal.renderBlockLayer(blockLayerIn, partialTicks, pass, entityIn);
    }

    @SuppressWarnings("null")
    public static ListenableFuture<Object> uploadChunk(ChunkRenderDispatcher chunkRenderDispatcher, BlockRenderLayer blockRenderLayer,
            BufferBuilder bufferBuilder, ChunkRenderer renderChunk, ChunkRenderData compiledChunk, double distanceSq)
    {
        assert blockRenderLayer == BlockRenderLayer.SOLID || blockRenderLayer == BlockRenderLayer.TRANSLUCENT;
        
        if (MinecraftClient.getInstance().isMainThread())
        {
            if(blockRenderLayer == BlockRenderLayer.SOLID)
                ((ChunkRendererExt)renderChunk).setSolidDrawable((Solid) ((CompoundBufferBuilder)bufferBuilder).produceDrawable());
            else
                ((ChunkRendererExt)renderChunk).setTranslucentDrawable((Translucent) ((CompoundBufferBuilder)bufferBuilder).produceDrawable());
            
            bufferBuilder.setOffset(0.0D, 0.0D, 0.0D);
            return Futures.<Object>immediateFuture((Object)null);
        }
        else
        {
            ListenableFutureTask<Object> listenablefuturetask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    uploadChunk(chunkRenderDispatcher, blockRenderLayer, bufferBuilder, renderChunk,
                            compiledChunk, distanceSq);
                }
            }, (Object)null);

            synchronized (chunkRenderDispatcher.queueChunkUploads)
            {
                chunkRenderDispatcher.queueChunkUploads.add(chunkRenderDispatcher.new PendingUpload(listenablefuturetask, distanceSq));
                return listenablefuturetask;
            }
        }
    }
}
