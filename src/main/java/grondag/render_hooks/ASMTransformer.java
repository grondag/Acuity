package grondag.render_hooks;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;


@SideOnly(Side.CLIENT)
public class ASMTransformer implements IClassTransformer
{
    @SuppressWarnings("null")
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (transformedName.equals("net.minecraft.client.renderer.RegionRenderCacheBuilder")) {
            return patchRegionRenderCacheBuilder(name, basicClass, name.compareTo(transformedName) != 0);
        }
        
        return basicClass;
    }
    
    private static final String REGION_RENDER_CACHE_BUILDER_MATERIAL_BUFFER_FIELD = "materialBuffers";
    
    @SuppressWarnings("null")
    public byte[] patchRegionRenderCacheBuilder(String name, byte[] bytes, boolean obfuscated)
    {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        
        addField(
                classNode, 
                Opcodes.ACC_PUBLIC, 
                REGION_RENDER_CACHE_BUILDER_MATERIAL_BUFFER_FIELD, 
                "Lgrondag/render_hooks/core/MaterialBufferManager;", 
                null);
        
          
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
        
    }
    
    private static void addField(ClassNode classNode, int opCodes, String fieldName, String type, Object init)
    {
        for(FieldNode f : classNode.fields)
        {
            if (f.name.equals(fieldName)) 
            {
                RenderHooks.INSTANCE.getLog().error("Unable to add field %s to class %s - field already exists.", fieldName, classNode.name );
                return;
            }
        }
        classNode.fields.add(new FieldNode(opCodes, fieldName, type, null, init));
    }
    
}
