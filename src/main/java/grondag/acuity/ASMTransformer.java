package grondag.acuity;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.NOP;

import java.util.Iterator;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
public class ASMTransformer implements IClassTransformer
{
    private static boolean allPatchesSuccessful = true;
    
    // language translation won't be enabled while some patches are running
    private static final String msg_patching_notice = "Patching %s";
    private static final String msg_patching_fail = "Unable to patch %s due to unexpected error ";
    private static final String msg_patching_fail_warning_1 = "Acuity Rendering API will be disabled and partial patches may cause problems.";
    private static final String msg_patching_fail_warning_2 = "Acuity Rendering API or a conflicting mod should be removed to prevent strangeness or crashing.";
    private static final String msg_fail_patch_gameloop_yield = "Unable to remove call to Thread.yield() in Minecraft game loop.  This error does not prevent Acuity from operating.";
    
    public static final boolean allPatchesSuccessful()
    {
        return allPatchesSuccessful;
    }

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
