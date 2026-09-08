# HomeSystem

**HomeSystem** is a Minecraft homes plugin that allows players to create, manage, list, and teleport to personal homes.

It is designed for modern Paper/Folia servers and supports persistent storage, configurable home limits, and proxy/server-aware teleport handling.

## Features

- Create homes with `/sethome`
- Teleport to homes with `/home`
- Delete homes with `/delhome`
- List homes with `/homes`
- Per-player home storage
- Per-world home limits
- Permission-based home limits
- Persistent database storage
- Configurable messages
- Cross-server pending home teleports
- BungeeCord plugin messaging
- GriefPrevention integration
- Folia support

## Commands

| Command | Description |
|---|---|
| `/sethome` | Create or update a home |
| `/home` | Teleport to a home |
| `/delhome` | Delete a home |
| `/homes` | List your homes |

## Configuration

HomeSystem provides a `config.yml` for server-specific settings, including the configured server name and home-related limits.

Messages are handled separately through the plugin's message utility, allowing server owners to customize player-facing feedback.

## Storage

Home data is managed through `HomesManager` and persisted using a database-backed system.

The plugin includes:

- MySQL Connector/J
- HikariCP connection pooling

This makes it suitable for servers where player data needs to persist reliably across restarts and potentially across multiple servers.

## Cross-Server Teleportation

HomeSystem registers the `BungeeCord` outgoing plugin channel.

When a player requests a home located on another configured server, the plugin can store a pending teleport and complete the process when the player joins the destination server.

## Requirements

- Minecraft/Paper 26.2
- GriefPrevention
- Java
- MySQL-compatible database

The plugin declares Folia support.

## Building

The project uses Gradle with Kotlin build scripts.

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The resulting JAR will be generated in:

```text
build/libs/
```

## Installation

1. Install GriefPrevention.
2. Configure your database connection.
3. Place `HomeSystem.jar` into the server's `plugins` directory.
4. Start the server.
5. Adjust `config.yml` to suit your network.

## Architecture

```text
me.shingas.homeSystem
├── commands
├── data
├── managers
├── utils
└── HomeSystem.java
```

The main plugin class handles lifecycle and command registration, while home persistence and business logic are separated into dedicated managers and data classes.

## License

This project is open source. Check the repository for the applicable license.
