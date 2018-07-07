package grondag.render_hooks.core;

import grondag.render_hooks.api.IPipelinedBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.pipeline.VertexLighterSmoothAo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelinedBlockModelRenderer
{
    private static final ThreadLocal<PipelinedVertexLighter> lighters = new ThreadLocal<PipelinedVertexLighter>()
    {
        @Override
        protected PipelinedVertexLighter initialValue()
        {
            return new PipelinedVertexLighter();
        }
    };
    
    public static boolean renderModel(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder bufferIn, boolean checkSides)
    {
        try
        {
            final IPipelinedBakedModel model = (IPipelinedBakedModel)modelIn;
            final PipelinedVertexLighter lighter = lighters.get();
            ((CompoundBufferBuilder)bufferIn).setOffset(posIn);
            VertexConsumerBufferBuilder buffer = ((CompoundBufferBuilder)bufferIn).getMaterialBuffer(model.p)
            lighters.get().r
            boolean flag = Minecraft.isAmbientOcclusionEnabled() && stateIn.getLightValue(worldIn, posIn) == 0 && modelIn.isAmbientOcclusion(stateIn);

       
            return true;
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block model (pipelined render)");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block model being tesselated");
            CrashReportCategory.addBlockInfo(crashreportcategory, posIn, stateIn);
            throw new ReportedException(crashreport);
        }
    }
    
}
