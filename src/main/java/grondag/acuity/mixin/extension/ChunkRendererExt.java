package grondag.acuity.mixin.extension;

import grondag.acuity.buffering.DrawableChunk;

public interface ChunkRendererExt
{
    void setSolidDrawable(DrawableChunk.Solid drawable);
    void setTranslucentDrawable(DrawableChunk.Translucent drawable);
    DrawableChunk.Solid getSolidDrawable();
    DrawableChunk.Translucent getTranslucentDrawable();
    void releaseDrawables();
}
