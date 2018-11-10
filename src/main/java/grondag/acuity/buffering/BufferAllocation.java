package grondag.acuity.buffering;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

public abstract class BufferAllocation implements IBufferAllocation
{
    protected BufferSlice slice;
    protected final int startVertex;
    protected int quadCount;
    protected AtomicBoolean isFree = new AtomicBoolean(true);
    protected boolean isDeleted = false;
    
    protected BufferAllocation(BufferSlice slice, int startVertex)
    {
        this.startVertex = startVertex;
        this.slice = slice;
    }
    
    @Override
    public int startVertex()
    {
        assert !isDeleted;
        return startVertex;
    }

    @Override
    public int quadCount()
    {
        assert !isDeleted;
        return quadCount;
    }

    @Override
    public void setQuadCount(int quadCount)
    {
        assert !isDeleted;
        this.quadCount = quadCount;
    }
    
    @Override
    public boolean claim()
    {
        assert !isDeleted;
        return this.isFree.compareAndSet(true, false);
    }
    
    @Override
    public BufferSlice slice()
    {
        return this.slice;
    }

    public static class Root extends BufferAllocation
    {
        protected final MappedBuffer buffer;
        
        protected Root(BufferSlice slice, MappedBuffer buffer)
        {
            super(slice, 0);
            this.buffer = buffer;
            assert slice.isMax;
        }
        
        @Override
        public MappedBuffer buffer()
        {
            assert !isDeleted;
            return buffer;
        }
        
        @Override
        public void release()
        {
            assert !isDeleted;
            this.isFree.set(true);
            MappedBufferStore.acceptFree(this);
        }
    }
    
    public static class Slice extends BufferAllocation
    {
        private final BufferAllocation parent;
        protected @Nullable Slice buddy;
        
        public Slice(BufferSlice slice, int startVertex, BufferAllocation parent)
        {
            super(slice, startVertex);
            this.parent = parent;
            assert parent.slice.divisionLevel == slice.divisionLevel - 1;
        }
        
        @Override
        public MappedBuffer buffer()
        {
            return parent.buffer();
        }
        
        @SuppressWarnings("null")
        @Override
        public void release()
        {
            assert !this.isDeleted;
            assert !this.isFree.get();
            
            if(this.buddy.claim())
            {
                this.buddy.isDeleted = true;
                this.isDeleted = true;
                this.parent.release();
            }
            else
            {
                this.isFree.set(true);
                MappedBufferStore.acceptFree(this);
            }
        }
    }
}
