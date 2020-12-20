package io.mellen.manalytics.command.subcommand;

import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.command.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class PushCommand extends Subcommand {
    public PushCommand(AnalyticsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean consoleOnly() {
        return false;
    }

    @Override
    public String getPermission() {
        return "manalytics.event.push";
    }

    @Override
    public boolean execute(CommandSender sender, String... parameters) {
        if (parameters.length >= 2) {
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(parameters[0]);
            String eventName = parameters[1];
            String customOne = null, customTwo = null, customThree = null, customFour = null;
            if (parameters.length > 2) {
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < parameters.length; i++) {
                    sb.append(parameters[i]).append(" ");
                }
                String customString = sb.toString().trim();
                String[] parts = customString.split(",");
                customOne = (parts.length > 0) ? parts[0] : null;
                customTwo = (parts.length > 1) ? parts[1] : null;
                customThree = (parts.length > 2) ? parts[2] : null;
                customFour = (parts.length > 3) ? parts[3] : null;
            }

            String finalCustomOne = customOne;
            String finalCustomTwo = customTwo;
            String finalCustomThree = customThree;
            String finalCustomFour = customFour;
            plugin.async(() -> {
                plugin.getAnalyticsEngine().pushEvent(plugin.getAnalyticsEngine().getPlayer(targetPlayer), eventName, finalCustomOne, finalCustomTwo, finalCustomThree, finalCustomFour);
            });
            message(sender, "&aEvent pushed successfully!");
            return true;
        }
        message(sender, "&cUsage: /analytics push <playerName> <eventName> ([customOne],[customTwo],[customThree],[customFour])");
        return false;
    }
}
