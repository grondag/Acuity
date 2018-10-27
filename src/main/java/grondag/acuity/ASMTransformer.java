package grondag.acuity;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IFEQ;
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
import org.objectweb.asm.tree.FieldNode;
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
    private static final String msg_fail_patch_compiled_chunk = "Unable to locate and patch setVisibility in CompiledChunk";
    private static final String msg_fail_patch_vertex_format_element = "Unable to locate and patch isFirstOrUV() reference in VertexFormatElement.<init>";
    private static final String msg_patching_notice = "Patching %s";
    private static final String msg_patching_fail = "Unable to patch %s due to unexpected error ";
    private static final String msg_patching_fail_warning_1 = "Acuity Rendering API will be disabled and partial patches may cause problems.";
    private static final String msg_patching_fail_warning_2 = "Acuity Rendering API or a conflicting mod should be removed to prevent strangeness or crashing.";
    private static final String msg_field_already_exists = "Unable to add field %s to class %s - field already exists.";
    private static final String msg_fail_patch_gameloop_yield = "Unable to remove call to Thread.yield() in Minecraft game loop.  This error does not prevent Acuity from operating.";
    
    public static final boolean allPatchesSuccessful()
    {
        return allPatchesSuccessful;
    }
    
//    private Consumer<ClassNode> patchRenderChunk = classNode ->
//    {
//        Iterator<MethodNode> methods = classNode.methods.iterator();
//        boolean newWorked = false;
//        boolean invokedWorked = false;
//        boolean setPositionWorked = false;
//        
//        while (methods.hasNext())
//        {
//            MethodNode m = methods.next();
//            
//            // Initializer isn't obfuscated
//            if (m.name.equals("<init>")) 
//            {
//                for (int i = 0; i < m.instructions.size(); i++)
//                {
//                    AbstractInsnNode next = m.instructions.get(i);
//                    
//                    if(next.getOpcode() == NEW)
//                    {
//                        TypeInsnNode op = (TypeInsnNode)next;
//                        if(op.desc.equals("net/minecraft/client/renderer/vertex/VertexBuffer"))
//                        {
//                            op.desc = "grondag/acuity/core/CompoundVertexBuffer";
//                            newWorked = true;
//                        }
//                    }
//                    // constructors are always INVOKESPECIAL
//                    else if(next.getOpcode() == INVOKESPECIAL)
//                    {
//                        MethodInsnNode op = (MethodInsnNode)next;
//                        if(op.owner.equals("net/minecraft/client/renderer/vertex/VertexBuffer") && op.name.equals("<init>"))
//                        {
//                            op.owner = "grondag/acuity/core/CompoundVertexBuffer";
//                            op.itf = false;
//                            invokedWorked = true;
//                            break;
//                        }
//                    }
//                }
//            }
//            else if (m.name.equals("func_189562_a") || m.name.equals("setPosition")) 
//            {
//                for (int i = 0; i < m.instructions.size(); i++)
//                {
//                    AbstractInsnNode next = m.instructions.get(i);
//                    
//                    // private but may have been transformed so could be either of theseL
//                    if(next instanceof MethodInsnNode && isStringOneOf(((MethodInsnNode)next).name, "func_178567_n", "initModelviewMatrix"))
//                    {
//                        MethodInsnNode op = (MethodInsnNode)next;
//                        op.setOpcode(INVOKESTATIC);
//                        op.owner = "grondag/acuity/hooks/PipelineHooks";
//                        op.name = "renderChunkInitModelViewMatrix";
//                        op.desc = "(Lnet/minecraft/client/renderer/chunk/RenderChunk;)V";
//                        setPositionWorked = true;
//                        break;
//                    }
//                }
//            }
//        }
//        if(!(newWorked && invokedWorked))
//        {
//            Acuity.INSTANCE.getLog().error(msg_fail_patch_render_chunk);
//            allPatchesSuccessful = false;
//        }
//        if(!setPositionWorked)
//            Acuity.INSTANCE.getLog().warn(msg_fail_patch_render_chunk_set_position);
//    };
    
    private Consumer<ClassNode> patchCompiledChunk = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        boolean worked = false;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            if (m.name.equals("func_178488_a") || m.name.equals("setVisibility")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    if(next.getOpcode() == RETURN)
                    {
                        m.instructions.insertBefore(next, new VarInsnNode(ALOAD, 0));
                        m.instructions.insertBefore(next, new MethodInsnNode(INVOKESTATIC, "grondag/acuity/hooks/PipelineHooks", "mergeRenderLayers", "(Lnet/minecraft/client/renderer/chunk/CompiledChunk;)V", false));
                        worked = true;
                        break;
                    }
                }
                break;
            }
            
        }
        if(!worked)
        {
            Acuity.INSTANCE.getLog().error(msg_fail_patch_compiled_chunk);
            allPatchesSuccessful = false;
        }
    };
    
    private Consumer<ClassNode> patchChunkRenderDispatcher = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        
        boolean worked = false;
        
        while (methods.hasNext() && !worked)
        {
            MethodNode m = methods.next();
            
            if (m.name.equals("func_188245_a") || m.name.equals("uploadChunk"))
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    if(next.getOpcode() == INVOKESTATIC)
                    {
                        MethodInsnNode op = (MethodInsnNode)next;
                        
                        // conditional block we want to modify starts with a check for vbo enabled
                        if(op.owner.equals("net/minecraft/client/renderer/OpenGlHelper")
                                && (op.name.equals("func_176075_f") || op.name.equals("useVbo")))
                        {
                            // next one should be IFEQ, confirm
                            int j = i+1;
                            
                            next = m.instructions.get(j++);
                            if(next.getOpcode() != IFEQ)
                                break;
                            
                            // skip labels and line numbers
                            do
                            {
                                next = m.instructions.get(j++);
                            } while(next.getOpcode() != ALOAD && j < m.instructions.size());
                            
                            if(next.getOpcode() != ALOAD)
                                break;
                            
                            // no longer needed to push *this* on stack before calling static hook
                            if(((VarInsnNode)next).var == 0)
                            {
                                m.instructions.remove(next);
                                next = m.instructions.get(j++);
                            }
                            
                            // skip until call to vbo upload instance method
                            // might be INVOKESPECIAL or might be INVOKEVIRTUAL if access transfomer made public
                            // so check the node class instead of the op code
                            boolean looking = true;
                            do
                            {
                                next = m.instructions.get(j++);
                                if(next instanceof MethodInsnNode)
                                {
                                    MethodInsnNode ins = (MethodInsnNode)next;
                                    if(ins.owner.equals("net/minecraft/client/renderer/chunk/ChunkRenderDispatcher")
                                            && (ins.name.equals("func_178506_a") || ins.name.equals("uploadVertexBuffer"))
                                            && ins.desc.equals("(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/client/renderer/vertex/VertexBuffer;)V"))
                                    {
                                        ins.setOpcode(INVOKESTATIC);
                                        ins.owner = "grondag/acuity/hooks/PipelineHooks";
                                        ins.name = "uploadVertexBuffer";
                                        ins.itf = false;
                                        looking = false;
                                    }
                                }
                            } while(looking && j < m.instructions.size());

                            if(looking)
                                break;

                            worked = true;
                            break;
                        }
                    }
                }
                break;
            }
        }
        if(!worked)
        {
            Acuity.INSTANCE.getLog().error(String.format(msg_fail_patch_locate, "net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.uploadChunk"));
            allPatchesSuccessful = false;
        }
    };
    
    
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
        
        if (transformedName.equals("net.minecraft.client.renderer.chunk.CompiledChunk"))
            return patch(transformedName, basicClass, obfuscated, patchCompiledChunk); 
        
        if (transformedName.equals("net.minecraft.client.renderer.chunk.ChunkRenderDispatcher"))
            return patch(transformedName, basicClass, obfuscated, patchChunkRenderDispatcher, ClassWriter.COMPUTE_FRAMES); 
        
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
    
    @SuppressWarnings("unused")
    private static void addField(ClassNode classNode, int opCodes, String fieldName, String type, Object init)
    {
        for(FieldNode f : classNode.fields)
        {
            if (f.name.equals(fieldName)) 
            {
                Acuity.INSTANCE.getLog().error(String.format(msg_field_already_exists, fieldName, classNode.name));
                return;
            }
        }
        classNode.fields.add(new FieldNode(opCodes, fieldName, type, null, init));
    }
}
