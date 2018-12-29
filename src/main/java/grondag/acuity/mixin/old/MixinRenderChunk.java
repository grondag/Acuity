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
import grondag.acuity.extension.AcuityChunkVisibility;
import grondag.acuity.hooks.ChunkRebuildHelper;
import grondag.acuity.hooks.ChunkRenderDataStore;
import grondag.acuity.hooks.IRenderChunk;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk implements IRenderChunk
{
    @Shadow public static int renderChunksUpdated;

    @Shadow public ChunkRenderData compiledChunk;
    @Shadow private BlockPos.Mutable position;
    @Shadow private ChunkCache worldView;
    @Shadow abstract void preRenderBlocks(BufferBuilder bufferBuilderIn, BlockPos pos);
    @Shadow abstract void postRenderBlocks(BlockRenderLayer layer, float x, float y, float z, BufferBuilder bufferBuilderIn, ChunkRenderData compiledChunkIn);
    @Shadow private ReentrantLock lockCompileTask;
    @Shadow private Set<BlockEntity> setTileEntities;
    @Shadow private RenderGlobal renderGlobal;

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
    private void onInitModelviewMatrix(RenderChunk renderChunk)
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

        ((AcuityChunkVisibility)compiledChunk.setVisibility).releaseVisibilityData();
        ChunkRenderDataStore.release(compiledChunk);
    }

    // shouldn't be necessary if rebuild chunk hook works, but insurance if not
    @Redirect(method = "rebuildChunk", require = 1,       
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;setVisibility(Lnet/minecraft/client/renderer/chunk/SetVisibility;)V"))       
    private void onSetVisibility(ChunkRenderData compiledChunk, SetVisibility setVisibility)       
    {        
        compiledChunk.setVisibility(setVisibility);      
        PipelineHooks.mergeRenderLayers(compiledChunk);      
    }
    @Inject(method = "stopCompileTask", require = 1, 
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;compiledChunk:Lnet/minecraft/client/renderer/chunk/CompiledChunk;"))
    private void onStopCompiledChunk(CallbackInfo ci)
    {
        if(compiledChunk == null || compiledChunk == ChunkRenderData.EMPTY)
            return;

        ((AcuityChunkVisibility)compiledChunk.setVisibility).releaseVisibilityData();
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

        final VisGraph visGraph = help.visGraph;
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
                                if (!compiledChunk.isLayerStarted(layer))
                                {
                                    compiledChunk.setLayerStarted(layer);
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
                    compiledChunk.setLayerUsed(layer);

                if (compiledChunk.isLayerStarted(layer))
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
