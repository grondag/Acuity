package grondag.acuity;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.Iterator;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
public class ASMTransformer implements IClassTransformer
{
    private static boolean allPatchesSuccessful = true;
    
    // language translation won't be enabled while some patches are running
    private static final String msg_fail_patch_locate = "Unable to locate and patch %s";
    private static final String msg_fail_patch_vertex_format_element = "Unable to locate and patch isFirstOrUV() reference in VertexFormatElement.<init>";
    private static final String msg_patching_notice = "Patching %s";
    private static final String msg_patching_fail = "Unable to patch %s due to unexpected error ";
    private static final String msg_patching_fail_warning_1 = "Acuity Rendering API will be disabled and partial patches may cause problems.";
    private static final String msg_patching_fail_warning_2 = "Acuity Rendering API or a conflicting mod should be removed to prevent strangeness or crashing.";
    private static final String msg_fail_patch_gameloop_yield = "Unable to remove call to Thread.yield() in Minecraft game loop.  This error does not prevent Acuity from operating.";
    
    public static final boolean allPatchesSuccessful()
    {
        return allPatchesSuccessful;
    }
    
    private Consumer<ClassNode> patchVertexFormatElement = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        boolean worked = false;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            // Initializer isn't obfuscated
            if (m.name.equals("<init>")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    if(next instanceof MethodInsnNode)
                    {
                        MethodInsnNode ins = (MethodInsnNode)next;
                        if(ins.owner.equals("net/minecraft/client/renderer/vertex/VertexFormatElement")
                                && (ins.name.equals("isFirstOrUV") || ins.name.equals("func_177372_a"))
                                && ins.desc.equals("(ILnet/minecraft/client/renderer/vertex/VertexFormatElement$EnumUsage;)Z"))
                        {
                            ins.setOpcode(INVOKESTATIC);
                            ins.owner = "grondag/acuity/hooks/PipelineHooks";
                            ins.desc = "(Ljava/lang/Object;ILnet/minecraft/client/renderer/vertex/VertexFormatElement$EnumUsage;)Z";
                            ins.name = "isFirstOrUV";
                            ins.itf = false;
                            worked = true;
                            break;
                        }
                    }
                }
            }
        }
        if(!worked)
        {
            Acuity.INSTANCE.getLog().error(msg_fail_patch_vertex_format_element);
            allPatchesSuccessful = false;
        }
    };
    
    private Consumer<ClassNode> patchOpenGlHelper = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        boolean didVbo = false;
        boolean didGen = false;
        boolean didDel = false;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            if (m.name.equals("func_176075_f") || m.name.equals("useVbo")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    if(next.getOpcode() == GETSTATIC)
                    {
                        // delete all operations up until return
                        while(next.getOpcode() != IRETURN && i < m.instructions.size())
                        {
                            m.instructions.remove(next);
                            next = m.instructions.get(i);
                        }
                       
                        if(next.getOpcode() == IRETURN)
                        {
                            // insert call to our hook before return statement
                            m.instructions.insertBefore(next, new MethodInsnNode(INVOKESTATIC, "grondag/acuity/hooks/PipelineHooks", "useVbo", "()Z", false));
                            didVbo = true;
                        }
                        break;
                    }
                }
            }
            else if (m.name.equals("func_176073_e") || m.name.equals("glGenBuffers")) 
            {
                m.instructions.clear();
                m.instructions.add(new MethodInsnNode(INVOKESTATIC, "grondag/acuity/opengl/GLBufferStore", "glGenBuffers", "()I", false));
                m.instructions.add(new InsnNode(IRETURN));
                didGen = true;
            }
            else if (m.name.equals("func_176074_g") || m.name.equals("glDeleteBuffers")) 
            {
                m.instructions.clear();
                m.instructions.add(new VarInsnNode(ILOAD, 0));
                m.instructions.add(new MethodInsnNode(INVOKESTATIC, "grondag/acuity/opengl/GLBufferStore", "glDeleteBuffers", "(I)V", false));
                m.instructions.add(new InsnNode(RETURN));
                didDel = true;
            }
            
            if(didVbo && didGen && didDel)
                break;
        }
        
        
        if(!didVbo)
        {
            Acuity.INSTANCE.getLog().error(String.format(msg_fail_patch_locate, "OpenGlHelper.useVbo()"));
            allPatchesSuccessful = false;
        }
    };

    private Consumer<ClassNode> patchMinecraft = classNode ->
    {
        if(!AcuityCore.config.disableYieldInGameLoop)
            return;
        
        Iterator<MethodNode> methods = classNode.methods.iterator();
        boolean worked = false;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            if (m.name.equals("func_71411_J") || m.name.equals("runGameLoop")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    if(next.getOpcode() == INVOKESTATIC && ((MethodInsnNode)next).name.equals("yield"))
                    {
                        m.instructions.set(next, new InsnNode(NOP));
                        worked = true;
                        break;
                    }
                }
                break;
            }
            
        }
        if(!worked)
        {
            Acuity.INSTANCE.getLog().error(msg_fail_patch_gameloop_yield);
        }
    };
    
    
    @SuppressWarnings("null")
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        // give up if we had a failure
        if(!allPatchesSuccessful) return basicClass;
        
        final boolean obfuscated = name.compareTo(transformedName) != 0;
        
        if (transformedName.equals("net.minecraft.client.renderer.vertex.VertexFormatElement"))
            return patch(transformedName, basicClass, obfuscated, patchVertexFormatElement, ClassWriter.COMPUTE_FRAMES); 
        
        if (transformedName.equals("net.minecraft.client.renderer.OpenGlHelper"))
            return patch(transformedName, basicClass, obfuscated, patchOpenGlHelper, ClassWriter.COMPUTE_FRAMES); 
        
        if (transformedName.equals("net.minecraft.client.Minecraft"))
            return patch(transformedName, basicClass, obfuscated, patchMinecraft); 
        
        return basicClass;
    }
    
    public byte[] patch(String name, byte[] bytes, boolean obfuscated, Consumer<ClassNode> patcher)
    {
        return patch(name, bytes, obfuscated, patcher, 0);
    }
    
    public byte[] patch(String name, byte[] bytes, boolean obfuscated, Consumer<ClassNode> patcher, int flags)
    {
        Acuity.INSTANCE.getLog().info(String.format(msg_patching_notice, name));

        byte[] result = bytes;
        try
        {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, 0);
            patcher.accept(classNode);
            if(allPatchesSuccessful)
            {
                ClassWriter classWriter = new ClassWriter(flags);
                classNode.accept(classWriter);
                result = classWriter.toByteArray();
            }
        }
        catch(Exception e)
        {
            Acuity.INSTANCE.getLog().error(String.format(msg_patching_fail, name), e);
            allPatchesSuccessful = false;
        }
        
        if(!allPatchesSuccessful)
        {
            Acuity.INSTANCE.getLog().warn(msg_patching_fail_warning_1);
            Acuity.INSTANCE.getLog().warn(msg_patching_fail_warning_2);
        }
        return result;
    }
}
