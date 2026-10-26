package ru.nothingflare.simplechat.database;

public enum DatabaseType {
    SQLITE,
    MYSQL;

    public static DatabaseType fromString(String type) {
        if (type == null) {
            return SQLITE;
        }
        return switch (type.toLowerCase().trim()) {
            case "mysql" -> MYSQL;
            default -> SQLITE;
        };
    }
}
