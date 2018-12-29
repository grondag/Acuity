package grondag.acuity.extension;


public interface AcuityChunkVisibility
{
    public Object getVisibilityData();
    
    public void setVisibilityData( Object data);

    public void releaseVisibilityData();

    public void clear();
}
