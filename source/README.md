# Dungeon Map Generator

A Java Swing dungeon map generator with configurable map settings, seed support, themes, animated reveal generation, save-to-text, and PNG export.

## Run

From this folder:

```bash
javac *.java
java Main
```

## Main Features

- Random room and hallway generation
- Room types: start, combat, treasure, trap, boss, secret, exit
- Special tiles: enemies, treasure, traps, boss, key, potion, locked door, secret door
- Seeded generation for repeatable maps
- Visual Swing editor/preview window
- Theme dropdown: Classic, Crypt, Cave, Ice, Lava
- Animated generation reveal button
- Zoom slider
- Save map as `.txt`
- Export map as `.png`

## Notes

The animated generation currently reveals the completed generated map tile-by-tile. A future improvement would be recording each generation step as rooms, hallways, doors, and entities are placed, then playing those steps back individually.
