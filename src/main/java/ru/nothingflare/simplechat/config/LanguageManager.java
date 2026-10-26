package ru.nothingflare.simplechat.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.nothingflare.simplechat.SimpleChatLogPlugin;
import ru.nothingflare.simplechat.util.ColorUtil;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    public static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "en", "ru", "zh", "de", "fr", "es", "pt", "ja", "uk", "it", "pl", "tr", "ko"
    );

    private final SimpleChatLogPlugin plugin;
    private FileConfiguration langConfig;
    private String currentLanguage = "ru";
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(SimpleChatLogPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveDefaultLanguages();

        String lang = plugin.getConfig().getString("language", "ru").toLowerCase();
        if (!SUPPORTED_LANGUAGES.contains(lang)) {
            plugin.getLogger().warning("Unsupported language '" + lang + "' specified in config.yml! Defaulting to 'en'.");
            lang = "en";
        }
        this.currentLanguage = lang;

        File langFile = new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + langFile.getName() + " not found, defaulting to messages_en.yml.");
            langFile = new File(plugin.getDataFolder(), "lang/messages_en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defaultStream = plugin.getResource("lang/messages_" + lang + ".yml");
        if (defaultStream == null) {
            defaultStream = plugin.getResource("lang/messages_en.yml");
        }
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultConfig);
        }

        messageCache.clear();
        String rawPrefix = langConfig.getString("prefix", "&8[&6SimpleChatLog&8] &r");

        if (langConfig.getDefaults() != null) {
            for (String key : langConfig.getDefaults().getKeys(true)) {
                if (langConfig.getDefaults().isString(key)) {
                    String val = langConfig.getDefaults().getString(key, "");
                    if (val != null) {
                        val = val.replace("{prefix}", rawPrefix);
                        messageCache.put(key, ColorUtil.color(val));
                    }
                }
            }
        }

        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                String val = langConfig.getString(key, "");
                if (val != null) {
                    val = val.replace("{prefix}", rawPrefix);
                    messageCache.put(key, ColorUtil.color(val));
                }
            }
        }

        plugin.getLogger().info("Loaded language: " + currentLanguage + " (" + langFile.getName() + ")");
    }

    private void saveDefaultLanguages() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        for (String lang : SUPPORTED_LANGUAGES) {
            String fileName = "lang/messages_" + lang + ".yml";
            File targetFile = new File(plugin.getDataFolder(), fileName);
            if (!targetFile.exists()) {
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                }
            }
        }
    }

    public String getRawMessage(String key) {
        if (langConfig == null) {
            return key;
        }
        return langConfig.getString(key, key);
    }

    public String getMessage(String key) {
        return getMessage(key, null);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messageCache.get(key);
        if (message == null) {
            message = getRawMessage(key);
            String prefix = getRawMessage("prefix");
            message = message.replace("{prefix}", prefix);
            message = ColorUtil.color(message);
        }

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }

        return message;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
