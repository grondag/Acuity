package grondag.acuity.hooks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;

import grondag.acuity.Acuity;
import grondag.acuity.Configurator;
import grondag.acuity.api.IPipelinedBakedModel;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.buffering.IDrawableChunk;
import grondag.acuity.core.CompoundBufferBuilder;
import grondag.acuity.core.CompoundVertexLighter;
import grondag.acuity.core.FluidBuilder;
import grondag.acuity.core.VanillaQuadWrapper;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelineHooks
{
    @SuppressWarnings("null")
    private static ThreadLocal<CompoundVertexLighter> lighters;
    @SuppressWarnings("null")
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
    
    public static void linkBuilders(RegionRenderCacheBuilder cache)
    {
        for(BlockRenderLayer layer : BlockRenderLayer.values())
        {
            CompoundBufferBuilder builder = (CompoundBufferBuilder) cache.getWorldRendererByLayer(layer);
            builder.setupLinks(cache, layer);
        }
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
    public static void mergeRenderLayers(CompiledChunk compiledChunk)
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
    public static boolean renderFluidDebug(BlockFluidRenderer fluidRenderer, IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
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
    public static boolean renderFluid(BlockFluidRenderer fluidRenderer, IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
    {
        if(Acuity.isModEnabled())
        {
            RenderPipeline target;
            if(blockStateIn.getMaterial() == Material.LAVA)
            {
                target = PipelineManager.INSTANCE.getLavaPipeline();
            }
            else
            {
                if(!didWarnUnhandledFluid && blockStateIn.getMaterial() != Material.WATER)
                {
                    Acuity.INSTANCE.getLog().warn(I18n.translateToLocal("misc.warn_unknown_fluid_render"));
                    didWarnUnhandledFluid = true;
                }
                target = PipelineManager.INSTANCE.getWaterPipeline();
            }
            final CompoundVertexLighter lighter = lighters.get();
            lighter.prepare((CompoundBufferBuilder)bufferBuilderIn, MinecraftForgeClient.getRenderLayer(), blockAccess, blockStateIn, blockPosIn, false);
            return fluidRenderer.renderFluid(blockAccess, blockStateIn, blockPosIn, fluidBuilders.get().prepare(target, lighter));
        }
        else
            return fluidRenderer.renderFluid(blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
    }

    private static AtomicInteger blockModelCount = new AtomicInteger();
    private static LongAccumulator blockModelNanos = new LongAccumulator((l, r) -> l + r, 0);
    
    /**
     * Performance counting version of #
     */
    public static boolean renderModelDebug(BlockModelRenderer blockModelRenderer, IBlockAccess blockAccess, IBakedModel model, 
            IBlockState state, BlockPos pos, BufferBuilder bufferBuilderIn, boolean checkSides)
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
    
    public static boolean renderModel(BlockModelRenderer blockModelRenderer, IBlockAccess blockAccess, IBakedModel model, IBlockState state, BlockPos pos,
            BufferBuilder bufferBuilderIn, boolean checkSides)
    {
        if(Acuity.isModEnabled())
        {
            if(model instanceof IPipelinedBakedModel)
                return renderModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
            else
                return renderVanillaModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
        }
        else
            return blockModelRenderer.renderModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
    }
    
    private static boolean renderModel(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder bufferIn, boolean checkSides)
    {
        try
        {
            final IPipelinedBakedModel model = (IPipelinedBakedModel)modelIn;
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

    private static boolean renderVanillaModel(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder bufferIn, boolean checkSides)
    {
        try
        {
            final CompoundVertexLighter lighter = lighters.get();
            final VanillaQuadWrapper wrapper = quadWrappers.get();
            final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            
            lighter.prepare((CompoundBufferBuilder)bufferIn, layer, worldIn, stateIn, posIn, checkSides);
            
            wrapper.prepare(layer, modelIn.isAmbientOcclusion() && stateIn.getLightValue(worldIn, posIn) == 0);
            
            modelIn.getQuads(stateIn, null, lighter.positionRandom()).forEach(q -> wrapper.wrapAndLight(lighter, q));
            for(EnumFacing face : EnumFacing.VALUES)
            {
                List<BakedQuad> list = modelIn.getQuads(stateIn, face, lighter.positionRandom());
                if (!list.isEmpty() && (!checkSides || stateIn.shouldSideBeRendered(worldIn, posIn, face)))
                   list.forEach(q -> wrapper.wrapAndLight(lighter, q));
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

    public static void uploadVertexBuffer(ChunkRenderDispatcher dispatch, BufferBuilder source, VertexBuffer target)
    {
        if(Acuity.isModEnabled())
            ((CompoundBufferBuilder)source).uploadTo((IDrawableChunk)target);
        else
            dispatch.uploadVertexBuffer(source, target);
    }
    
    public static boolean isFirstOrUV(int index, VertexFormatElement.EnumUsage usage)
    {
        // has to apply even when mod is disabled so that our formats can be instantiated
        return index == 0 || usage == VertexFormatElement.EnumUsage.UV || usage == VertexFormatElement.EnumUsage.GENERIC;
    }

    public static boolean useVbo()
    {
        return Acuity.isModEnabled() || (OpenGlHelper.vboSupported && Minecraft.getMinecraft().gameSettings.useVbo);
    }

    /**
     * When Acuity is enabled the per-chunk matrix is never used, so is wasteful to update when frustum moves.
     * Matters more when lots of block updates or other high-throughput because adds to contention.
     */
    public static void renderChunkInitModelViewMatrix(RenderChunk renderChunk)
    {
        if(Acuity.isModEnabled())
        {
            // this is called right after setting chunk position because it was moved in the frustum
            // let buffers in the chunk know they are no longer valid and should not render
            ((IDrawableChunk)renderChunk.getVertexBufferByLayer(BlockRenderLayer.SOLID.ordinal())).clear();
            ((IDrawableChunk)renderChunk.getVertexBufferByLayer(BlockRenderLayer.TRANSLUCENT.ordinal())).clear();
        }
        else
            renderChunk.initModelviewMatrix();
    }

    public static boolean shouldUploadLayer(CompiledChunk compiledchunk, BlockRenderLayer blockrenderlayer)
    {
        return Acuity.isModEnabled()
            ? compiledchunk.isLayerStarted(blockrenderlayer) && !compiledchunk.isLayerEmpty(blockrenderlayer)
            : compiledchunk.isLayerStarted(blockrenderlayer);
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
                case CUTOUT_MIPPED:
                default: 
                    return 0;
            }
        }
        else
            return renderGlobal.renderBlockLayer(blockLayerIn, partialTicks, pass, entityIn);
    }
}
