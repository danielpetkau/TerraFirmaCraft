### Misc Changes
- Adds channels and molds from the Casting with Channels addon
- Fix pumpkins crashing when broken in creative
- Add heating recipes for metal blocks
- Fix patchouli book rendering under item tooltips or off screen
- Fix birds not being pluckable (closes #3141)
- Made liquid ink more efficient in scribing tables
- Made bison hitboxes more closely match the visual size of bison
- Fixed various issues with jams and sandwiches
### World Generation Changes
- Fix river canyons messing up at river cave entrances (closes #)
- Modify stair-step canyons to be less likely to have 100% vertical cliffs
- Tweak generation of various soil types to be more consistent
### Farming Changes
- Fixed crops dying after time skips due to inconsistent calculations of hydration (closes #3112 closes #3099)
- Removed accumulated rainfall/storm effects on hydration (10% max -> 0% max)
- Increased maximum hydration contribution (50% max @ 500mm ->60% max @ 600mm)
- All fruit trees and bushes now calculate hydration identically to crops, based on where the base of the tree/bush is. For grafted branches, the position used for the calculation is still the base of the overall tree.
- All fruit trees and bushes now use average annual rainfall and average annual temperature, both calculated at the lowest block of the plant.
- Average temperature checks are now elevation-sensitive (mainly affects fruit trees and berry blocks, though berry blocks already used elevation based temperatures)
- Updated fruit and berry climate ranges to ensure that plants will survive where they generate
- Added additional rainfall info to the climate screen (now shows current rainfall in addition to average and peak)
- Added explanation to new hydration system to the Hydration entry of the field guide
- Changed how crops calculate hydration over time skips to match how temperature is calculated (checks only start and end times)
- Fixed fruit tree saplings being used up when splicing to another sapling in creative mode