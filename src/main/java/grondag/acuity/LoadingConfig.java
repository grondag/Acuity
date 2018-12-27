package grondag.acuity;

import java.io.File;

public class LoadingConfig
{
    public static LoadingConfig INSTANCE = new LoadingConfig(new File("config/acuity.cfg"));
            
    public final boolean disableYieldInGameLoop;
    public final boolean enableRenderStats;
    public final boolean enableFluidStats;
    public final boolean enableBlockStats;

    private LoadingConfig(File file)
    {
        // TODO: read configs
//        if (!file.exists())
//        {
            disableYieldInGameLoop = true;
            enableRenderStats = false;
            enableFluidStats = false;
            enableBlockStats = false;
//            return;
//        }

    }
}
