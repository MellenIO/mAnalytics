package io.mellen.manalytics;

import io.mellen.manalytics.command.AnalyticsCommand;
import io.mellen.manalytics.data.analytics.AnalyticsEngine;
import io.mellen.manalytics.data.connection.MysqlConnection;
import io.mellen.manalytics.data.events.EventRenderer;
import io.mellen.manalytics.listeners.EntityDeathListener;
import io.mellen.manalytics.listeners.PlayerStatusListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AnalyticsPlugin extends JavaPlugin {

    private static AnalyticsPlugin INSTANCE;

    private MysqlConnection connection;

    private AnalyticsEngine engine;

    @Override
    public void onEnable() {
        INSTANCE = this;

        saveDefaultConfig();

        getLogger().info("Initialising MySQL connection");
        connection = new MysqlConnection(this, getConfig().getString("mysql.host"), getConfig().getString("mysql.port"), getConfig().getString("mysql.username"), getConfig().getString("mysql.password"), getConfig().getString("mysql.database"));
        getLogger().info("Loading entities");
        connection.loadEntityTypes();

        getLogger().info("Loading analytics engine");
        engine = new AnalyticsEngine(this, connection);

        getLogger().info("Loading event renderers");
        EventRenderer.load(getConfig().getConfigurationSection("events"));
        getLogger().info("Loaded event renderers");

        //Load Minecraft events
        getLogger().info("Loading Minecraft event listeners");
        getCommand("analytics").setExecutor(new AnalyticsCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerStatusListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);

        getLogger().info("Analytics initialisation complete!");
    }

    @Override
    public void onDisable() {
        connection.close();
    }

    public AnalyticsEngine getAnalyticsEngine() {
        return engine;
    }

    public void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this, runnable);
    }

    public void debug(String message) {
        connection.debug(message);
    }

    public static AnalyticsPlugin getInstance() {
        return INSTANCE;
    }
}
