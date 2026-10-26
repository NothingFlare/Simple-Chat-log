package ru.nothingflare.simplechat.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.nothingflare.simplechat.SimpleChatLogPlugin;

public class PaperChatListener implements Listener {

    private final SimpleChatLogPlugin plugin;

    public PaperChatListener(SimpleChatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPaperChat(AsyncChatEvent event) {
        if (plugin.isHideChatFromConsole()) {
            event.viewers().remove(plugin.getServer().getConsoleSender());
        }

        if (event.isCancelled() && !plugin.isLogCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        plugin.getQueue().log(player, message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.removeLiveListener(event.getPlayer());
    }
}
