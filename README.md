# Introduction

**Acuity** is a core mod modders can use to implement multi-layered textures, emissive rendering and fancy visual effects (shaders) for block rendering with good performance on moderate to high-end hardware, all without using TileEntitySpecialRenderer.  Future versions may also offer performance and aesthetic benefits for players.

This mod is client-side only and can be quickly enabled and disabled at run time via configuration settings. When the mod is disabled, the game uses normal Minecraft rendering.

# Why is this mod needed when Optifine and shader packs already exist?
Those solutions are oriented towards players and don't offer much control or extensibility for modders and pack makers.  Optifine, while technically amazing, is not open-source and not targeted at Forge modders. It's also not available on the Twitch launcher where most mods and packs are nowadays distributed.  Similarly, shaders demand high-end hardware, can be difficult for players to configure or to distribute with a pack, and - most importantly - don't provide a consistent framework for modders to control the visual appearance of their blocks in a way that will be consistent with the shader pack.

# Current Features
* Render blocks with multiple texture layers (for example, base color + decorative texture + border) in a single pass without transparency. (It's possible to do this in vanilla, but requires three quads and possibly one or more transparency sorts unless you can get by with cutout textures.)
* Implement custom shaders for your models that will be automatically activated *per-quad*.
* *Per-quad* emissive rendering, can also control per-texture layer within the same quad.

# Permanent Limitations & Constraints
* Don'create more shaders than you need - while the mod tries to be efficient, more pipelines mean more, smaller draw calls to the GPU, limiting performance
* Modders have no control over vertex formats. The mod will select a vertex format for you based on the number of texture layers you need (1 to 3). This is necessary to support enhanced lighting models and limit the number of GL state changes. (But you could put whatever data you want into the extra color/UV attributes of the multi-layer formats...)
* Modders should rely on the provided lighting functions so that future enhanced lighting models "just work" with your block - unless your block really is meant to look different and thus doesn't need standard lighting.
* Avoid excessive variation when rendering transparency. Ideally, use the single-layer vertex format and the default pipeline or only one or two custom pipelines. You *can* have multiple vertex formats and pipelines in the transparency layer, and the mod will automatically sort quads across formats and pipelines and then interleave draw calls based on the correct ordering. But it will mean more GL state changes and could thus impact peformance. (It should still be more efficient than TESRs because it can still render from a static VBO.)

# Current Limitations 
* No enhanced lighting model is available - this will wait for MC 1.13 and LWJGL 3 support.
* No support for animated blocks. (TESR still works.) Will probably be necessary in future for good results with enhanced lighting.
* No support for particles or entities. Will probably be necessary in future for good results with enhanced lighting.
  
# Mod Packs and Support
This mod is in active development and is not feature-complete nor stable. The first stable, non-alpha release will be for 1.13. There will NEVER be a non-alpha build of this mod for 1.12 and support for 1.12 will cease as soon as 1.13 Forge becomes available. By the time this mod is stable, 1.13 should be a viable option for pack makers. 

That all said, you MAY use this mod in ModPacks if you are willing to accept the current instability and lack of future support for 1.12.

This mod is [licensed under the MIT license](https://github.com/grondag/Acuity/blob/master/LICENSE). This means that no warranty is provided.

However, useful bug reports are always welcome.  Please use the [issue tracker](https://github.com/grondag/Acuity/issues) for all bug reports. 

# Contributing
This mod is a lot of work, and I will happily consider serious offers of collaboration.  Best way to start would be to post a feature request on the issue tracker to start a discussion and then create a pull request to implement an agreed-on feature. All contriburs must agree to license all submitted content under the license terms of this mod.






