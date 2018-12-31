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

package grondag.acuity.mixin.old;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.acuity.Acuity;
import grondag.acuity.buffering.DrawableChunk.Solid;
import grondag.acuity.buffering.DrawableChunk.Translucent;
import grondag.acuity.hooks.ChunkRebuildHelper;
import grondag.acuity.hooks.ChunkRenderDataStore;
import grondag.acuity.hooks.PipelineHooks;
import grondag.acuity.mixin.extension.ChunkRenderDataExt;
import grondag.acuity.mixin.extension.ChunkRendererExt;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkVisibility;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ChunkRenderer.class)
public abstract class MixinChunkRenderer implements ChunkRendererExt
{
    @Shadow public static int renderChunksUpdated;

    @Shadow public ChunkRenderData compiledChunk;
    @Shadow private BlockPos.Mutable position;
    @Shadow private ChunkCache worldView;
    @Shadow abstract void preRenderBlocks(BufferBuilder bufferBuilderIn, BlockPos pos);
    @Shadow abstract void postRenderBlocks(BlockRenderLayer layer, float x, float y, float z, BufferBuilder bufferBuilderIn, ChunkRenderData compiledChunkIn);
    @Shadow private ReentrantLock lockCompileTask;
    @Shadow private Set<BlockEntity> setTileEntities;
    @Shadow private WorldRenderer renderer;

    Solid solidDrawable;
    Translucent translucentDrawable;

    @Override
    public Solid getSolidDrawable()
    {
        return solidDrawable;
    }

    @Override
    public Translucent getTranslucentDrawable()
    {
        return translucentDrawable;
    }

