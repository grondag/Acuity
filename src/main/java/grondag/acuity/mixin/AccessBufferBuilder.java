package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.BufferBuilder;

@Mixin(BufferBuilder.class)
public interface AccessBufferBuilder
{
    @Accessor("building") public boolean isBuilding();
}
