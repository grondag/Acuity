package grondag.acuity.mixin.old;

import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Queues;

import grondag.acuity.Acuity;
import grondag.acuity.hooks.IMutableAxisAlignedBB;
import grondag.acuity.hooks.IRenderGlobal;
import grondag.acuity.hooks.ISetVisibility;
import grondag.acuity.hooks.PipelineHooks;
import grondag.acuity.hooks.VisibilityHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Vector3d;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal implements IRenderGlobal
{
    @Shadow private ViewFrustum viewFrustum;
    @Shadow private Minecraft mc;
    @Shadow private WorldClient world;
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
    
    @Shadow public abstract Set<EnumFacing> getVisibleFacings(BlockPos pos);
    @Shadow protected abstract Vector3f getViewVector(Entity entityIn, double partialTicks);
    @Shadow public abstract void loadRenderers();
    @Shadow @Nullable protected abstract RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing);
    @Shadow protected abstract void fixTerrainFrustum(double x, double y, double z);
    
    /**
     * Called from {@link RenderGlobal#setupTerrain(net.minecraft.entity.Entity, double, net.minecraft.client.renderer.culling.ICamera, int, boolean)}.
     * Relies on pre-computed visibility stored during render chunk rebuild vs computing on fly each time.
     */
    @Inject(method = "getVisibleFacings", at = @At("HEAD"), cancellable = true, expect = 1)
    public void onGetVisibleFacings(BlockPos eyePos, CallbackInfoReturnable<Set<EnumFacing>> ci)
    {
        if(Acuity.isModEnabled())
        {
            RenderChunk renderChunk = viewFrustum.getRenderChunk(eyePos);
            if(renderChunk != null)
            {
                Object visData = ((ISetVisibility)renderChunk.compiledChunk.setVisibility).getVisibilityData();
                // unbuilt chunks won't have extended info
                if(visData != null)
                {
                    ci.setReturnValue(VisibilityHooks.getVisibleFacingsExt(visData, eyePos));
                }
            }
        }
    }

    @Redirect(method = "renderBlockLayer", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;isLayerStarted(Lnet/minecraft/util/BlockRenderLayer;)Z"))
    private boolean isLayerStarted(CompiledChunk compiledChunk, BlockRenderLayer layer)
    {
        return PipelineHooks.shouldUploadLayer(compiledChunk, layer);
    }
    
    private final MutableBlockPos eyePos = new MutableBlockPos();
    private final MutableBlockPos viewChunkOrigin = new MutableBlockPos();
    private final Queue<RenderGlobal.ContainerLocalRenderInformation> renderQueue = Queues.<RenderGlobal.ContainerLocalRenderInformation>newArrayDeque();
    private final IMutableAxisAlignedBB box = (IMutableAxisAlignedBB) new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    
    @SuppressWarnings("null")
    @Override
    public void setupTerrainFast(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator)
    {
        if (this.mc.gameSettings.renderDistanceChunks != this.renderDistanceChunks)
        {
            this.loadRenderers();
        }

        this.world.profiler.startSection("camera");
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

        this.world.profiler.endStartSection("renderlistcamera");
        double viewX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * partialTicks;
        double viewY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * partialTicks;
        double viewZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * partialTicks;
        this.renderContainer.initialize(viewX, viewY, viewZ);
        
        this.world.profiler.endStartSection("cull");
        if (this.debugFixedClippingHelper != null)
        {
            Frustum frustum = new Frustum(this.debugFixedClippingHelper);
            frustum.setPosition(this.debugTerrainFrustumPosition.x, this.debugTerrainFrustumPosition.y, this.debugTerrainFrustumPosition.z);
            camera = frustum;
        }

        this.mc.profiler.endStartSection("culling");
        final BlockPos eyePos = this.eyePos.setPos(viewX, viewY + (double)viewEntity.getEyeHeight(), viewZ);
        final BlockPos viewChunkOrigin = this.viewChunkOrigin.setPos(MathHelper.floor(viewX / 16.0D) * 16, MathHelper.floor(viewY / 16.0D) * 16, MathHelper.floor(viewZ / 16.0D) * 16);
        RenderChunk eyeRenderChunk = this.viewFrustum.getRenderChunk(eyePos);
        
        this.displayListEntitiesDirty = this.displayListEntitiesDirty || !this.chunksToUpdate.isEmpty() || viewEntity.posX != this.lastViewEntityX || viewEntity.posY != this.lastViewEntityY || viewEntity.posZ != this.lastViewEntityZ || (double)viewEntity.rotationPitch != this.lastViewEntityPitch || (double)viewEntity.rotationYaw != this.lastViewEntityYaw;
        this.lastViewEntityX = viewEntity.posX;
        this.lastViewEntityY = viewEntity.posY;
        this.lastViewEntityZ = viewEntity.posZ;
        this.lastViewEntityPitch = (double)viewEntity.rotationPitch;
        this.lastViewEntityYaw = (double)viewEntity.rotationYaw;
        boolean haveDebugFixedClippingHelper = this.debugFixedClippingHelper != null;
        
        this.mc.profiler.endStartSection("update");

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
                Set<EnumFacing> eyeFacings = this.getVisibleFacings(eyePos);

                if (eyeFacings.size() == 1)
                {
                    Vector3f vector3f = this.getViewVector(viewEntity, partialTicks);
                    EnumFacing enumfacing = EnumFacing.getFacingFromVector(vector3f.x, vector3f.y, vector3f.z).getOpposite();
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

            this.mc.profiler.startSection("iteration");

            while (!queue.isEmpty())
            {
                final RenderGlobal.ContainerLocalRenderInformation info = queue.poll();
                final RenderChunk infoChunk = info.renderChunk;
                final EnumFacing infoFace = info.facing;
                this.renderInfos.add(info);
                if(infoChunk.needsUpdate())
                    this.chunksToUpdate.add(infoChunk);
                
                for(int i = 0; i < 6; i++)
                {
                    final EnumFacing checkFace = EnumFacing.VALUES[i];
                    final RenderChunk checkChunk = this.getRenderChunkOffset(viewChunkOrigin, infoChunk, checkFace);

                    if ((!renderMany || !info.hasDirection(checkFace.getOpposite())) && (!renderMany || infoFace == null || infoChunk.getCompiledChunk().isVisible(infoFace.getOpposite(), checkFace)) && checkChunk != null && checkChunk.setFrameIndex(frameCount) && camera.isBoundingBoxInFrustum(checkChunk.boundingBox))
                    {
                        RenderGlobal.ContainerLocalRenderInformation newInfo = ((RenderGlobal)(Object)(this)).new ContainerLocalRenderInformation(checkChunk, checkFace, info.counter + 1);
                        newInfo.setDirection(info.setFacing, checkFace);
                        queue.add(newInfo);
                    }
                }
            }

            this.mc.profiler.endSection();
        }
        
        this.displayListEntitiesDirty = displayListEntitiesDirty || !this.chunksToUpdate.isEmpty();

        this.mc.profiler.endStartSection("captureFrustum");

        if (this.debugFixTerrainFrustum)
        {
            this.fixTerrainFrustum(viewX, viewY, viewZ);
            this.debugFixTerrainFrustum = false;
        }
    }
}
