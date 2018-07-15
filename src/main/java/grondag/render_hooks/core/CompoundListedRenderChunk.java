package grondag.render_hooks.core;

import java.nio.ByteBuffer;
import java.util.List;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundListedRenderChunk extends ListedRenderChunk
{
    private final boolean isModEnabled = RenderHooks.isModEnabled();
    
    public CompoundListedRenderChunk(World worldIn, RenderGlobal renderGlobalIn, int index)
    {
        super(worldIn, renderGlobalIn, index);
    }

    /**
     * Always returns glList for layer even if empty as way 
     * of passing render layer to API without changing method signatures.
     */
    @Override
    public int getDisplayList(BlockRenderLayer layer, CompiledChunk p_178600_2_)
    {
        return isModEnabled ? this.baseDisplayList + layer.ordinal() : super.getDisplayList(layer, p_178600_2_);
    }
    
    public void prepareForUpload(int vanillaList)
    {
        GlStateManager.glNewList(vanillaList, 4864);
    }

    /**
     * Implementation for lists is based on WorldVertexBufferUploader
     */
    public void uploadBuffer(IRenderPipeline pipeline, BufferBuilder bufferBuilderIn)
    {
        if(bufferBuilderIn.getVertexCount() == 0) return;
        
        GlStateManager.pushMatrix();
        this.multModelviewMatrix();
        
        VertexFormat vertexformat = bufferBuilderIn.getVertexFormat();
        int i = vertexformat.getNextOffset();
        ByteBuffer bytebuffer = bufferBuilderIn.getByteBuffer();
        List<VertexFormatElement> list = vertexformat.getElements();

        for (int j = 0; j < list.size(); ++j)
        {
            VertexFormatElement vertexformatelement = list.get(j);
            bytebuffer.position(vertexformat.getOffset(j));
            vertexformatelement.getUsage().preDraw(vertexformat, j, i, bytebuffer);
        }

        pipeline.preDraw();
        GlStateManager.glDrawArrays(bufferBuilderIn.getDrawMode(), 0, bufferBuilderIn.getVertexCount());
        pipeline.postDraw();
        
        int i1 = 0;
        for (int j1 = list.size(); i1 < j1; ++i1)
        {
            VertexFormatElement vertexformatelement1 = list.get(i1);
            vertexformatelement1.getUsage().postDraw(vertexformat, i1, i, bytebuffer);
        }

        bufferBuilderIn.reset();
        GlStateManager.popMatrix();
    }
    
    public void completeUpload()
    {
        GlStateManager.glEndList();        
    }
}
