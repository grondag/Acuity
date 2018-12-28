package grondag.acuity.mixin.perf;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import grondag.acuity.hooks.MutableBoundingBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;

@Mixin(BoundingBox.class)
public abstract class MixinBoundingBox implements MutableBoundingBox
{
    @Shadow @Final @Mutable private double minX;
    @Shadow @Final @Mutable private double minY;
    @Shadow @Final @Mutable private double minZ;
    @Shadow @Final @Mutable private double maxX;
    @Shadow @Final @Mutable private double maxY;
    @Shadow @Final @Mutable private double maxZ;
    
    @Override
    public MutableBoundingBox set(BoundingBox box)
    {
        this.minX = box.minX;
        this.minY = box.minY;
        this.minZ = box.minZ;
        this.maxX = box.maxX;
        this.maxY = box.maxY;
        this.maxZ = box.maxZ;
        return this;
    }
    
    @Override
    public MutableBoundingBox growMutable(double x, double y, double z)
    {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    @Override
    public MutableBoundingBox growMutable(double value)
    {
        return this.growMutable(value, value, value);
    }

    @Override
    public MutableBoundingBox offsetMutable(BlockPos pos)
    {
        this.minX += (double)pos.getX();
        this.minY += (double)pos.getY();
        this.minZ += (double)pos.getZ();
        this.maxX += (double)pos.getX();
        this.maxY += (double)pos.getY();
        this.maxZ += (double)pos.getZ();
        return this;
    }
    
    @Override
    public MutableBoundingBox expandMutable(double x, double y, double z)
    {
        if (x < 0.0D)
        {
            this.minX += x;
        }
        else if (x > 0.0D)
        {
            this.maxX += x;
        }

        if (y < 0.0D)
        {
            this.minY += y;
        }
        else if (y > 0.0D)
        {
            this.maxY += y;
        }

        if (z < 0.0D)
        {
            this.minZ += z;
        }
        else if (z > 0.0D)
        {
            this.maxZ += z;
        }
        
        return this;
    }
    
    @Override
    public BoundingBox cast()
    {
        return (BoundingBox)(Object)this;
    }

    @Override
    public BoundingBox toImmutable()
    {
        return new BoundingBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }
}
