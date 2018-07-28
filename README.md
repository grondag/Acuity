# Introduction

**Render Hooks** is the common utility library for Grondag's Minecraft Mods

# What modders can't change
* Lighting functions are fixed
* Texture formats are fixed
* Diffuse texture mapping is always via texture atlas
* Can be secondary texture atlas for normals, specular
* Would need to alter atlas construction to minimize area of textures that have secondary textures

# What modders can change
* Diffuse color can be modified or replaced
* Decide if/when to apply lighting function
* Modify results of lighting function
* Ultimately outputs a color per pixel - can ignore standard lighting if you want, but don't try to implement a diff. lighting model
  
# Mod Packs and Support
This mod is in active development and is not feature-complete nor stable.  You MAY use this mod in ModPacks but be aware that future releases may require a world reset. It is therefore not recommended for servers or worlds you intend to keep for a long time.

This mod is [licensed under the MIT license](https://github.com/grondag/Exotic-Matter/blob/master/LICENSE). This means that no warranty is provided.

However, useful bug reports are always welcome.  Please use the [issue tracker](https://github.com/grondag/Exotic-Matter/issues) for all bug reports. 

# Contributing
This mod is a lot of work, and I will happily consider serious offers of collaboration.  Best way to start would be to post a feature request on the issue tracker to start a discussion and then create a pull request to implement an agreed-on feature. All contriburs must agree to license all submitted content under the license terms of this mod.






