package io.mellen.manalytics.data.events;

import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.data.PlayerEvent;
import io.mellen.manalytics.util.DateUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class EventRenderer {

    private static Map<String, RendererData> rendererDataMap = new HashMap<>();

    public static void load(ConfigurationSection configRoot) {
        rendererDataMap = new HashMap<>();
        loadInner(configRoot, "");
    }

    private static void loadInner(ConfigurationSection config, String prefix) {
        for (String key : config.getKeys(false)) {
            String eventName = prefix + "." + key;
            if (eventName.charAt(0) == '.') {
                eventName = eventName.substring(1);
            }

            if (config.contains(key + ".text")) { //We have an event node
                AnalyticsPlugin.getInstance().debug("Loading event renderer for " + eventName);
                rendererDataMap.put(eventName,
                        new RendererData(
                                config.getString(key + ".text", ""),
                                config.getString(key + ".html", ""),
                                config.getString(key + ".custom.one", ""),
                                config.getString(key + ".custom.two", ""),
                                config.getString(key + ".custom.three", ""),
                                config.getString(key + ".custom.four", "")
                        )
                );
            }
            else {
                ConfigurationSection newSection = config.getConfigurationSection(key);
                if (null != newSection) {
                    loadInner(newSection, eventName);
                }
            }
        }
    }

    public static RendererData forEvent(String eventName) {
        return rendererDataMap.getOrDefault(eventName, rendererDataMap.get("default"));
    }

    public static class RendererData {
        private final String textFormat;
        private final String htmlFormat;
        private final String customOneLabel;
        private final String customTwoLabel;
        private final String customThreeLabel;
        private final String customFourLabel;

        public RendererData(String textFormat, String htmlFormat) {
            this.textFormat = textFormat;
            this.htmlFormat = htmlFormat;
            this.customOneLabel = null;
            this.customTwoLabel = null;
            this.customThreeLabel = null;
            this.customFourLabel = null;
        }

        public RendererData(String textFormat, String htmlFormat, String customOneLabel, String customTwoLabel, String customThreeLabel, String customFourLabel) {

            this.textFormat = textFormat;
            this.htmlFormat = htmlFormat;
            this.customOneLabel = customOneLabel;
            this.customTwoLabel = customTwoLabel;
            this.customThreeLabel = customThreeLabel;
            this.customFourLabel = customFourLabel;
        }

        public String renderText(PlayerEvent event) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%%time_pretty%%", DateUtil.relativeFormat(event.getCreatedAt()));
            replacements.put("%%time_long%%", DateUtil.utcFormat(event.getCreatedAt()));
            replacements.put("%%player_name%%", event.getPlayer().getName());
            replacements.put("%%player_uuid%%", event.getPlayer().getUuid().toString());
            replacements.put("%%custom_one%%", event.getCustomOne());
            replacements.put("%%custom_two%%", event.getCustomTwo());
            replacements.put("%%custom_three%%", event.getCustomThree());
            replacements.put("%%custom_four%%", event.getCustomFour());

            String result = textFormat;
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                result = result.replaceAll(replacement.getKey(), replacement.getValue());
            }
            return result;
        }

        public String getCustomFourLabel() {
            return customFourLabel;
        }

        public String getCustomThreeLabel() {
            return customThreeLabel;
        }

        public String getCustomTwoLabel() {
            return customTwoLabel;
        }

        public String getCustomOneLabel() {
            return customOneLabel;
        }

        public String getHtmlFormat() {
            return htmlFormat;
        }

        public String getTextFormat() {
            return textFormat;
        }
    }
}
