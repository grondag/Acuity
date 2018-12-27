package grondag.acuity.buffering;

import java.util.concurrent.ArrayBlockingQueue;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class DelegateLists
{
    private static final ArrayBlockingQueue<ObjectArrayList<DrawableChunkDelegate>> delegateLists = new ArrayBlockingQueue<>(4096);

    static ObjectArrayList<DrawableChunkDelegate> getReadyDelegateList()
    {
        ObjectArrayList<DrawableChunkDelegate> result = delegateLists.poll();
        if(result == null)
            result = new ObjectArrayList<>();
        return result;
    }
    
    static void releaseDelegateList(ObjectArrayList<DrawableChunkDelegate> list)
    {
        if(!list.isEmpty())
            list.clear();
        delegateLists.offer(list);
    }
}
