package grondag.acuity.core;

import java.util.EnumSet;

import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.util.EnumFacing;

public class SetVisibilityExt extends SetVisibility
{
    public Object visibility = EnumSet.<EnumFacing>noneOf(EnumFacing.class);
}
