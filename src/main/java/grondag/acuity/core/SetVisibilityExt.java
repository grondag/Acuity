package grondag.acuity.core;

import java.util.EnumSet;

import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.util.EnumFacing;

public class SetVisibilityExt extends SetVisibility
{
    
    //TODO: handle this as a mixin to SetVisibility instead of a sub-class
    //That should be more reliable / less invasive.
    
    public Object visibility = EnumSet.<EnumFacing>noneOf(EnumFacing.class);
}
