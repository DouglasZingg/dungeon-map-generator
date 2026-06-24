# Dungeon Map Generator

A Java Swing single-floor dungeon map generator with packed rooms, special room shapes, main/side path generation, dead ends, themes, animated generation, and PNG export.

## Run

```bash
javac *.java
java Main
```

## Current Features

- Single-floor packed dungeon generation
- Preset map sizes: 14x28, 24x36, and 36x48
- Main path, side paths, and dead-end room controls
- Rectangle, L-shaped, T-shaped, and cross-shaped rooms
- Theme dropdown
- Color-only map rendering
- Color-only legend above the map preview
- PNG export
- Generate Dungeon creates a fresh seed
- Regenerate From Seed rebuilds the current seed
- Window opens maximized

## Cleanup Notes

- Removed multi-floor/stair generation because it was unstable.
- Removed custom map size controls.
- Removed stair tile types and floor navigation UI.
- Updated the title/subtitle to describe the single-floor version.
