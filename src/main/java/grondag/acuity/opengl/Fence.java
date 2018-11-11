package grondag.acuity.opengl;

public abstract class Fence
{
    private boolean isSet = false;
    
    public final boolean isReached()
    {
        return !isSet || isReachedImpl();
    }
    
    protected abstract boolean isReachedImpl();

    public final void set()
    {
        isSet = true;
        setImpl();
    }
    protected abstract void setImpl();

    public void deleteGlResources()
    {
        
    }
}