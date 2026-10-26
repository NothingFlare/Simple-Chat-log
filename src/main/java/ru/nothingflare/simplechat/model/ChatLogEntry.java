package ru.nothingflare.simplechat.model;

import java.util.UUID;

public class ChatLogEntry {

    private final UUID playerUuid;
    private final String playerName;
    private final String message;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final long timestamp;

    public ChatLogEntry(UUID playerUuid, String playerName, String message, String world, double x, double y, double z, long timestamp) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.message = message;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
