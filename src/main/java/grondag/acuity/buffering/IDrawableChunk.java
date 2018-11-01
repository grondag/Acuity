package grondag.acuity.buffering;

import grondag.acuity.core.BufferStore.ExpandableByteBuffer;
import grondag.acuity.core.VertexPackingList;

/**
 * Plays same role as VertexBuffer in RenderChunk but implementation
 * is much different.<p>
 * 
 * For solid layer, each pipeline will be separately collected
 * into memory-mapped buffers specific to that pipeline so that during
 * render we are able to render multiple chunks per pipeline out of 
 * the same buffer.<p>
 * 
 * For translucent layer, all pipelines will be collected into the 
 * same buffer because rendering order must be maintained.<p>
 * 
 * In both cases, it is possible for a pipeline's vertices to span
 * two buffers because our memory-mapped buffers are fixed size.<p>
 * 
 * The implementation handles the draw commands and vertex attribute 
 * state but relies on caller to manage shaders, uniforms, transforms
 * or any other GL state.<p>
 *
 *
 */
public interface IDrawableChunk
{

    VertexPackingList packingList();

    int drawCount();

    int quadCount();

    void clear();

    void upload(ExpandableByteBuffer left, VertexPackingList right);
    
    public static interface Solid extends IDrawableChunk
    {
        void prepareSolidRender();
        void renderSolidNext();
    }
    
    public static interface Translucent extends IDrawableChunk
    {
        void renderChunkTranslucent();
    }
}
