### Nutrition Rework
- The nutrition of many food items have been changed for consistency and balance purposes.
  - All default TFC foods now only use 1 decimal precision (i.e. 0.1 instead of 0.15)
  - All foods will give 4 hunger points instead of some giving 2.
  - Pumpkins are now nutritionally vegetables.
  - Many other changes.
- Sandwiches
  - Now only output 1 sandwich instead of 2
  - No longer give more nutrients out than you put in (0.75 multiplier on average, same as salads)
  - Can be crafted with two loaves of bread and 1-3 fillings (formerly 1 filling only)
  - Overall, sandwiches are slightly more nutrient dense than before, but twice as expensive
- Soups
  - No longer produce far more nutrients than put in
  - Output same numbers of soup as before, but total nutrients out will only be 20% higher than what was put in, instead of up to 255%
  - Intended to be useful for stretching food supplies, but bad for getting to full nutrients
- Added bison meat, and cooked versions of cassava and lentils.

### Changes
- Food items with similar creation dates are now stackable. Relatedly, fixed a bug where the food creation date rounding window was wrong, resulting in unexpected behavior.
- Readded the weld button.
- Reworked the Anvil GUI.
  - Modernized the texture of the background and buttons.
  - Changed the position of some elements.
  - The target indicator will now grow in size if the "acceptable work range" config option is used.
- Reworked sewing recipes to support blank slots like they did in 1.20. The required keys in the pattern are now different.
- Since 1.21 switched to 24-minute days, a lot of recipe and config timings that previously were round numbers (i.e. 8 hr.) are now not round numbers (i.e. 6:39). In light of this, we have rebalanced the times that many recipes, config options, etc. take to reflect this and have made the associated changes in the Field Guide. The result of this is that the timings of many gameplay actions, like pit kilns, barrel recipes, etc. have changed slightly.
- Improved compatibility with Sodium.
- Removed support for Embeddium and Rubidium.
- Expanded field guide information related to climate.
- Sea Ice can now cool items like regular ice.
- Made significant improvements to how volcanic features blend with the world (removing the chunk border on the sides of some volcanoes)
- Updated the Russian, Polish, and Ukrainian localizations.

### Fixes
- Added a config option to allow bubble columns to provide air. (#3157)
- Fixed incorrect or missing config translations.
- Fixed the item size of crucibles and tools.
- Fixed the mud brick recipe and added mud brick tags.
- Fixed a major incompatibility with Distant Horizons.
- Fixed the day counter starting on the wrong day. (#3414)
- Fixed trapped chest textures z-fighting.
- Added tags for cut gems (#3415)
- Fix some bleaching recipes not working.
- Fix many broken/missing barrel recipe translation keys.
- Fixed barrels sometimes not being able to make mud.
- Fixed boiling pots allowing fluid extraction (#3371)
- Fix milking cows with full buckets adding wear and tear (#3045)
- Fixed formatting mistakes in the Japanese field guide translation.
- Fixed being able to light fires underwater in some circumstances.