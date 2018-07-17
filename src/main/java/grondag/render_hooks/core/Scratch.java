package grondag.render_hooks.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class Scratch
{
    private boolean lightmapUpdateNeeded;
    private Minecraft mc;
    private float torchFlickerX;
    private float bossColorModifier;
    private int bossColorModifierPrev;
    private int[] lightmapColors;

    private void updateLightmap(float partialTicks)
    {
        if (this.lightmapUpdateNeeded)
        {
            this.mc.mcProfiler.startSection("lightTex");
            World world = this.mc.world;

            if (world != null)
            {
                float sunBrightness = world.getSunBrightness(1.0F);
                float sunPlusAmbient = sunBrightness * 0.95F + 0.05F;

                for (int i = 0; i < 256; ++i)
                {
                    float worldScaledSunPlusAmbient = world.provider.getLightBrightnessTable()[i / 16] * sunPlusAmbient;
                    float worldScaledTorchWithFlicker = world.provider.getLightBrightnessTable()[i % 16] * (this.torchFlickerX * 0.1F + 1.5F);

                    if (world.getLastLightningBolt() > 0)
                    {
                        worldScaledSunPlusAmbient = world.provider.getLightBrightnessTable()[i / 16];
                    }

                    float sun65 = worldScaledSunPlusAmbient * (sunBrightness * 0.65F + 0.35F);
                    float alsoSun65 = worldScaledSunPlusAmbient * (sunBrightness * 0.65F + 0.35F);
                    float torch6twice = worldScaledTorchWithFlicker * ((worldScaledTorchWithFlicker * 0.6F + 0.4F) * 0.6F + 0.4F);
                    float torchSquared6 = worldScaledTorchWithFlicker * (worldScaledTorchWithFlicker * worldScaledTorchWithFlicker * 0.6F + 0.4F);
                    float red = sun65 + worldScaledTorchWithFlicker;
                    float green = alsoSun65 + torch6twice;
                    float blue = worldScaledSunPlusAmbient + torchSquared6;
                    red = red * 0.96F + 0.03F;
                    green = green * 0.96F + 0.03F;
                    blue = blue * 0.96F + 0.03F;

                    if (this.bossColorModifier > 0.0F)
                    {
                        float f11 = this.bossColorModifierPrev + (this.bossColorModifier - this.bossColorModifierPrev) * partialTicks;
                        red = red * (1.0F - f11) + red * 0.7F * f11;
                        green = green * (1.0F - f11) + green * 0.6F * f11;
                        blue = blue * (1.0F - f11) + blue * 0.6F * f11;
                    }

                    if (world.provider.getDimensionType().getId() == 1)
                    {
                        red = 0.22F + worldScaledTorchWithFlicker * 0.75F;
                        green = 0.28F + torch6twice * 0.75F;
                        blue = 0.25F + torchSquared6 * 0.75F;
                    }

                    float[] colors = {red, green, blue};
                    world.provider.getLightmapColors(partialTicks, sunBrightness, worldScaledSunPlusAmbient, worldScaledTorchWithFlicker, colors);
                    red = colors[0]; green = colors[1]; blue = colors[2];

                    // Forge: fix MC-58177
                    red = MathHelper.clamp(red, 0f, 1f);
                    green = MathHelper.clamp(green, 0f, 1f);
                    blue = MathHelper.clamp(blue, 0f, 1f);

                    if (this.mc.player.isPotionActive(MobEffects.NIGHT_VISION))
                    {
                        float nightVisionBrightness = this.getNightVisionBrightness(this.mc.player, partialTicks);
                        float inverseRed = 1.0F / red;

                        if (inverseRed > 1.0F / green)
                        {
                            inverseRed = 1.0F / green;
                        }

                        if (inverseRed > 1.0F / blue)
                        {
                            inverseRed = 1.0F / blue;
                        }

                        red = red * (1.0F - nightVisionBrightness) + red * inverseRed * nightVisionBrightness;
                        green = green * (1.0F - nightVisionBrightness) + green * inverseRed * nightVisionBrightness;
                        blue = blue * (1.0F - nightVisionBrightness) + blue * inverseRed * nightVisionBrightness;
                    }

                    if (red > 1.0F)
                    {
                        red = 1.0F;
                    }

                    if (green > 1.0F)
                    {
                        green = 1.0F;
                    }

                    if (blue > 1.0F)
                    {
                        blue = 1.0F;
                    }

                    float gamma = this.mc.gameSettings.gammaSetting;
                    float flippedRed = 1.0F - red;
                    float flippedGreen = 1.0F - green;
                    float flippedBlue = 1.0F - blue;
                    flippedRed = 1.0F - flippedRed * flippedRed * flippedRed * flippedRed;
                    flippedGreen = 1.0F - flippedGreen * flippedGreen * flippedGreen * flippedGreen;
                    flippedBlue = 1.0F - flippedBlue * flippedBlue * flippedBlue * flippedBlue;
                    red = red * (1.0F - gamma) + flippedRed * gamma;
                    green = green * (1.0F - gamma) + flippedGreen * gamma;
                    blue = blue * (1.0F - gamma) + flippedBlue * gamma;
                    red = red * 0.96F + 0.03F;
                    green = green * 0.96F + 0.03F;
                    blue = blue * 0.96F + 0.03F;

                    if (red > 1.0F)
                    {
                        red = 1.0F;
                    }

                    if (green > 1.0F)
                    {
                        green = 1.0F;
                    }

                    if (blue > 1.0F)
                    {
                        blue = 1.0F;
                    }

                    if (red < 0.0F)
                    {
                        red = 0.0F;
                    }

                    if (green < 0.0F)
                    {
                        green = 0.0F;
                    }

                    if (blue < 0.0F)
                    {
                        blue = 0.0F;
                    }

                    int j = 255;
                    int k = (int)(red * 255.0F);
                    int l = (int)(green * 255.0F);
                    int i1 = (int)(blue * 255.0F);
                    this.lightmapColors[i] = -16777216 | k << 16 | l << 8 | i1;
                }

                //this.lightmapTexture.updateDynamicTexture();
                this.lightmapUpdateNeeded = false;
                this.mc.mcProfiler.endSection();
            }
        }
    }

    private float getNightVisionBrightness(EntityPlayerSP player, float partialTicks)
    {
        // TODO Auto-generated method stub
        return 0;
    }
}
