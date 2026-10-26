package ru.nothingflare.simplechat.queue;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.nothingflare.simplechat.SimpleChatLogPlugin;
import ru.nothingflare.simplechat.model.ChatLogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class ChatLogQueue {

    private final SimpleChatLogPlugin plugin;
    private final ConcurrentLinkedQueue<ChatLogEntry> queue = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalLogged = new AtomicLong(0);

    // Кэш для мгновенной дедупликации сообщений (например, при двойном срабатывании Paper + Spigot моста)
    private final Map<String, Long> recentMessageTimes = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledTask;

    private int batchSize;
    private long flushIntervalMs;

    public ChatLogQueue(SimpleChatLogPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void start() {
        this.batchSize = plugin.getConfig().getInt("settings.queue.batch-size", 100);
        this.flushIntervalMs = plugin.getConfig().getLong("settings.queue.flush-interval-ms", 1000L);

        if (executorService != null && !executorService.isShutdown()) {
            stop();
        }

        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SimpleChatLog-Worker");
            thread.setDaemon(true);
            return thread;
        });

        scheduledTask = executorService.scheduleWithFixedDelay(
                this::flush,
                flushIntervalMs,
                flushIntervalMs,
                TimeUnit.MILLISECONDS
        );

        plugin.getLogger().info("Асинхронный воркер очереди запущен (интервал: " + flushIntervalMs + " мс, пачка: " + batchSize + ").");
    }

    public void log(Player player, String message) {
        if (player == null || message == null || message.trim().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        String deduplicationKey = player.getUniqueId().toString() + ":" + message;
        Long lastSeen = recentMessageTimes.put(deduplicationKey, now);

        // Периодическая очистка устаревших записей дедупликации
        if (recentMessageTimes.size() > 300) {
            recentMessageTimes.entrySet().removeIf(entry -> now - entry.getValue() > 1000L);
        }

        // Если это же сообщение от того же игрока было обработано менее 250 мс назад — это дубликат из моста событий
        if (lastSeen != null && (now - lastSeen) < 250L) {
            return;
        }

        Location loc = player.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";

        ChatLogEntry entry = new ChatLogEntry(
                player.getUniqueId(),
                player.getName(),
                message,
                worldName,
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                now
        );

        queue.offer(entry);
        totalLogged.incrementAndGet();

        // Оповещение слушателей режима реального времени (/chatlog live)
        plugin.broadcastLiveMessage(player, message);
    }

    public void flush() {
        if (queue.isEmpty()) {
            return;
        }

        try {
            while (!queue.isEmpty()) {
                List<ChatLogEntry> batch = new ArrayList<>(batchSize);
                ChatLogEntry entry;
                while (batch.size() < batchSize && (entry = queue.poll()) != null) {
                    batch.add(entry);
                }

                if (!batch.isEmpty()) {
                    plugin.getDatabaseManager().insertBatch(batch);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Непредвиденная ошибка при сбросе очереди чата: " + e.getMessage(), e);
        }
    }

    public synchronized void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executorService = null;
        }

        // Финальный синхронный сброс всех оставшихся сообщений
        flush();
        plugin.getLogger().info("Очередь сообщений успешно остановлена и сброшена в базу данных.");
    }

    public int getPendingCount() {
        return queue.size();
    }

    public long getTotalLogged() {
        return totalLogged.get();
    }
}
