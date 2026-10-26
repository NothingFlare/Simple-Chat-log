package ru.nothingflare.simplechat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.nothingflare.simplechat.command.ChatLogCommand;
import ru.nothingflare.simplechat.config.LanguageManager;
import ru.nothingflare.simplechat.database.DatabaseManager;
import ru.nothingflare.simplechat.listener.BukkitChatListener;
import ru.nothingflare.simplechat.listener.PaperChatListener;
import ru.nothingflare.simplechat.queue.ChatLogQueue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleChatLogPlugin extends JavaPlugin {

    private static SimpleChatLogPlugin instance;

    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private ChatLogQueue queue;

    private boolean logCancelled;
    private boolean hideChatFromConsole;

    private final Set<CommandSender> liveListeners = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadSettings();

        this.languageManager = new LanguageManager(this);
        this.languageManager.load();

        this.databaseManager = new DatabaseManager(this);
        if (!this.databaseManager.init()) {
            getLogger().severe("Не удалось подключиться к базе данных! Плагин отключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.queue = new ChatLogQueue(this);
        this.queue.start();

        registerChatListener();
        registerCommands();

        printBanner(false);
    }

    @Override
    public void onDisable() {
        liveListeners.clear();

        if (this.queue != null) {
            this.queue.stop();
        }

        if (this.databaseManager != null) {
            this.databaseManager.close();
        }

        getLogger().info("SimpleChatLog успешно отключен!");
    }

    private void loadSettings() {
        this.logCancelled = getConfig().getBoolean("settings.log-cancelled", false);
        this.hideChatFromConsole = getConfig().getBoolean("settings.hide-chat-from-console", true);
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadSettings();

        if (this.languageManager != null) {
            this.languageManager.load();
        }

        if (this.queue != null) {
            this.queue.start();
        }

        printBanner(true);
    }

    public void printBanner(boolean isReload) {
        getLogger().info("");
        getLogger().info("   ____ _                 _        ____  _            _      _                 ");
        getLogger().info("  / ___(_)_ __ ___  _ __ | | ___  / ___|| |__   __ _ | |_   | |    ___    __ _ ");
        getLogger().info("  \\___ \\| | '_ ` _ \\| '_ \\| |/ _ \\| |   | '_ \\ / _` || __|  | |    / _ \\  / _` |");
        getLogger().info("   ___) | | | | | | | |_) | |  __/| |___| | | |(_| | | |_   | |___ (_) || (_| |");
        getLogger().info("  |____/|_|_| |_| |_| .__/|_|\\___| \\____|_| |_|\\__,_| \\__|  |_____|\\___/ \\__, |");
        getLogger().info("                    |_|                                                   |___/ ");
        getLogger().info("");
        getLogger().info("Привет, мир!");
        getLogger().info("Наш Telegram канал!");
        getLogger().info("t.me/lighthouse_top");
        getLogger().info("Версия Simple ChatLog: " + getDescription().getVersion());
        getLogger().info("Язык: " + (languageManager != null ? languageManager.getCurrentLanguage().toUpperCase() : "RU"));
        String platform = isFolia() ? "Folia" : Bukkit.getName();
        getLogger().info("Simple ChatLog запущен на " + platform);
        if (isReload) {
            getLogger().info("Simple ChatLog успешно перезагружен!");
        } else {
            getLogger().info("Simple ChatLog успешно включен!");
        }
        getLogger().info("");
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean toggleLiveListener(CommandSender sender) {
        if (liveListeners.contains(sender)) {
            liveListeners.remove(sender);
            return false;
        } else {
            liveListeners.add(sender);
            return true;
        }
    }

    public void removeLiveListener(CommandSender sender) {
        liveListeners.remove(sender);
    }

    public void broadcastLiveMessage(Player player, String message) {
        if (liveListeners.isEmpty() || player == null || message == null) {
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("message", message);
        placeholders.put("world", player.getWorld() != null ? player.getWorld().getName() : "unknown");
        placeholders.put("x", String.format(Locale.ROOT, "%.0f", player.getLocation().getX()));
        placeholders.put("y", String.format(Locale.ROOT, "%.0f", player.getLocation().getY()));
        placeholders.put("z", String.format(Locale.ROOT, "%.0f", player.getLocation().getZ()));

        String formatted = languageManager.getMessage("live_format", placeholders);

        for (CommandSender listener : liveListeners) {
            listener.sendMessage(formatted);
        }
    }

    private void registerChatListener() {
        boolean paperSupported = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            paperSupported = true;
        } catch (ClassNotFoundException ignored) {
        }

        String preferredMode = getConfig().getString("settings.chat-event", "auto").toLowerCase();

        if ("spigot".equals(preferredMode)) {
            getServer().getPluginManager().registerEvents(new BukkitChatListener(this), this);
            getLogger().info("Слушатель чата: Spigot AsyncPlayerChatEvent.");
        } else if ("paper".equals(preferredMode) && paperSupported) {
            getServer().getPluginManager().registerEvents(new PaperChatListener(this), this);
            getLogger().info("Слушатель чата: Modern Paper AsyncChatEvent.");
        } else {
            // Режим 'auto': универсальный режим для 100% совместимости с любыми чат-плагинами (OCDChat, Chatty, EssentialsX, LPC и др.)
            getServer().getPluginManager().registerEvents(new BukkitChatListener(this), this);
            if (paperSupported) {
                getServer().getPluginManager().registerEvents(new PaperChatListener(this), this);
                getLogger().info("Слушатель чата: Универсальный гибридный режим (Paper + Spigot с дедупликацией).");
            } else {
                getLogger().info("Слушатель чата: Spigot AsyncPlayerChatEvent.");
            }
        }
    }

    private void registerCommands() {
        ChatLogCommand command = new ChatLogCommand(this);
        PluginCommand pluginCommand = getCommand("chatlog");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    public static SimpleChatLogPlugin getInstance() {
        return instance;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ChatLogQueue getQueue() {
        return queue;
    }

    public boolean isLogCancelled() {
        return logCancelled;
    }

    public boolean isHideChatFromConsole() {
        return hideChatFromConsole;
    }
}
