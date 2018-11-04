package grondag.acuity.hooks;

import javax.annotation.Nullable;

import grondag.acuity.buffering.DrawableChunk;

public interface IRenderChunk
{
    void setSolidDrawable(DrawableChunk.Solid drawable);
    void setTranslucentDrawable(DrawableChunk.Translucent drawable);
    @Nullable DrawableChunk.Solid getSolidDrawable();
    @Nullable DrawableChunk.Translucent getTranslucentDrawable();
    void releaseDrawables();
}
