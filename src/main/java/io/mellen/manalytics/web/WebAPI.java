package io.mellen.manalytics.web;

import com.google.gson.Gson;
import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.data.Player;
import io.mellen.manalytics.web.response.PlayerResponse;
import spark.Spark;

import java.util.UUID;


public class WebAPI {
    private AnalyticsPlugin plugin;

    private String username, password;
    private int port;

    public WebAPI(AnalyticsPlugin plugin, String authUser, String authPassword, int port) {
        this.plugin = plugin;
        this.username = authUser;
        this.password = authPassword;
        this.port = port;
    }

    public void startListening() {
        Spark.port(port);
        Spark.threadPool(8, 2, 30000);
        Spark.get("/player/:idType/:id", (request, response) -> {
            PlayerResponse responseData = new PlayerResponse(request, plugin);
            response.type("application/json");

            return new Gson().toJson(responseData);
        });
    }

    public void stopListening() {
        Spark.stop();
    }
}
