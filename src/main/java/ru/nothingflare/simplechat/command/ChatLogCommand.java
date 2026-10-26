package ru.nothingflare.simplechat.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.nothingflare.simplechat.SimpleChatLogPlugin;
import ru.nothingflare.simplechat.config.LanguageManager;
import ru.nothingflare.simplechat.model.ChatLogEntry;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChatLogCommand implements CommandExecutor, TabCompleter {

    private final SimpleChatLogPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public ChatLogCommand(SimpleChatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lm = plugin.getLanguageManager();

        if (!sender.hasPermission("chatlog.admin")) {
            sender.sendMessage(lm.getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "stats" -> handleStats(sender);
            case "search" -> handleSearch(sender, args);
            case "live", "spy" -> handleLive(sender);
            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void handleLive(CommandSender sender) {
        LanguageManager lm = plugin.getLanguageManager();
        boolean enabled = plugin.toggleLiveListener(sender);
        if (enabled) {
            sender.sendMessage(lm.getMessage("live_enabled"));
        } else {
            sender.sendMessage(lm.getMessage("live_disabled"));
        }
    }

    private void handleReload(CommandSender sender) {
        LanguageManager lm = plugin.getLanguageManager();
        sender.sendMessage(lm.getMessage("reloading"));
        plugin.reloadPluginConfig();
        sender.sendMessage(lm.getMessage("reloaded"));
    }

    private void handleStats(CommandSender sender) {
        LanguageManager lm = plugin.getLanguageManager();

        sender.sendMessage(lm.getMessage("stats_header"));
        sender.sendMessage(lm.getMessage("stats_db_type", Collections.singletonMap(
                "db_type", plugin.getDatabaseManager().getDatabaseType().name()
        )));
        sender.sendMessage(lm.getMessage("stats_queue_pending", Collections.singletonMap(
                "pending", String.valueOf(plugin.getQueue().getPendingCount())
        )));
        sender.sendMessage(lm.getMessage("stats_session_logged", Collections.singletonMap(
                "session_logged", String.valueOf(plugin.getQueue().getTotalLogged())
        )));

        CompletableFuture.runAsync(() -> {
            long totalInDb = plugin.getDatabaseManager().getTotalLogsCount();
            sender.sendMessage(lm.getMessage("stats_total_db", Collections.singletonMap(
                    "total", String.valueOf(totalInDb)
            )));
            sender.sendMessage(lm.getMessage("stats_footer"));
        });
    }

    private void handleSearch(CommandSender sender, String[] args) {
        LanguageManager lm = plugin.getLanguageManager();

        if (args.length < 2) {
            sender.sendMessage(lm.getMessage("search_usage"));
            return;
        }

        String targetPlayer = args[1];
        int limit = 10;
        if (args.length >= 3) {
            try {
                limit = Math.max(1, Math.min(Integer.parseInt(args[2]), 50));
            } catch (NumberFormatException e) {
                sender.sendMessage(lm.getMessage("search_invalid_limit"));
            }
        }

        final int queryLimit = limit;
        sender.sendMessage(lm.getMessage("search_searching", Collections.singletonMap("player", targetPlayer)));

        CompletableFuture.runAsync(() -> {
            List<ChatLogEntry> results = plugin.getDatabaseManager().searchLogs(targetPlayer, queryLimit);
            if (results.isEmpty()) {
                sender.sendMessage(lm.getMessage("search_not_found", Collections.singletonMap("player", targetPlayer)));
                return;
            }

            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("player", targetPlayer);
            headerMap.put("count", String.valueOf(results.size()));
            sender.sendMessage(lm.getMessage("search_header", headerMap));

            for (ChatLogEntry entry : results) {
                Map<String, String> entryMap = new HashMap<>();
                entryMap.put("date", dateFormat.format(new Date(entry.getTimestamp())));
                entryMap.put("world", entry.getWorld());
                entryMap.put("x", String.format(Locale.ROOT, "%.0f", entry.getX()));
                entryMap.put("y", String.format(Locale.ROOT, "%.0f", entry.getY()));
                entryMap.put("z", String.format(Locale.ROOT, "%.0f", entry.getZ()));
                entryMap.put("player", entry.getPlayerName());
                entryMap.put("message", entry.getMessage());

                sender.sendMessage(lm.getMessage("search_format", entryMap));
            }
            sender.sendMessage(lm.getMessage("search_footer"));
        });
    }

    private void sendHelp(CommandSender sender, String label) {
        LanguageManager lm = plugin.getLanguageManager();
        Map<String, String> cmdMap = Collections.singletonMap("cmd", label);

        sender.sendMessage(lm.getMessage("help_header"));
        sender.sendMessage(lm.getMessage("help_live", cmdMap));
        sender.sendMessage(lm.getMessage("help_stats", cmdMap));
        sender.sendMessage(lm.getMessage("help_search", cmdMap));
        sender.sendMessage(lm.getMessage("help_reload", cmdMap));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("chatlog.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = Arrays.asList("live", "stats", "search", "reload");
            String prefix = args[0].toLowerCase();
            List<String> filtered = new ArrayList<>();
            for (String s : list) {
                if (s.startsWith(prefix)) {
                    filtered.add(s);
                }
            }
            return filtered;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("search")) {
            String prefix = args[1].toLowerCase();
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    playerNames.add(p.getName());
                }
            }
            return playerNames;
        }

        return Collections.emptyList();
    }
}
