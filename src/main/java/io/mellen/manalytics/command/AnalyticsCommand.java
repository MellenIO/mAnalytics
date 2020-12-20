package io.mellen.manalytics.command;

import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.command.subcommand.PushCommand;
import io.mellen.manalytics.command.subcommand.ViewCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsCommand implements CommandExecutor {
    private Map<String, Subcommand> subcommands = new HashMap<>();

    private AnalyticsPlugin plugin;

    public AnalyticsCommand(AnalyticsPlugin plugin) {
        this.plugin = plugin;

        subcommands = new HashMap<>();
        subcommands.put("push", new PushCommand(plugin));
        subcommands.put("view", new ViewCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length > 0) {
            if (subcommands.containsKey(strings[0].toLowerCase())) {
                Subcommand subcommand = subcommands.get(strings[0].toLowerCase());
                if (subcommand.consoleOnly()) {
                    if (commandSender instanceof ConsoleCommandSender) {
                        return subcommand.execute(commandSender, Arrays.copyOfRange(strings, 1, strings.length));
                    }
                    else {
                        commandSender.sendMessage("This command must be executed as console only.");
                    }
                }
                else {
                    if (commandSender instanceof Player) {
                        if (((Player)commandSender).hasPermission(subcommand.getPermission())) {
                            return subcommand.execute(commandSender, Arrays.copyOfRange(strings, 1, strings.length));
                        }
                        else {
                            commandSender.sendMessage("You do not have permission to run this command");
                        }
                    }
                }
            }
        }
        return false;
    }
}