    @Redirect(method = "setPosition", require = 1,
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;initModelviewMatrix()V"))
    private void onInitModelviewMatrix(ChunkRenderer renderChunk)
    {
        PipelineHooks.renderChunkInitModelViewMatrix(renderChunk);
    }

    @Inject(method = "deleteGlResources*", at = @At("RETURN"), require = 1)
    private void onDeleteGlResources(CallbackInfo ci)
    {
        releaseDrawables();
    }
    
    @Inject(method = "setCompiledChunk", require = 1, 
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;compiledChunk:Lnet/minecraft/client/renderer/chunk/CompiledChunk;"))
    private void onSetCompiledChunk(ChunkRenderData compiledChunkIn, CallbackInfo ci)
    {
        if(compiledChunk == null || compiledChunk == ChunkRenderData.EMPTY || compiledChunkIn == compiledChunk)
            return;

        ChunkRenderDataStore.release(compiledChunk);
    }

    // shouldn't be necessary if rebuild chunk hook works, but insurance if not
    @Redirect(method = "rebuildChunk", require = 1,       
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;setVisibility(Lnet/minecraft/client/renderer/chunk/SetVisibility;)V"))       
    private void onSetVisibility(ChunkRenderData compiledChunk, ChunkVisibility setVisibility)       
    {        
        compiledChunk.setChunkVisibility(setVisibility);      
        PipelineHooks.mergeRenderLayers(compiledChunk);      
    }
    @Inject(method = "stopCompileTask", require = 1, 
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;compiledChunk:Lnet/minecraft/client/renderer/chunk/CompiledChunk;"))
    private void onStopCompiledChunk(CallbackInfo ci)
    {
        if(compiledChunk == null || compiledChunk == ChunkRenderData.EMPTY)
            return;

        ChunkRenderDataStore.release(compiledChunk);
    }

    @Override
    public void setSolidDrawable(Solid drawable)
    {
        solidDrawable = drawable;
    }

    @Override
    public void setTranslucentDrawable(Translucent drawable)
    {
        translucentDrawable = drawable;
    }

    @Override
    public void releaseDrawables()
    {
        if(solidDrawable != null)
        {
            solidDrawable.clear();
            solidDrawable = null;
        }

        if(translucentDrawable != null)
        {
            translucentDrawable.clear();
            translucentDrawable = null;
        }
    }

    @Inject(method = "rebuildChunk", at = @At("HEAD"), cancellable = true, require = 1)
    private void onRebuildChunk(final float x, final float y, final float z, final ChunkCompileTaskGenerator generator, final CallbackInfo ci)
    {
        if(!Acuity.isModEnabled())
            return;

        final ChunkRebuildHelper help = ChunkRebuildHelper.get();
        help.clear();

        final ChunkRenderData compiledChunk = ChunkRenderDataStore.claim();
        final BlockPos.Mutable minPos = this.position;

        generator.getLock().lock();

        try
        {
            if (generator.getStatus() != ChunkCompileTaskGenerator.Status.COMPILING)
                return;

            generator.setCompiledChunk(compiledChunk);
        }
        finally
        {
            generator.getLock().unlock();
        }

        final HashSet<BlockEntity> tileEntities = help.tileEntities;

        if (!this.worldView.isEmpty())
        {
            ++renderChunksUpdated;
            final boolean[] layerFlags = help.layerFlags;
            final BlockRenderManager blockrendererdispatcher = MinecraftClient.getInstance().getBlockRenderManager();
            final BlockPos.Mutable searchPos = help.searchPos;
            final int xMin = minPos.getX();
            final int yMin = minPos.getY();
            final int zMin = minPos.getZ();
            final BufferBuilder[] builders = help.builders(generator.getRegionRenderCacheBuilder());

            for(int xPos = 0; xPos < 16; xPos++)
            {
                for(int yPos = 0; yPos < 16; yPos++)
                {
                    for(int zPos = 0; zPos < 16; zPos++)
                    {
                        searchPos.set(xMin + xPos, yMin + yPos, zMin + zPos);
                        final BlockState iblockstate = this.worldView.getBlockState(searchPos);
                        final Block block = iblockstate.getBlock();

                        if (iblockstate.isOpaqueCube())
                            visGraph.setOpaqueCube(searchPos);

                        if (block.hasBlockEntity())
                        {
                            final BlockEntity blockEntity = this.worldView.getBlockEntity(searchPos, WorldChunk.AccessType.GET);

                            if (blockEntity != null)
                            {
                                BlockEntityRenderer<BlockEntity> tileentityspecialrenderer = BlockEntityRenderDispatcher.INSTANCE.<BlockEntity>get(blockEntity);

                                if (tileentityspecialrenderer != null)
                                {
                                    // method_3563 indicates visible everywhere
                                    if (tileentityspecialrenderer.method_3563(blockEntity))
                                        tileEntities.add(blockEntity);
                                    else 
                                        compiledChunk.addTileEntity(blockEntity);
                                }
                            }
                        }

                        for(int i = 0; i < ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT; i++)
                        {
                            final BlockRenderLayer layer = help.layers[i];
                            if(!block.canRenderInLayer(iblockstate, layer)) 
                                continue;

                            //FIXME: support fabric hook when available
                            //net.minecraftforge.client.ForgeHooksClient.setRenderLayer(layer);

                            if (block.getDefaultState().getRenderType() != BlockRenderType.INVISIBLE)
                            {
                                if (!compiledChunk.isInitialized(layer))
                                {
                                    compiledChunk.setInitialized(layer);
                                    this.preRenderBlocks(builders[i], minPos);
                                }

                                layerFlags[i] |= blockrendererdispatcher.renderBlock(iblockstate, searchPos, this.worldView, builders[i]);
                            }
                        }
                      //FIXME: support fabric hook when available
                      //net.minecraftforge.client.ForgeHooksClient.setRenderLayer(null);
                    }
                }
            }


            for(int i = 0; i < ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT; i++)
            {
                final BlockRenderLayer layer = help.layers[i];
                if (layerFlags[i])
                    ((ChunkRenderDataExt)compiledChunk).setNonEmpty(layer);

                if (compiledChunk.isInitialized(layer))
                    this.postRenderBlocks(layer, x, y, z, builders[i], compiledChunk);
            }
        }

        compiledChunk.setVisibility(visGraph.computeVisibility());

        PipelineHooks.mergeRenderLayers(compiledChunk);

        this.lockCompileTask.lock();

        try
        {
            help.tileEntitiesToAdd.addAll(tileEntities);
            help.tileEntitiesToRemove.addAll(this.setTileEntities);

            help.tileEntitiesToAdd.removeAll(this.setTileEntities);
            help.tileEntitiesToRemove.removeAll(tileEntities);

            this.setTileEntities.clear();
            this.setTileEntities.addAll(tileEntities);
            this.renderGlobal.updateTileEntities(help.tileEntitiesToRemove, help.tileEntitiesToAdd);
        }
        finally
        {
            this.lockCompileTask.unlock();
        }

        ci.cancel();
    }
}
