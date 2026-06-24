# Dungeon Map Generator

A Java Swing dungeon map generator with theme support, PNG/text export, size presets, custom sizes, and animated reveal.

## Run

```bash
javac *.java
java Main
```

## Current generation style

The generator now uses a **packed BSP-style room layout** instead of the older loose random-room placement. This is intended to look closer to tabletop dungeon maps:

- rooms fill more of the map
- hallways are 1 tile wide
- rooms connect from edge to edge instead of center to center
- extra short connections are added so the layout feels less linear
- long empty hallways are reduced

## Map size controls

Use the `Map Size` dropdown:

- `24 x 36`
- `36 x 48`
- `14 x 28`
- `Custom`

When `Custom` is selected, the `Custom Width` and `Custom Height` spinners become editable. Change those values, then press `Generate Dungeon`.

Changing settings does not automatically regenerate the dungeon. Press:

- `Generate Dungeon` for a new seed using the current settings
- `Regenerate From Seed` to reuse the current seed
- `Animate Generation` to create a new seed and reveal the map visually
