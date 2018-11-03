package grondag.acuity.buffering;

import java.util.function.Consumer;

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
    int drawCount();

    int quadCount();

    /**
     * Called when buffer content is no longer current and will not be rendered.
     */
    void clear();

    void upload(IUploadableChunk payload);
    
    public static interface Solid extends IDrawableChunk
    {
        /**
         * Prepares for iteration and handles any internal housekeeping.
         * Called each frame from client thread before any call to {@link #renderSolidNext()}.
         */
        void prepareSolidRender(Consumer<IDrawableBufferDelegate> consumer);
    }
    
    public static interface Translucent extends IDrawableChunk
    {
        void renderChunkTranslucent();
    }
}
