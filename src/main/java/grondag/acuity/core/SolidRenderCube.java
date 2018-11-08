package grondag.acuity.core;

import java.util.function.Consumer;

import grondag.acuity.api.PipelineManager;
import grondag.acuity.buffering.SolidDrawableChunkDelegate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SolidRenderCube implements Consumer<SolidDrawableChunkDelegate>
{
    public final ObjectArrayList<SolidDrawableChunkDelegate>[] pipelineLists;
    
    public SolidRenderCube()
    {
        final int size = PipelineManager.INSTANCE.pipelineCount();
        @SuppressWarnings("unchecked")
        ObjectArrayList<SolidDrawableChunkDelegate>[] buffers = new ObjectArrayList[size];
        for(int i = 0; i < size; i++)
        {
            buffers[i] = new ObjectArrayList<SolidDrawableChunkDelegate>();
        }
        this.pipelineLists = buffers;
    }

    @Override
    public void accept(@SuppressWarnings("null") SolidDrawableChunkDelegate d)
    {
        pipelineLists[d.getPipeline().getIndex()].add(d);   
    }
}
