package ru.nothingflare.simplechat.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.nothingflare.simplechat.SimpleChatLogPlugin;

public class BukkitChatListener implements Listener {

    private final SimpleChatLogPlugin plugin;

    public BukkitChatListener(SimpleChatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() && !plugin.isLogCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        plugin.getQueue().log(player, message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.removeLiveListener(event.getPlayer());
    }
}
