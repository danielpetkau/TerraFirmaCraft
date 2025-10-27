package net.dries007.tfc.util.calendar;

//Im not sure where to leave this but ig it interacts with the calendar system in a roundabout way.
//This interface is to be implemented in any block entity with a timer based recipe, to interact with the clocks timer mode.
public interface IRecipeTimer {
    int getRecipeDuration();

    long getRemainingTime();
}
