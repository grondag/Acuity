package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;

@Mixin(WalkNodeProcessor.class)
public abstract class MixinWalkNodeProcessor
{
    private static ThreadLocal<BlockPos.Mutable> pathNodePos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    // prevents significant garbage build up during chunk load
    @Redirect(method = "getPathNodeType(Lnet/minecraft/world/IBlockAccess;III)Lnet/minecraft/pathfinding/PathNodeType;", expect = 1,
            at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onGetPathNodeTypePos(int x, int y, int z) 
    {
        return pathNodePos.get().set(x, y, z);
    }
    
    // prevents significant garbage build up during chunk load
    @Redirect(method = "getPathNodeTypeRaw(Lnet/minecraft/world/IBlockAccess;III)Lnet/minecraft/pathfinding/PathNodeType;", expect = 1,
            at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onGetPathNodeTypeRawPos(int x, int y, int z)
    {
        return pathNodePos.get().set(x, y, z);
    }
}
