package grondag.render_hooks.core;

import java.util.List;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelinedBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelineHooks
{
    private static final ThreadLocal<CompoundVertexLighter> lighters = new ThreadLocal<CompoundVertexLighter>()
    {
        @Override
        protected CompoundVertexLighter initialValue()
        {
            return new CompoundVertexLighter();
        }
    };
    
    private static final ThreadLocal<VanillaQuadWrapper> quadWrappers = new ThreadLocal<VanillaQuadWrapper>()
    {
        @Override
        protected VanillaQuadWrapper initialValue()
        {
            return new VanillaQuadWrapper();
        }
    };
    
    
    public static boolean renderModel(BlockModelRenderer blockModelRenderer, IBlockAccess blockAccess, IBakedModel model, IBlockState state, BlockPos pos,
            BufferBuilder bufferBuilderIn, boolean checkSides)
    {
        if(RenderHooks.isModEnabled())
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
            lighter.releaseResources();
            return lighter.didOutput();
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block model (pipelined render)");
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
            wrapper.prepare(layer);
            
            modelIn.getQuads(stateIn, null, lighter.positionRandom()).forEach(q -> wrapper.wrapAndLight(lighter, q));
            for(EnumFacing face : EnumFacing.VALUES)
            {
                List<BakedQuad> list = modelIn.getQuads(stateIn, face, lighter.positionRandom());
                if (!list.isEmpty() && (!checkSides || stateIn.shouldSideBeRendered(worldIn, posIn, face)))
                   list.forEach(q -> wrapper.wrapAndLight(lighter, q));
            }
            
            lighter.releaseResources();
            
            return lighter.didOutput();
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block model (pipelined render)");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block model being tesselated");
            CrashReportCategory.addBlockInfo(crashreportcategory, posIn, stateIn);
            throw new ReportedException(crashreport);
        }
    }
    
    public static void  uploadDisplayList(BufferBuilder source, int vanillaList, RenderChunk target)
    {
        if(RenderHooks.isModEnabled())
            ((CompoundBufferBuilder)source).uploadTo((CompoundListedRenderChunk)target, vanillaList);
        else
            Minecraft.getMinecraft().renderGlobal.renderDispatcher.uploadDisplayList(source, vanillaList, target);
    }

    public static void uploadVertexBuffer(BufferBuilder source, VertexBuffer target)
    {
        if(RenderHooks.isModEnabled())
            ((CompoundBufferBuilder)source).uploadTo((CompoundVertexBuffer)target);
        else
            Minecraft.getMinecraft().renderGlobal.renderDispatcher.uploadVertexBuffer(source, target);
    }
    
    public static boolean isFirstOrUV(Object callerIgnored, int index, VertexFormatElement.EnumUsage usage)
    {
        if(RenderHooks.isModEnabled())
            return index == 0 || usage == VertexFormatElement.EnumUsage.UV || usage == VertexFormatElement.EnumUsage.GENERIC;
        else
            return index == 0 || usage == VertexFormatElement.EnumUsage.UV;
    }
}
