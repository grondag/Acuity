package grondag.acuity.hooks;

import grondag.acuity.buffering.IDrawableChunk;

public interface IRenderChunk
{
    IDrawableChunk.Solid getSolidDrawable();
    IDrawableChunk.Translucent getTranslucentDrawable();
}
