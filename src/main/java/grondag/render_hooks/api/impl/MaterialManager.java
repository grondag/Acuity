package grondag.render_hooks.api.impl;

import grondag.render_hooks.api.IMaterialManager;
import grondag.render_hooks.api.IMaterialRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MaterialManager implements IMaterialManager
{
    private ObjectArrayList<IMaterialRenderer> list = new ObjectArrayList<>();
    
    @Override
    public synchronized int createMaterial(IMaterialRenderer material)
    {
        final int n = list.size();
        if(n < MAX_MATERIAL_COUNT)
        {
            list.add(material);
            return n + 1;
        }
        else
            return -1;
    }
    
    public IMaterialRenderer getMaterial(int materialID)
    {
        return list.get(materialID);
    }
    
    public int materialCount()
    {
        return list.size();
    }
}
