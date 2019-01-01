# Introduction

**Acuity** offers mod authors the ability to implement multi-layered textures, emissive rendering and fancy visual effects (shaders) for block rendering with good performance on moderate to high-end hardware without creating unecessary block entities.

The active version of Acuity is targeting the Fabric toolchain and API. The Forge 1.12 version is still available but was never distributed in a release version.  Given the changes coming in 1.14 is unlikely a Forge version will be available soon.

This mod is client-side only.

# Who is this mod for?
This API targeted at mod authors who want a powerful, flexible and consistent framework to control the visual appearance of game objects in a lighting-consistent way.  The API and implementation are open source and will be distributed on the Twitch launcher where most mods and packs are nowadays distributed.

The API may eventually also provide shader pack authors a way to implement shader packs in a performant way with good mod compatibility.

# Core Feature Set
* Simple to use, relative to what it does.  Declare your shaders and uniform callbacks (if needed), implement a method to pipe in your vertices and you're done.  The API handles the rest.
* Handlers for built-in Minecraft models and Fabric models when available.
* Multi-texture and cutout rending in a single quad and single draw call
* Cutout, mip map and lighting can be controlled per-quad, per-layer
* Custom shader and uniform support. Shaders can be designated by quad
* Default pipelines that will support most multi-layer rendering scenarious without custom shaders
* Shader library with basic lighting and texturing functions
* Performance optimizations as needed to ensure consistent good performance in complex modded scenes

# Acuity is not a modeling library
Acuity is a rendering API that can handle practically any rendering scenario, but it assumes you already have vertex data with fully baked texture coordinates. Mod authors with complex rendering needs will probably want to implement this directly. [Exotic Matter](https://github.com/grondag/Exotic-Matter) has advanced modelling features but is not yet ported to Fabric and is not meant to be a public API.  Other mod authors are welcome and encouraged to build model APIs or libraries on top of Acuity.   

# Current Status
* APIs for runtime, pipeline management and block rendering are draft complete.
* Block rendering implementation under active development - test build targeted for late Jan 2019.
* Item and BlockEntity rendering will be priorities after block rendering is working and stable
* Entity rendering is a low priority
* Enhanced lighting model is a low priority

# Limitations & Constraints
* Expect lighting bugs in early releases
* Mixin patches in early build may be brutal hacks. The 1.14 codebase isn't baked yet so I see no point in trying to be surgical.
* Similarly, more exotic performance optimizations will wait until 1.14 is released and we know the extent of rendering changes are clear.  
* Don't create more shaders than you need - while the mod tries to be efficient, more pipelines mean more, smaller draw calls to the GPU, limiting performance
* Vertex formats are fixed and determined by texture depth. This is necessary to limit the number of GL state changes. (But you can put whatever data you want into the extra color/UV attributes of the multi-layer formats...)
* Use the provided glsl library functions so that future enhanced lighting models "just work" with your block - unless your block really is meant to look different and thus doesn't need standard lighting.
* Avoid excessive variation when rendering transparency. Ideally, use the single-layer vertex format and the default pipeline or only one or two custom pipelines. You *can* have multiple vertex formats and pipelines in the transparency layer, and the mod will automatically sort quads across formats and pipelines and then interleave draw calls based on the correct ordering. But it will mean more GL state changes and could thus impact peformance.
* No support for particles currently planned. Could change if the need is there.

  
# Mod Packs and Support
This mod is in active development and is not feature-complete nor stable. That said, you MAY use this mod in ModPacks if you are willing to accept the current instability and lack of support.

This mod is [licensed under the MIT license](https://github.com/grondag/Acuity/blob/master/LICENSE). This means no warranty is provided.

Useful bug reports are always welcome.  Please use the [issue tracker](https://github.com/grondag/Acuity/issues) for all bug reports. 

# Including Acuity in Your Dev Environment
There is nothing to include yet, but maven information is as follows...

```gradle
repositories {
    maven {
    	  name = "grondag"
    	  url = "https://grondag-repo.appspot.com"
    }
}
```

# Contributing
This mod is a lot of work, and I will happily consider serious offers of collaboration.  Best way to start would be to post a feature request on the issue tracker to start a discussion and then create a pull request to implement an agreed-on feature. All contriburs must agree to license all submitted content under the license terms of this mod.
