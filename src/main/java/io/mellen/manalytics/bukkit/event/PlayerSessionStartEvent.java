package io.mellen.manalytics.bukkit.event;

import io.mellen.manalytics.data.Player;
import io.mellen.manalytics.data.PlayerEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerSessionStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    private Player dataPlayer;

    public PlayerSessionStartEvent(Player dataPlayer) {
        super(true);
        this.dataPlayer = dataPlayer;
    }

    public Player getDataPlayer() {
        return dataPlayer;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}