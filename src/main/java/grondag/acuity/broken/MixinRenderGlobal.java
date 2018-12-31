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

package grondag.acuity.broken;

import java.util.List;
import java.util.Queue;
import java.util.Set;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Queues;

import grondag.acuity.Acuity;
import grondag.acuity.fermion.varia.DirectionHelper;
import grondag.acuity.hooks.IRenderGlobal;
import grondag.acuity.mixin.extension.ChunkRenderDispatcherExt;
import grondag.acuity.mixin.extension.MinecraftClientExt;
import grondag.acuity.mixin.extension.MutableBoundingBox;
import grondag.acuity.mixin.extension.ChunkRenderDataExt;
import net.minecraft.class_3689;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal implements IRenderGlobal
{
    @Shadow private ChunkRenderDispatcher viewFrustum;
    @Shadow private MinecraftClient client;
    @Shadow private ClientWorld world;
    @Shadow private double frustumUpdatePosX;
    @Shadow private double frustumUpdatePosY;
    @Shadow private double frustumUpdatePosZ;
    @Shadow private int frustumUpdatePosChunkX;
    @Shadow private int frustumUpdatePosChunkY;
    @Shadow private int frustumUpdatePosChunkZ;
    @Shadow private double lastViewEntityX;
    @Shadow private double lastViewEntityY;
    @Shadow private double lastViewEntityZ;
    @Shadow private double lastViewEntityPitch;
    @Shadow private double lastViewEntityYaw;
    @Shadow private boolean displayListEntitiesDirty;
    @Shadow private Set<RenderChunk> chunksToUpdate;
    @Shadow private ClippingHelper debugFixedClippingHelper;
    @Shadow private List<RenderGlobal.ContainerLocalRenderInformation> renderInfos;
    @Shadow private int renderDistanceChunks;
    @Shadow private ChunkRenderContainer renderContainer;
    @Shadow private Vector3d debugTerrainFrustumPosition;
    @Shadow private boolean debugFixTerrainFrustum;
    @Shadow public ChunkRenderDispatcher renderDispatcher;
    
    @Shadow public abstract Set<Direction> getVisibleFacings(BlockPos pos);
    @Shadow protected abstract Vector3f getViewVector(Entity entityIn, double partialTicks);
    @Shadow public abstract void loadRenderers();
    @Shadow protected abstract RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, Direction facing);
    @Shadow protected abstract void fixTerrainFrustum(double x, double y, double z);

    @Redirect(method = "renderBlockLayer", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;isLayerStarted(Lnet/minecraft/util/BlockRenderLayer;)Z"))
    private boolean isLayerStarted(ChunkRenderData compiledChunk, BlockRenderLayer layer)
    {
        return PipelineHooks.shouldUploadLayer(compiledChunk, layer);
    }
    
    private final BlockPos.Mutable eyePos = new BlockPos.Mutable();
    private final BlockPos.Mutable viewChunkOrigin = new BlockPos.Mutable();
    private final Queue<RenderGlobal.ContainerLocalRenderInformation> renderQueue = Queues.<RenderGlobal.ContainerLocalRenderInformation>newArrayDeque();
    private final MutableBoundingBox box = (MutableBoundingBox) new BoundingBox(0, 0, 0, 0, 0, 0);
    
    @SuppressWarnings("null")
    @Override
    public void setupTerrainFast(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator)
    {
        final class_3689 profiler = ((MinecraftClientExt)mc).profiler();
        if (this.mc.gameSettings.renderDistanceChunks != this.renderDistanceChunks)
        {
            this.loadRenderers();
        }

        profiler.begin("camera");
        double dx = viewEntity.posX - this.frustumUpdatePosX;
        double dy = viewEntity.posY - this.frustumUpdatePosY;
        double dz = viewEntity.posZ - this.frustumUpdatePosZ;

        if (this.frustumUpdatePosChunkX != viewEntity.chunkCoordX || this.frustumUpdatePosChunkY != viewEntity.chunkCoordY || this.frustumUpdatePosChunkZ != viewEntity.chunkCoordZ || dx * dx + dy * dy + dz * dz > 16.0D)
        {
            this.frustumUpdatePosX = viewEntity.posX;
            this.frustumUpdatePosY = viewEntity.posY;
            this.frustumUpdatePosZ = viewEntity.posZ;
            this.frustumUpdatePosChunkX = viewEntity.chunkCoordX;
            this.frustumUpdatePosChunkY = viewEntity.chunkCoordY;
            this.frustumUpdatePosChunkZ = viewEntity.chunkCoordZ;
            this.viewFrustum.updateChunkPositions(viewEntity.posX, viewEntity.posZ);
        }

        profiler.endBegin("renderlistcamera");
        double viewX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * partialTicks;
        double viewY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * partialTicks;
        double viewZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * partialTicks;
        this.renderContainer.initialize(viewX, viewY, viewZ);
        
        profiler.endBegin("cull");
        if (this.debugFixedClippingHelper != null)
        {
            Frustum frustum = new Frustum(this.debugFixedClippingHelper);
            frustum.setPosition(this.debugTerrainFrustumPosition.x, this.debugTerrainFrustumPosition.y, this.debugTerrainFrustumPosition.z);
            camera = frustum;
        }

        profiler.endBegin("culling");
        final BlockPos eyePos = this.eyePos.set(viewX, viewY + (double)viewEntity.getEyeHeight(), viewZ);
        final BlockPos viewChunkOrigin = this.viewChunkOrigin.set(MathHelper.floor(viewX / 16.0D) * 16, MathHelper.floor(viewY / 16.0D) * 16, MathHelper.floor(viewZ / 16.0D) * 16);
        RenderChunk eyeRenderChunk = this.viewFrustum.getRenderChunk(eyePos);
        
        this.displayListEntitiesDirty = this.displayListEntitiesDirty || !this.chunksToUpdate.isEmpty() || viewEntity.posX != this.lastViewEntityX || viewEntity.posY != this.lastViewEntityY || viewEntity.posZ != this.lastViewEntityZ || (double)viewEntity.rotationPitch != this.lastViewEntityPitch || (double)viewEntity.rotationYaw != this.lastViewEntityYaw;
        this.lastViewEntityX = viewEntity.posX;
        this.lastViewEntityY = viewEntity.posY;
        this.lastViewEntityZ = viewEntity.posZ;
        this.lastViewEntityPitch = (double)viewEntity.rotationPitch;
        this.lastViewEntityYaw = (double)viewEntity.rotationYaw;
        boolean haveDebugFixedClippingHelper = this.debugFixedClippingHelper != null;
        
        profiler.endBegin("update");

        if (!haveDebugFixedClippingHelper && this.displayListEntitiesDirty)
        {
            this.displayListEntitiesDirty = false;
            this.renderInfos.clear();
            Queue<RenderGlobal.ContainerLocalRenderInformation> queue = this.renderQueue;
            queue.clear();
            
            Entity.setRenderDistanceWeight(MathHelper.clamp((double)this.mc.gameSettings.renderDistanceChunks / 8.0D, 1.0D, 2.5D));
            boolean renderMany = this.mc.renderChunksMany;

            if (eyeRenderChunk != null)
            {
                boolean renderEyeChunk = false;
                RenderGlobal.ContainerLocalRenderInformation eyeRenderInfo = ((RenderGlobal)(Object)(this)).new ContainerLocalRenderInformation(eyeRenderChunk, null, 0);
                Set<Direction> eyeFacings = this.getVisibleFacings(eyePos);

                if (eyeFacings.size() == 1)
                {
                    Vector3f vector3f = this.getViewVector(viewEntity, partialTicks);
                    Direction enumfacing = Direction.getFacingFromVector(vector3f.x, vector3f.y, vector3f.z).getOpposite();
                    eyeFacings.remove(enumfacing);
                }

                if (eyeFacings.isEmpty())
                {
                    renderEyeChunk = true;
                }

                if (renderEyeChunk && !playerSpectator)
                {
                    this.renderInfos.add(eyeRenderInfo);
                }
                else
                {
                    if (playerSpectator && this.world.getBlockState(eyePos).isOpaqueCube())
                    {
                        renderMany = false;
                    }

                    eyeRenderChunk.setFrameIndex(frameCount);
                    queue.add(eyeRenderInfo);
                }
            }
            else
            {
                int i = eyePos.getY() > 0 ? 248 : 8;

                for (int j = -this.renderDistanceChunks; j <= this.renderDistanceChunks; ++j)
                {
                    for (int k = -this.renderDistanceChunks; k <= this.renderDistanceChunks; ++k)
                    {
                        RenderChunk renderChunk = this.viewFrustum.getRenderChunk(new BlockPos((j << 4) + 8, i, (k << 4) + 8));

                        if (renderChunk != null && camera.isBoundingBoxInFrustum(
                                box.set(renderChunk.boundingBox)
                                .expandMutable(0.0, eyePos.getY() > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY, 0.0)
                                .cast())) // Forge: fix MC-73139
                        {
                            renderChunk.setFrameIndex(frameCount);
                            queue.add(((RenderGlobal)(Object)(this)).new ContainerLocalRenderInformation(renderChunk, null, 0));
                        }
                    }
                }
            }

            profiler.begin("iteration");

            while (!queue.isEmpty())
            {
                final RenderGlobal.ContainerLocalRenderInformation info = queue.poll();
                final RenderChunk infoChunk = info.renderChunk;
                final Direction infoFace = info.facing;
                this.renderInfos.add(info);
                if(infoChunk.needsUpdate())
                    this.chunksToUpdate.add(infoChunk);
                
                for(int i = 0; i < 6; i++)
                {
                    final Direction checkFace = DirectionHelper.fromOrdinal(i);
                    final RenderChunk checkChunk = this.getRenderChunkOffset(viewChunkOrigin, infoChunk, checkFace);

                    if ((!renderMany || !info.hasDirection(checkFace.getOpposite())) && (!renderMany || infoFace == null || infoChunk.getCompiledChunk().isVisible(infoFace.getOpposite(), checkFace)) && checkChunk != null && checkChunk.setFrameIndex(frameCount) && camera.isBoundingBoxInFrustum(checkChunk.boundingBox))
                    {
                        RenderGlobal.ContainerLocalRenderInformation newInfo = ((RenderGlobal)(Object)(this)).new ContainerLocalRenderInformation(checkChunk, checkFace, info.counter + 1);
                        newInfo.setDirection(info.setFacing, checkFace);
                        queue.add(newInfo);
                    }
                }
            }

            profiler.end();
        }
        
        this.displayListEntitiesDirty = displayListEntitiesDirty || !this.chunksToUpdate.isEmpty();

        profiler.endBegin("captureFrustum");

        if (this.debugFixTerrainFrustum)
        {
            this.fixTerrainFrustum(viewX, viewY, viewZ);
            this.debugFixTerrainFrustum = false;
        }
    }
}
