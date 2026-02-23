
/**
 * Message from me on discord (general explanation)
 * <br>
 * as of right now this is what is happening:
 * <br>
 * - We intercept PlayerRecipeBookClickEvent
 * - HandlerRecipeBookClick has to update the logic to fix the issue, made another class cuz it might get bigger, which it did
 * - Many classes seem to use such behaviour, but we only care about crafting tables so we need to only process AbstractCraftingMenu, but by that point we did 99% of the logic anyway
 * - handlePylonItemPlacement method should handle how placement takes place, we have a method but normallly it is handled by a method in AbstractCraftingMenu, so we need to use reflection to call some stuff around to simulate normal behaviour and just change what we need
 * - PylonServerPlaceRecipe.placeRecipe, which is based on a delegate of ServerPlaceRecipe (the original one that was used in the original method), however most of ServerPlaceRecipe is closed, private methods, that can't be changed and aren't flexible so we need to make our own version (PylonServerPlaceRecipe).
 * - we finally replace moveItemToGrid, which in turn require changes to other method until findSlotMatchingCraftingIngredient, so we can exclude from ItemOrExact.Item matches our pylon items, this is because minecraft matches items in 2 ways: ItemOrExact.Item (so material based matching), ItemOrExact.Exact (pdc and component, perfect matching).
 * - stuff doesn't get moved to grid as expected, but it doesn't fail either, so we need to send a ghost recipe in said cases, this is handled by StackedItemContents#canCraft
 * - StackedItemContents basically accounts for every item in the inventory in his own way, so I make a bunch of reflection in order to add a makeshift method accountPylonItems which makes pylon item stacks be process as ItemOrExact.Exact ALWAYS
 *
 * @author Vaan1310 (on the run after writing this piece of code)
 */
package io.github.pylonmc.rebar.nms.recipe;