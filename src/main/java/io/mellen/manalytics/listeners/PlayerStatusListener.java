package io.mellen.manalytics.listeners;

import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.data.Player;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerStatusListener implements Listener {

    private AnalyticsPlugin plugin;

    private boolean cleanPlayerOnLeave;

    public PlayerStatusListener(AnalyticsPlugin plugin) {
        this.plugin = plugin;

        cleanPlayerOnLeave = plugin.getConfig().getBoolean("settings.data.remove-player-object-on-leave", false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.async(() -> {
            Player dataPlayer = plugin.getAnalyticsEngine().getPlayer(event.getPlayer());
            dataPlayer.startSession(plugin.getAnalyticsEngine().getConnection());
            plugin.debug("Started player session for " + dataPlayer.getName());

            Location loc = event.getPlayer().getLocation();
            plugin.getAnalyticsEngine().pushEvent(dataPlayer, "player.join", loc.getWorld().getName(), "" + loc.getBlockX(), "" + loc.getBlockY(), "" + loc.getBlockZ());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerQuitEvent event) {
        plugin.async(() -> {
            Player dataPlayer = plugin.getAnalyticsEngine().getPlayer(event.getPlayer());
            dataPlayer.endSession(plugin.getAnalyticsEngine().getConnection());

            dataPlayer.setName(event.getPlayer().getName());
            dataPlayer.save(plugin.getAnalyticsEngine().getConnection());

            plugin.debug("Ended player session for " + dataPlayer.getName());

            Location loc = event.getPlayer().getLocation();
            plugin.getAnalyticsEngine().pushEvent(dataPlayer, "player.leave", loc.getWorld().getName(), "" + loc.getBlockX(), "" + loc.getBlockY(), "" + loc.getBlockZ());

            if (cleanPlayerOnLeave) {
                plugin.getAnalyticsEngine().removePlayer(event.getPlayer().getUniqueId());
            }
        });
    }
}
