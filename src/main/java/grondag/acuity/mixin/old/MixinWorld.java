package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.hooks.MutableBoundingBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(World.class)
public abstract class MixinWorld
{
    private static ThreadLocal<MutableBoundingBox> mutableAABB = new ThreadLocal<MutableBoundingBox>()
    {
        @Override
        protected MutableBoundingBox initialValue()
        {
            return (MutableBoundingBox)new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        }
    };
    
    private static ThreadLocal<BlockPos.Mutable> mutablePos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    // prevents significant garbage build up
    @Redirect(method = "getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;",
            at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/AxisAlignedBB;grow(D)Lnet/minecraft/util/math/AxisAlignedBB;"))
    private AxisAlignedBB onGetCBGrow(AxisAlignedBB box, double d)
    {
        return mutableAABB.get().set(box).growMutable(d).cast();
    }
    
    // prevents significant garbage build up
    @Redirect(method = "isFlammableWithin",
                      at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onIsFlammableWithin(int x, int y, int z) 
    {
        return mutablePos.get().set(x, y, z);
    }
    
    private static final Direction[] RAW_LIGHT_DIRECTION = Direction.values();
    
    // prevents significant garbage build up
    @Redirect(method = "getRawLight",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/EnumFacing;values()[Lnet/minecraft/util/EnumFacing;"))
    private Direction[] onGetRawLightFaces() 
    {
        return RAW_LIGHT_DIRECTION;
    }
    
    // prevents significant garbage build up
    @Redirect(method = "checkLightFor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/EnumFacing;values()[Lnet/minecraft/util/EnumFacing;"))
    private EnumFacing[] onCheckLightForFaces() 
    {
        return EnumFacing.VALUES;
    }
    
    
    private static ThreadLocal<BlockPos.Mutable> checkLightPos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    // prevents significant garbage build up
    @Redirect(method = "checkLightFor",
            at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onCheckLightForPos(int x, int y, int z) 
    {
        return checkLightPos.get().set(x, y, z);
    }
}
