package io.mellen.manalytics.listeners;

import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.util.ItemUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;

public class EntityDeathListener implements Listener {

    private AnalyticsPlugin plugin;

    private List<String> entityConfig = new ArrayList<>();

    private boolean entityKillEnabled;
    private boolean playerKillEnabled;
    private boolean playerDeathEnabled;

    public EntityDeathListener(AnalyticsPlugin plugin) {
        this.plugin = plugin;

        entityKillEnabled = plugin.getConfig().getBoolean("settings.player.kill.entity.enabled", false);
        playerKillEnabled = plugin.getConfig().getBoolean("settings.player.kill.player.enabled", false);
        playerDeathEnabled = plugin.getConfig().getBoolean("settings.player.death.enabled", false);

        entityConfig = plugin.getConfig().getStringList("settings.player.kill.entity.types");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!entityKillEnabled) return;

        EntityType entityType = event.getEntityType();
        Player killer = event.getEntity().getKiller();
        if (!entityType.equals(EntityType.PLAYER) && killer != null) {
            String entityTypeName = entityType.name();
            if (entityConfig.contains(entityTypeName)) {
                plugin.async(() -> {
                    plugin.getAnalyticsEngine().pushEvent(
                            plugin.getAnalyticsEngine().getPlayer(killer),
                            "player.kill.entity",
                            event.getEntity().getName(),
                            entityType.name(),
                            ItemUtil.getItemName(killer.getItemInHand()),
                            killer.getLocation().toString()
                    );
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player deadPlayer = event.getEntity();
        Player killer = deadPlayer.getKiller();
        if (killer != null && playerKillEnabled) {
            //Push player.kill.player event, killer has killed deadPlayer
            plugin.async(() -> {
                plugin.getAnalyticsEngine().pushEvent(
                        plugin.getAnalyticsEngine().getPlayer(killer),
                        "player.kill.player",
                        deadPlayer.getName(),
                        deadPlayer.getUniqueId().toString(),
                        ItemUtil.getItemName(killer.getItemInHand()),
                        deadPlayer.getLastDamageCause().getCause().name()
                );
            });

        }

        if (playerDeathEnabled) {
            plugin.async(() -> {
                plugin.getAnalyticsEngine().pushEvent(
                        plugin.getAnalyticsEngine().getPlayer(deadPlayer),
                        "player.death",
                        deadPlayer.getLastDamageCause().getCause().name(),
                        deadPlayer.getLocation().getWorld().getName(),
                        deadPlayer.getLocation().toVector().toString(),
                        null //TODO add a 4th parameter?
                );
            });
        }


    }
}
