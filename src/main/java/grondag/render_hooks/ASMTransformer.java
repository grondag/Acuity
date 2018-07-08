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

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
public class ASMTransformer implements IClassTransformer
{
    private static boolean allPatchesSuccessful = true;
    
    public static boolean allPatchesSuccessful()
    {
        return allPatchesSuccessful;
    }
    
    private Consumer<ClassNode> patchBlockRendererDispatcher = classNode ->
    {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        
        boolean worked = false;
        
        while (methods.hasNext())
        {
            MethodNode m = methods.next();
            
            if (m.name.equals("func_175018_a") || m.name.equals("renderBlock"))
            {
                for (int i = 0; i < m.instructions.size(); i++)
                {
                    AbstractInsnNode next = m.instructions.get(i);
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
            RenderHooks.INSTANCE.getLog().error("Unable to locate net/minecraft/client/renderer/BlockModelRenderer.renderBlock");
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
    
    @SuppressWarnings("null")
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        // give up if we had a failure
        if(!allPatchesSuccessful) return basicClass;
        
        final boolean obfuscated = name.compareTo(transformedName) != 0;
        
        if(transformedName.equals("net.minecraft.client.renderer.BlockRendererDispatcher"))
            return patch(name, basicClass, obfuscated, patchBlockRendererDispatcher);
        
        if (transformedName.equals("net.minecraft.client.renderer.RegionRenderCacheBuilder"))
            return patch(name, basicClass, obfuscated, patchRegionRenderCacheBuilder);
        
        if (transformedName.equals("net.minecraft.client.renderer.chunk.RenderChunk"))
            return patch(name, basicClass, obfuscated, patchRenderChunk); 
        
        if (transformedName.equals("net.minecraft.client.renderer.chunk.ListChunkFactory"))
            return patch(name, basicClass, obfuscated, patchListChunkFactory); 
        
        
        return basicClass;
    }
    
    public byte[] patch(String name, byte[] bytes, boolean obfuscated, Consumer<ClassNode> patcher)
    {
        RenderHooks.INSTANCE.getLog().info("Patching " + name);

        byte[] result = bytes;
        try
        {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, 0);
            patcher.accept(classNode);
            ClassWriter classWriter = new ClassWriter(0);
            classNode.accept(classWriter);
            result = classWriter.toByteArray();
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
