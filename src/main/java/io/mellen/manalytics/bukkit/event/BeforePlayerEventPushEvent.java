package io.mellen.manalytics.bukkit.event;

import io.mellen.manalytics.data.Player;
import io.mellen.manalytics.data.PlayerEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BeforePlayerEventPushEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    private PlayerEvent playerEvent;

    public BeforePlayerEventPushEvent(PlayerEvent playerEvent) {
        super(true);
        this.playerEvent = playerEvent;
    }

    public PlayerEvent getPlayerEvent() {
        return playerEvent;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
