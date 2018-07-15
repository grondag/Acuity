package grondag.render_hooks;

import static org.objectweb.asm.Opcodes.*;

import java.util.Iterator;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
public class ASMTransformer implements IClassTransformer
{
    private static boolean allPatchesSuccessful = true;
    
    public static final boolean allPatchesSuccessful()
    {
        return allPatchesSuccessful;
    }
    
    private Consumer<ClassNode> patchBlockRendererDispatcher = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        
        boolean blockWorked = false;
        boolean fluidWorked = false;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            if (m.name.equals("func_175018_a") || m.name.equals("renderBlock"))
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    // public, so will always be INVOKEVIRTUAL
                    if(next.getOpcode() == INVOKEVIRTUAL)
                    {
                        MethodInsnNode op = (MethodInsnNode)next;
                        if(op.owner.equals("net/minecraft/client/renderer/BlockModelRenderer")
                                && (op.name.equals("func_178267_a") || op.name.equals("renderModel")))
                        {
                            op.setOpcode(INVOKESTATIC);
                            op.owner = "grondag/render_hooks/core/PipelineHooks";
                            op.name = "renderModel";
                            op.desc = "(Lnet/minecraft/client/renderer/BlockModelRenderer;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;Z)Z";
                            op.itf = false;
                            blockWorked = true;
                        }
                        else if(op.owner.equals("net/minecraft/client/renderer/BlockFluidRenderer")
                                && (op.name.equals("func_178270_a") || op.name.equals("renderFluid")))
                        {
                            op.setOpcode(INVOKESTATIC);
                            op.owner = "grondag/render_hooks/core/PipelineHooks";
                            op.name = "renderFluid";
                            op.desc = "(Lnet/minecraft/client/renderer/BlockFluidRenderer;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;)Z";
                            op.itf = false;
                            fluidWorked = true;
                        }
                        
                        if(blockWorked && fluidWorked)
                            break;
                    }
                }
                break;
            }
        }
        if(!blockWorked || !fluidWorked)
        {
            RenderHooks.INSTANCE.getLog().error("Unable to locate and patch net/minecraft/client/renderer/BlockModelRenderer.renderBlock");
            allPatchesSuccessful = false;
        }
    };
    
    private Consumer<ClassNode> patchRegionRenderCacheBuilder = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        int newCount = 0;
        int invokeCount = 0;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            // Initializer isn't obfuscated
            if (m.name.equals("<init>")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    if(next.getOpcode() == NEW)
                    {
                        TypeInsnNode op = (TypeInsnNode)next;
                        if(op.desc.equals("net/minecraft/client/renderer/BufferBuilder"))
                        {
                            op.desc = "grondag/render_hooks/core/CompoundBufferBuilder";
                            newCount++;
                        }
                    }
                    // constructors are always INVOKESPECIAL
                    else if(next.getOpcode() == INVOKESPECIAL)
                    {
                        MethodInsnNode op = (MethodInsnNode)next;
                        if(op.owner.equals("net/minecraft/client/renderer/BufferBuilder") && op.name.equals("<init>"))
                        {
                            op.owner = "grondag/render_hooks/core/CompoundBufferBuilder";
                            op.itf = false;
                            invokeCount++;
                        }
                    }
                }
            }
        }
        if(newCount != 4 || invokeCount != 4)
        {
            RenderHooks.INSTANCE.getLog().error("Unable to locate four expected BufferBuilder instances in RegionRenderCacheBuilder.<init>");
            allPatchesSuccessful = false;
        }
    };
    
    
    private Consumer<ClassNode> patchRenderChunk = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        boolean newWorked = false;
        boolean invokedWorked = false;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            // Initializer isn't obfuscated
            if (m.name.equals("<init>")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    if(next.getOpcode() == NEW)
                    {
                        TypeInsnNode op = (TypeInsnNode)next;
                        if(op.desc.equals("net/minecraft/client/renderer/vertex/VertexBuffer"))
                        {
                            op.desc = "grondag/render_hooks/core/CompoundVertexBuffer";
                            newWorked = true;
                        }
                    }
                    // constructors are always INVOKESPECIAL
                    else if(next.getOpcode() == INVOKESPECIAL)
                    {
                        MethodInsnNode op = (MethodInsnNode)next;
                        if(op.owner.equals("net/minecraft/client/renderer/vertex/VertexBuffer") && op.name.equals("<init>"))
                        {
                            op.owner = "grondag/render_hooks/core/CompoundVertexBuffer";
                            op.itf = false;
                            invokedWorked = true;
                            break;
                        }
                    }
                }
            }
        }
        if(!newWorked || !invokedWorked)
        {
            RenderHooks.INSTANCE.getLog().error("Unable to locate VertexBuffer instance in RenderChunk.<init>");
            allPatchesSuccessful = false;
        }
    };
    
    private Consumer<ClassNode> patchListChunkFactory = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        boolean newWorked = false;
        boolean invokedWorked = false;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            if (m.name.equals("func_189565_a") || m.name.equals("create")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    if(next.getOpcode() == NEW)
                    {
                        TypeInsnNode op = (TypeInsnNode)next;
                        if(op.desc.equals("net/minecraft/client/renderer/chunk/ListedRenderChunk"))
                        {
                            op.desc = "grondag/render_hooks/core/CompoundListedRenderChunk";
                            newWorked = true;
                        }
                    }
                    // constructors are always INVOKESPECIAL
                    else if(next.getOpcode() == INVOKESPECIAL)
                    {
                        MethodInsnNode op = (MethodInsnNode)next;
                        if(op.owner.equals("net/minecraft/client/renderer/chunk/ListedRenderChunk") && op.name.equals("<init>"))
                        {
                            op.owner = "grondag/render_hooks/core/CompoundListedRenderChunk";
                            op.itf = false;
                            invokedWorked = true;
                            break;
                        }
                    }
                }
            }
        }
        if(!newWorked || !invokedWorked)
        {
            RenderHooks.INSTANCE.getLog().error("Unable to locate ListedRenderChunk instance in ListChunkFactory.<init>");
            allPatchesSuccessful = false;
        }
    };
    
    private Consumer<ClassNode> patchChunkRenderDispatcher = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        
        boolean worked = false;
        
        while (methods.hasNext())
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
                            
                            // push this on stack before the other arguments
                            m.instructions.insertBefore(next, new VarInsnNode(ALOAD, 0));
                            
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
                                        ins.owner = "grondag/render_hooks/core/PipelineHooks";
                                        ins.name = "uploadVertexBuffer";
                                        ins.itf = false;
                                        looking = false;
                                    }
                                }
                            } while(looking && j < m.instructions.size());

                            if(looking)
                                break;
                            
                            // skip to next ALOAD
                            do
                            {
                                next = m.instructions.get(j++);
                            } while(next.getOpcode() != ALOAD && j < m.instructions.size());
                            
                            if(next.getOpcode() != ALOAD)
                                break;
                            
                            // push this on stack before the other arguments
                            m.instructions.insertBefore(next, new VarInsnNode(ALOAD, 0));
                            
                            // skip until call to display list upload instance method
                            // might be INVOKESPECIAL or might be INVOKEVIRTUAL if access transfomer made public
                            // so check the node class instead of the op code
                            looking = true;
                            do
                            {
                                next = m.instructions.get(j++);
                                if(next instanceof MethodInsnNode)
                                {
                                    MethodInsnNode ins = (MethodInsnNode)next;
                                    if(ins.owner.equals("net/minecraft/client/renderer/chunk/ChunkRenderDispatcher")
                                            && (ins.name.equals("func_178510_a") || ins.name.equals("uploadDisplayList"))
                                            && ins.desc.equals("(Lnet/minecraft/client/renderer/BufferBuilder;ILnet/minecraft/client/renderer/chunk/RenderChunk;)V"))
                                    {
                                        ins.setOpcode(INVOKESTATIC);
                                        ins.owner = "grondag/render_hooks/core/PipelineHooks";
                                        ins.name = "uploadDisplayList";
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
                if(!worked)
                {
                    RenderHooks.INSTANCE.getLog().error("Unable to patch net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.uploadChunk");
                    allPatchesSuccessful = false;
                    return;
                }
            }
        }
        if(!worked)
        {
            RenderHooks.INSTANCE.getLog().error("Unable to locate net/minecraft/client/renderer/chunk/ChunkRenderDispatcher.uploadChunk");
            allPatchesSuccessful = false;
        }
    };
    
    private Consumer<ClassNode> patchRenderGlobal = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        int newCount = 0;
        int invokeCount = 0;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            // patching two different locations
            if (m.name.equals("func_72712_a") || m.name.equals("loadRenderers") || m.name.equals("<init>")) 
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
                    
                    if(next.getOpcode() == NEW)
                    {
                        TypeInsnNode op = (TypeInsnNode)next;
                        if(op.desc.equals("net/minecraft/client/renderer/RenderList"))
                        {
                            op.desc = "grondag/render_hooks/core/PipelinedRenderList";
                            newCount++;
                        }
                        else if(op.desc.equals("net/minecraft/client/renderer/VboRenderList"))
                        {
                            op.desc = "grondag/render_hooks/core/PipelinedVboRenderList";
                            newCount++;
                        }
                    }
                    // constructors are always INVOKESPECIAL
                    else if(next.getOpcode() == INVOKESPECIAL)
                    {
                        MethodInsnNode op = (MethodInsnNode)next;
                        if(op.owner.equals("net/minecraft/client/renderer/RenderList") && op.name.equals("<init>"))
                        {
                            op.owner = "grondag/render_hooks/core/PipelinedRenderList";
                            op.itf = false;
                            invokeCount++;
                        }
                        else if(op.owner.equals("net/minecraft/client/renderer/VboRenderList") && op.name.equals("<init>"))
                        {
                            op.owner = "grondag/render_hooks/core/PipelinedVboRenderList";
                            op.itf = false;
                            invokeCount++;
                        }
                    }
                }
            }
        }
        if(newCount != 4 || invokeCount != 4)
        {
            RenderHooks.INSTANCE.getLog().error("Unable to locate all VBORenderList & RenderList instances in RenderGlobal");
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
                            ins.owner = "grondag/render_hooks/core/PipelineHooks";
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
            RenderHooks.INSTANCE.getLog().error("Unable to locate isFirstOrUV call in VertexFormatElement.<init>");
            allPatchesSuccessful = false;
        }
    };
    
    @SuppressWarnings("null")
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        // give up if we had a failure
        if(!allPatchesSuccessful) return basicClass;
        
        final boolean obfuscated = name.compareTo(transformedName) != 0;
        
        if(transformedName.equals("net.minecraft.client.renderer.BlockRendererDispatcher"))
            return patch(transformedName, basicClass, obfuscated, patchBlockRendererDispatcher);
        
        if (transformedName.equals("net.minecraft.client.renderer.RegionRenderCacheBuilder"))
            return patch(transformedName, basicClass, obfuscated, patchRegionRenderCacheBuilder);
        
        if (transformedName.equals("net.minecraft.client.renderer.chunk.RenderChunk"))
            return patch(transformedName, basicClass, obfuscated, patchRenderChunk); 
        
        if (transformedName.equals("net.minecraft.client.renderer.chunk.ListChunkFactory"))
            return patch(transformedName, basicClass, obfuscated, patchListChunkFactory); 
        
        if (transformedName.equals("net.minecraft.client.renderer.chunk.ChunkRenderDispatcher"))
            return patch(transformedName, basicClass, obfuscated, patchChunkRenderDispatcher, ClassWriter.COMPUTE_FRAMES); 
        
        if (transformedName.equals("net.minecraft.client.renderer.RenderGlobal"))
            return patch(transformedName, basicClass, obfuscated, patchRenderGlobal); 
        
        if (transformedName.equals("net.minecraft.client.renderer.vertex.VertexFormatElement"))
            return patch(transformedName, basicClass, obfuscated, patchVertexFormatElement, ClassWriter.COMPUTE_FRAMES); 
        
        return basicClass;
    }
    
    public byte[] patch(String name, byte[] bytes, boolean obfuscated, Consumer<ClassNode> patcher)
    {
        return patch(name, bytes, obfuscated, patcher, 0);
    }
    
    public byte[] patch(String name, byte[] bytes, boolean obfuscated, Consumer<ClassNode> patcher, int flags)
    {
        RenderHooks.INSTANCE.getLog().info("Patching " + name);

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
            RenderHooks.INSTANCE.getLog().error("Unable to patch " + name + " due to unexpected error:", e);
            allPatchesSuccessful = false;
        }
        
        if(!allPatchesSuccessful)
        {
            RenderHooks.INSTANCE.getLog().warn("RenderHooks will be disabled and partial patches may cause problems.");
            RenderHooks.INSTANCE.getLog().warn("RenderHooks or a conflicting mod should be removed to prevent strangeness or crashing.");
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
                RenderHooks.INSTANCE.getLog().error("Unable to add field %s to class %s - field already exists.", fieldName, classNode.name );
                return;
            }
        }
        classNode.fields.add(new FieldNode(opCodes, fieldName, type, null, init));
    }
    
}
