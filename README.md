# Burmalda Launcher

Windows launcher starter project for Burmalda Client.

## Build
The GitHub Actions workflow builds a self-contained Windows x64 executable.
Open this repository on GitHub, then select **Actions** → **Build Windows EXE**.
Download the `BurmaldaLauncher-win-x64` artifact after the workflow finishes.

## Current functionality
- Liquid Glass-inspired dark UI with RGB accent animation.
- Detects common `.minecraft` locations.
- Lets the user choose a Minecraft game directory.
- Installs a locally selected Burmalda Fabric mod JAR into that directory's `mods` folder.

This starter does not yet launch Minecraft or download updates from a release server.
Those features require choosing the supported Minecraft launcher and publishing a real
versioned JAR + manifest first.
