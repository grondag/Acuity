package grondag.acuity.hooks;

import javax.annotation.Nullable;

import grondag.acuity.buffering.IDrawableChunk;

public interface IRenderChunk
{
    void setSolidDrawable(IDrawableChunk.Solid drawable);
    void setTranslucentDrawable(IDrawableChunk.Translucent drawable);
    @Nullable IDrawableChunk.Solid getSolidDrawable();
    @Nullable IDrawableChunk.Translucent getTranslucentDrawable();
    void releaseDrawables();
}
