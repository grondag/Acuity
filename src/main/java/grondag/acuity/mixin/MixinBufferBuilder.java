package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.acuity.mixin.extension.BufferBuilderExt;
import net.minecraft.client.render.BufferBuilder;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements BufferBuilderExt
{
    @Shadow private boolean building;
    
    @Override
    public boolean isBuilding()
    {
        return building;
    }
}
