package io.mellen.manalytics.command.subcommand;

import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.command.Subcommand;
import io.mellen.manalytics.data.PlayerEvent;
import io.mellen.manalytics.data.events.EventRenderer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ViewCommand extends Subcommand {
    public ViewCommand(AnalyticsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean consoleOnly() {
        return false;
    }

    @Override
    public String getPermission() {
        return "manalytics.player.view";
    }

    @Override
    public boolean execute(CommandSender sender, String... parameters) {
        OfflinePlayer targetPlayer = Bukkit.getServer().getOfflinePlayer(parameters[0]);
        int page = (parameters.length > 1) ? Integer.valueOf(parameters[1]) : 1;

        List<PlayerEvent> events = plugin.getAnalyticsEngine().getEventsForPlayer(plugin.getAnalyticsEngine().getPlayer(targetPlayer), page, 8);
        message(sender, "&aEvent History for &6" + targetPlayer.getName());
        for (PlayerEvent event : events) {
            message(sender, EventRenderer.forEvent(event.getEventName()).renderText(event));
        }
        message(sender, "&a--------");

        return true;
    }
}
