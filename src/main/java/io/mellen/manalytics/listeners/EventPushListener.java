package io.mellen.manalytics.listeners;

import io.mellen.manalytics.bukkit.event.BeforePlayerEventPushEvent;
import io.mellen.manalytics.data.PlayerEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EventPushListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void beforeEventPush(BeforePlayerEventPushEvent event) {
        PlayerEvent evt = event.getPlayerEvent();
        evt.setData("server_name", "The Far Lands");
    }
}
