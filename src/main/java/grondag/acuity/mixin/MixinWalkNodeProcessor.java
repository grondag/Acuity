package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

@Mixin(WalkNodeProcessor.class)
public abstract class MixinWalkNodeProcessor
{
    private static ThreadLocal<MutableBlockPos> pathNodePos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    // prevents significant garbage build up during chunk load
    @Redirect(method = "getPathNodeType(Lnet/minecraft/world/IBlockAccess;III)Lnet/minecraft/pathfinding/PathNodeType;", expect = 1,
            at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onGetPathNodeTypePos(int x, int y, int z) 
    {
        return pathNodePos.get().setPos(x, y, z);
    }
    
    // prevents significant garbage build up during chunk load
    @Redirect(method = "getPathNodeTypeRaw(Lnet/minecraft/world/IBlockAccess;III)Lnet/minecraft/pathfinding/PathNodeType;", expect = 1,
            at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onGetPathNodeTypeRawPos(int x, int y, int z)
    {
        return pathNodePos.get().setPos(x, y, z);
    }
}
