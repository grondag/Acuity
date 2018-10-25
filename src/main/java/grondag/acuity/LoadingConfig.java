package grondag.acuity;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class LoadingConfig
{
    public boolean disableYieldInGameLoop;
    public boolean enableRenderStats;
    public boolean enableFluidStats;
    public boolean enableBlockStats;

    public LoadingConfig(File file)
    {
        if (!file.exists()) {
            return;
        }

        Configuration config = new Configuration(file);
        disableYieldInGameLoop = config.get(Configuration.CATEGORY_GENERAL, "disableYieldInGameLoop", true).getBoolean();
        enableRenderStats = config.get(Configuration.CATEGORY_GENERAL, "enableRenderStats", false).getBoolean();
        enableFluidStats = config.get(Configuration.CATEGORY_GENERAL, "enableFluidStats", false).getBoolean();
        enableBlockStats = config.get(Configuration.CATEGORY_GENERAL, "enableBlockStats", false).getBoolean();
    }
}
