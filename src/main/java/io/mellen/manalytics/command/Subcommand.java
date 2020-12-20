package io.mellen.manalytics.command;

import io.mellen.manalytics.AnalyticsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public abstract class Subcommand {

    protected AnalyticsPlugin plugin;

    public Subcommand(AnalyticsPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract boolean consoleOnly();
    public abstract String getPermission();
    public abstract boolean execute(CommandSender sender, String... parameters);

    protected void message(CommandSender target, String message) {
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
