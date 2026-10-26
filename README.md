<div align="center">

# SimpleChatLog

**A lightweight, high-performance asynchronous chat logging plugin for Paper, Purpur, Spigot, and Folia with MySQL and SQLite support.**

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.16.5--1.21+-2b2b2b.svg?style=flat-square)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-17+-2b2b2b.svg?style=flat-square)](https://www.oracle.com/java/)
[![Folia](https://img.shields.io/badge/Folia-Supported-2b2b2b.svg?style=flat-square)](https://papermc.io/software/folia)
[![Release](https://img.shields.io/badge/Release-1.2-2b2b2b.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-2b2b2b.svg?style=flat-square)](LICENSE)

[Overview](#overview) • [Key Features](#key-features) • [Compatibility](#compatibility) • [Commands and Permissions](#commands-and-permissions) • [Configuration](#configuration) • [Localization](#localization) • [Database Structure](#database-structure) • [Installation](#installation) • [Compiling from Source](#compiling-from-source)

</div>

---

## Overview

SimpleChatLog is an optimized, non-blocking chat logging solution engineered specifically for modern Minecraft server environments. Traditional chat loggers frequently suffer from synchronous disk I/O bottlenecks and database query stalls directly on network netty or main tick threads. Under heavy chat traffic or slow database responses, this can trigger severe TPS drops, server lag spikes, and thread lockups.

SimpleChatLog resolves these architectural limitations by implementing a completely asynchronous queuing model. Player messages are captured with sub-microsecond latency into a thread-safe, non-blocking memory queue (`ConcurrentLinkedQueue`). A dedicated background daemon worker flushes messages in batched SQL transactions (`executeBatch()`), decoupling database persistence entirely from the server game loop.

The plugin provides dual storage backend support (SQLite with WAL mode and MySQL with shaded HikariCP connection pooling), a clean console mode to eliminate console log spam, an on-demand real-time live spy stream for staff, full Folia compatibility, and built-in localization for 13 languages.

---

## Key Features

- **Dual Storage Engine Support**
  - **SQLite**: Single-file local database with automated `PRAGMA journal_mode = WAL` and `PRAGMA synchronous = NORMAL` for fast concurrent read/write operations without file-locking contention.
  - **MySQL / MariaDB**: Enterprise-grade connection pooling powered by shaded **HikariCP** with `utf8mb4_unicode_ci` encoding, ensuring flawless storage of emojis, Unicode symbols, and multi-byte characters without database exceptions.
  - Toggle between backends with a single configuration parameter (`database.type`).

- **100% Asynchronous & Non-Blocking Pipeline**
  - Chat event listeners immediately push entries into an in-memory `ConcurrentLinkedQueue` with sub-microsecond latency (<0.001 ms).
  - Background daemon thread batches writes in configurable time intervals and batch sizes using prepared statement batching inside explicit SQL transactions.
  - Guaranteed graceful shutdown flush ensures all pending queue entries are safely committed during server stops or reloads with zero data loss.

- **Universal Chat Plugin Compatibility**
  - Listens at `EventPriority.LOWEST` to capture authentic raw player messages before formatting plugins (such as **OCDChat**, **Chatty**, **EssentialsX Chat**, **LPC**, **VentureChat**, **InteractiveChat**, or **TownyChat**) cancel the event or strip channel prefixes (`!`, `~`).
  - Employs a hybrid dual-listener pipeline on Paper servers that processes both Paper Adventure `AsyncChatEvent` and legacy Spigot `AsyncPlayerChatEvent`.
  - Built-in 250 ms deduplication cache prevents duplicate entries when events traverse Paper's legacy event bridge.

- **Clean Console & Real-Time Live Spy**
  - Automatically suppresses player chat flooding from the server console (`hide-chat-from-console: true`), keeping console output readable.
  - Built-in `/chatlog live` (or `/chatlog spy`) command allows in-game administrators and console operators to toggle an on-demand real-time stream of incoming player chat messages with world coordinates.

- **In-Game Asynchronous Log Inspection & Analytics**
  - Search any player's recent message history in real-time (`/chatlog search <player> [limit]`) with formatted timestamps, world names, and exact XYZ coordinates.
  - Inspect queue backlog, session throughput, and total database record metrics instantly (`/chatlog stats`).

- **Comprehensive Multi-Language Localization**
  - Bundled with 13 built-in translations out of the box.
  - Full support for MiniMessage, legacy `&` color codes, and RGB HEX formatting (`&#RRGGBB`, `<#RRGGBB>`, `{#RRGGBB}`).

- **Native Folia & Paper Concurrency**
  - Declares `folia-supported: true` in the plugin manifest.
  - Operates via Java's native scheduled executor service, completely independent of Bukkit's tick scheduler.

---

## Compatibility

| Platform | Supported Versions | Implementation Details |
| :--- | :--- | :--- |
| **Paper** | 1.16.5 – 1.21+ | Modern Kyori Adventure `AsyncChatEvent` with PlainText serializer |
| **Folia** | 1.19.4 – 1.21+ | Region-independent decoupled background scheduler |
| **Purpur** | 1.16.5 – 1.21+ | Fully supported |
| **Spigot / CraftBukkit** | 1.13 – 1.21+ | Classic `AsyncPlayerChatEvent` pipeline |

---

## Commands and Permissions

### Commands

| Command | Aliases | Description | Default Permission |
| :--- | :--- | :--- | :--- |
| `/chatlog live` | `/chatlog spy`, `/clog live` | Toggles real-time chat monitoring (works in-game and in console) | `chatlog.admin` |
| `/chatlog search <player> [limit]` | `/clog search` | Asynchronously queries and displays recent messages from a player | `chatlog.admin` |
| `/chatlog stats` | `/clog stats` | Displays database type, queue size, and total recorded message counts | `chatlog.admin` |
| `/chatlog reload` | `/clog reload` | Reloads configuration, active language files, and worker queues | `chatlog.admin` |

### Permissions

| Node | Description | Default Assignment |
| :--- | :--- | :--- |
| `chatlog.admin` | Grants full access to all SimpleChatLog commands (`live`, `search`, `stats`, `reload`) | `op` |

---

## Configuration

The default configuration file (`config.yml`):

```yaml
# =======================================================
#               SimpleChatLog Configuration
# =======================================================

# Active plugin language:
# Supported: en, ru, zh, de, fr, es, pt, ja, uk, it, pl, tr, ko
language: "en"

# Database storage engine: 'sqlite' or 'mysql'
database:
  type: "sqlite" # sqlite or mysql

  # SQLite settings (local file storage)
  sqlite:
    file: "chat_logs.db"

  # MySQL / MariaDB settings
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft_chat"
    username: "root"
    password: ""
    pool-size: 10
    connection-timeout: 30000

# Operational settings
settings:
  # Save messages exclusively to database and suppress them from the server console.
  # If true: Player chat will not clutter console logs.
  # Operators can view live chat anytime using: /chatlog live (or console: chatlog live)
  hide-chat-from-console: true

  # Record messages even if they were cancelled by other plugins.
  # Recommended 'true': Chat formatting plugins (OCDChat, Chatty, EssentialsX, VentureChat, etc.)
  # intentionally cancel standard Bukkit events to send custom distance/channel messages.
  log-cancelled: true

  # Chat listener event mode:
  # 'auto'   - Hybrid mode; auto-detects Paper/Spigot with deduplication (Recommended)
  # 'paper'  - Forces modern Paper AsyncChatEvent (Paper 1.16.5+)
  # 'spigot' - Forces classic Spigot AsyncPlayerChatEvent
  chat-event: "auto"

  # Asynchronous queue and batch writing settings
  queue:
    # Interval between batch writes to database (in milliseconds)
    # Default: 1000 ms (1 second)
    flush-interval-ms: 1000

    # Maximum batch size per single database transaction
    # Default: 100
    batch-size: 100
```

---

## Localization

Pre-configured language files are located in `plugins/SimpleChatLog/lang/`:

| Code | Language | Native Name | File |
| :---: | :--- | :--- | :--- |
| `en` | English | English | `messages_en.yml` |
| `ru` | Russian | Русский | `messages_ru.yml` |
| `zh` | Chinese (Simplified) | 简体中文 | `messages_zh.yml` |
| `de` | German | Deutsch | `messages_de.yml` |
| `fr` | French | Français | `messages_fr.yml` |
| `es` | Spanish | Español | `messages_es.yml` |
| `pt` | Portuguese | Português | `messages_pt.yml` |
| `ja` | Japanese | 日本語 | `messages_ja.yml` |
| `uk` | Ukrainian | Українська | `messages_uk.yml` |
| `it` | Italian | Italiano | `messages_it.yml` |
| `pl` | Polish | Polski | `messages_pl.yml` |
| `tr` | Turkish | Türkçe | `messages_tr.yml` |
| `ko` | Korean | 한국어 | `messages_ko.yml` |

---

## Database Structure

The `chat_logs` table is automatically initialized on startup with pre-built indices for rapid query execution:

| Column | Type (MySQL) | Type (SQLite) | Description | Indexed |
| :--- | :--- | :--- | :--- | :---: |
| `id` | `BIGINT AUTO_INCREMENT` | `INTEGER PRIMARY KEY AUTOINCREMENT` | Unique row identifier | Primary Key |
| `player_uuid` | `VARCHAR(36)` | `VARCHAR(36)` | Unique Minecraft Player UUID | Yes (`idx_chat_player_uuid`) |
| `player_name` | `VARCHAR(64)` | `VARCHAR(64)` | Player username at time of message | Yes (`idx_chat_player_name`) |
| `message` | `TEXT` | `TEXT` | Raw chat message content | No |
| `world` | `VARCHAR(64)` | `VARCHAR(64)` | World name where message was sent | No |
| `x` | `DOUBLE` | `REAL` | Player X coordinate | No |
| `y` | `DOUBLE` | `REAL` | Player Y coordinate | No |
| `z` | `DOUBLE` | `REAL` | Player Z coordinate | No |
| `created_at` | `BIGINT` | `INTEGER` | Epoch timestamp in milliseconds | Yes (`idx_chat_created_at`) |

---

## Installation

1. Download `simple-chat-log-1.2.jar` from the release section.
2. Place the `.jar` file into your server's `plugins/` directory.
3. Start or reload the server to generate default configuration and language files.
4. Modify `plugins/SimpleChatLog/config.yml` according to your database requirements (SQLite or MySQL).
5. Execute `/chatlog reload` to apply configuration adjustments without restarting the server.

---

## Compiling from Source

### Requirements
- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.8+
- Git

### Build Instructions

```bash
git clone https://github.com/your-username/SimpleChatLog.git
cd SimpleChatLog
mvn clean package
```

The compiled and shaded binary will be generated in:
```text
target/simple-chat-log-1.2.jar
```

---

## License

This project is licensed under the [MIT License](LICENSE).
