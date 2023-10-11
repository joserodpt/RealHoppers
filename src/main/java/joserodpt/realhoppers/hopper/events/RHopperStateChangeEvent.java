package joserodpt.realhoppers.hopper.events;

import joserodpt.realhoppers.hopper.RHopper;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RHopperStateChangeEvent extends Event implements Cancellable {
    private final RHopper hopper;
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean isCancelled;

    public RHopperStateChangeEvent(RHopper rh){
        this.hopper = rh;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    public RHopper getHopper() {
        return this.hopper;
    }

}