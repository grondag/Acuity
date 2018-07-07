package grondag.render_hooks;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;


@SideOnly(Side.CLIENT)
public class ASMTransformer implements IClassTransformer
{
    private static final String REGION_RENDER_CACHE_BUILDER_CLASS = "net.minecraft.client.renderer.RegionRenderCacheBuilder";
    private static final String REGION_RENDER_CACHE_BUILDER_MATERIAL_BUFFER_FIELD = "materialBuffers";
    private static final String RENDER_BUFFER_MANAGER_CLASS = "grondag/render_hooks/core/RenderMaterialBufferManager";
    
    private static final String BMR_CLASS = "net.minecraft.client.renderer.BlockModelRenderer";
    
    @SuppressWarnings("null")
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (transformedName.equals(REGION_RENDER_CACHE_BUILDER_CLASS))
            return patchRegionRenderCacheBuilder(name, basicClass, name.compareTo(transformedName) != 0);
        else if (transformedName.equals(BMR_CLASS))
            return patchBlockModelRenderer(name, basicClass, name.compareTo(transformedName) != 0);
        return basicClass;
    }
    
    private static String lwrap(String className)
    {
        return "L" + className + ";";
    }
    
    // in RenderChunk.rebuildChunk
    // replace...
    //    INVOKEVIRTUAL net/minecraft/client/renderer/BlockRendererDispatcher.renderBlock(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/BufferBuilder;)Z
    // with...
    //    INVOKESTATIC grondag/render_hooks/core/PipelineHooks.renderBlock(Lnet/minecraft/client/renderer/BlockRendererDispatcher;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos$MutableBlockPos;Lnet/minecraft/world/ChunkCache;Lnet/minecraft/client/renderer/BufferBuilder;)Z

    
    @SuppressWarnings("null")
    public byte[] patchBlockModelRenderer(String name, byte[] bytes, boolean obfuscated)
    {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        
        addField(
                classNode, 
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, 
                REGION_RENDER_CACHE_BUILDER_MATERIAL_BUFFER_FIELD, 
                lwrap(RENDER_BUFFER_MANAGER_CLASS), 
                null);
        
        Iterator<MethodNode> methods = classNode.methods.iterator();

        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            if ((m.name.equals("func_187493_a") || m.name.equals("renderModel")) && m.parameters.size() == 7) 
            {
                if(m.localVariables.get(9).desc.equals("Z") && m.localVariables.get(9).index == 9)
                {
                    for (int i = 0; i < m.instructions.size(); i++)
                    {
                        AbstractInsnNode next = m.instructions.get(i);
                        
                        // Add after flag local variable is set
                        if (next instanceof InsnNode 
                                && ((InsnNode)next).getOpcode() == ISTORE
                                && ((VarInsnNode)next).var == 9)
                        {
                            InsnList toAdd = new InsnList();
                            
                            // initalize the member we just created
                            toAdd.add(new VarInsnNode(ALOAD, 0));
                            toAdd.add(new TypeInsnNode(NEW, RENDER_BUFFER_MANAGER_CLASS));
                            toAdd.add(new InsnNode(DUP));
                            toAdd.add(new MethodInsnNode(INVOKESPECIAL, RENDER_BUFFER_MANAGER_CLASS, "<init>", "()V", false));
                            // forge sends us dots but ASM wants slashes
                            toAdd.add(new FieldInsnNode(PUTFIELD, REGION_RENDER_CACHE_BUILDER_CLASS.replace(".", "/"), REGION_RENDER_CACHE_BUILDER_MATERIAL_BUFFER_FIELD, lwrap(RENDER_BUFFER_MANAGER_CLASS)));
                            m.instructions.insertBefore(next, toAdd);
                            break;
                        }
                    }
                }
               
            }
        }
        
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
        
    }
    
    @SuppressWarnings("null")
    public byte[] patchRegionRenderCacheBuilder(String name, byte[] bytes, boolean obfuscated)
    {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        
        addField(
                classNode, 
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, 
                REGION_RENDER_CACHE_BUILDER_MATERIAL_BUFFER_FIELD, 
                lwrap(RENDER_BUFFER_MANAGER_CLASS), 
                null);
        
        Iterator<MethodNode> methods = classNode.methods.iterator();

        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            // Initializer isn't obfuscated
            if (m.name.equals("<init>")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    // Add at end
                    if (next instanceof InsnNode && ((InsnNode)next).getOpcode() == RETURN)
                    {
                        InsnList toAdd = new InsnList();
                        
                        // initalize the member we just created
                        toAdd.add(new VarInsnNode(ALOAD, 0));
                        toAdd.add(new TypeInsnNode(NEW, RENDER_BUFFER_MANAGER_CLASS));
                        toAdd.add(new InsnNode(DUP));
                        toAdd.add(new MethodInsnNode(INVOKESPECIAL, RENDER_BUFFER_MANAGER_CLASS, "<init>", "()V", false));
                        // forge sends us dots but ASM wants slashes
                        toAdd.add(new FieldInsnNode(PUTFIELD, REGION_RENDER_CACHE_BUILDER_CLASS.replace(".", "/"), REGION_RENDER_CACHE_BUILDER_MATERIAL_BUFFER_FIELD, lwrap(RENDER_BUFFER_MANAGER_CLASS)));
                        m.instructions.insertBefore(next, toAdd);
                        break;
                    }
                }
            }
        }
        
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
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
