package grondag.render_hooks.api;

public enum TextureFormat
{
    SINGLE,
    DOUBLE,
    TRIPLE;

    public int layerCount()
    {
        return this.ordinal() + 1;
    }
}
