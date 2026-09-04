package me.shingas.homeSystem.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HomeDatabase implements AutoCloseable {

    private final JavaPlugin plugin;
    private final String url;
    private final String username;
    private final String password;
    private final String homesTable;
    private final String pendingTable;

    public HomeDatabase(JavaPlugin plugin) {
        this.plugin = plugin;

        String host = required("database.host");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = required("database.name");
        this.username = required("database.username");
        this.password = plugin.getConfig().getString("database.password", "");

        String prefix = plugin.getConfig().getString("database.table-prefix", "homesystem_")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        if (prefix.isBlank()) prefix = "homesystem_";

        this.homesTable = prefix + "homes";
        this.pendingTable = prefix + "pending_teleports";
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&serverTimezone=UTC";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initialize();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is not available", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize the home database", e);
        }
    }

    private String required(String path) {
        String value = plugin.getConfig().getString(path);
        if (value == null || value.isBlank() || value.startsWith("change-me")) {
            throw new IllegalStateException("Configure " + path + " in config.yml");
        }
        return value;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private void initialize() throws SQLException {
        String homesSql = "CREATE TABLE IF NOT EXISTS " + homesTable + " ("
                + "uuid CHAR(36) NOT NULL, home_name VARCHAR(16) NOT NULL, "
                + "server_name VARCHAR(64) NOT NULL, world_name VARCHAR(255) NOT NULL, "
                + "x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, "
                + "yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, "
                + "PRIMARY KEY (uuid, home_name))";
        String pendingSql = "CREATE TABLE IF NOT EXISTS " + pendingTable + " ("
                + "uuid CHAR(36) NOT NULL PRIMARY KEY, target_server VARCHAR(64) NOT NULL, "
                + "home_name VARCHAR(16) NOT NULL, created_at TIMESTAMP NOT NULL "
                + "DEFAULT CURRENT_TIMESTAMP)";

        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(homesSql);
            statement.executeUpdate(pendingSql);
        }
    }

    public void saveHome(UUID uuid, String name, HomeEntry entry) throws SQLException {
        String sql = "INSERT INTO " + homesTable
                + " (uuid, home_name, server_name, world_name, x, y, z, yaw, pitch) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE server_name=VALUES(server_name), world_name=VALUES(world_name), "
                + "x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setString(3, entry.server());
            statement.setString(4, entry.world());
            statement.setDouble(5, entry.x());
            statement.setDouble(6, entry.y());
            statement.setDouble(7, entry.z());
            statement.setFloat(8, entry.yaw());
            statement.setFloat(9, entry.pitch());
            statement.executeUpdate();
        }
    }

    public boolean deleteHome(UUID uuid, String name) throws SQLException {
        String sql = "DELETE FROM " + homesTable + " WHERE uuid=? AND home_name=?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            return statement.executeUpdate() > 0;
        }
    }

    public HomeEntry getHome(UUID uuid, String name) throws SQLException {
        String sql = "SELECT server_name, world_name, x, y, z, yaw, pitch FROM "
                + homesTable + " WHERE uuid=? AND home_name=?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new HomeEntry(result.getString("world_name"), result.getDouble("x"),
                        result.getDouble("y"), result.getDouble("z"), result.getFloat("yaw"),
                        result.getFloat("pitch"), result.getString("server_name"));
            }
        }
    }

    public List<String> getHomeNames(UUID uuid) throws SQLException {
        String sql = "SELECT home_name FROM " + homesTable + " WHERE uuid=? ORDER BY home_name";
        List<String> names = new ArrayList<>();
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) names.add(result.getString(1));
            }
        }
        return names;
    }

    public int getHomeCount(UUID uuid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + homesTable + " WHERE uuid=?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    public int getHomesInWorld(UUID uuid, String world) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + homesTable + " WHERE uuid=? AND world_name=?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, world);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    public void setPendingTeleport(UUID uuid, String server, String home) throws SQLException {
        String sql = "INSERT INTO " + pendingTable + " (uuid, target_server, home_name) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE target_server=VALUES(target_server), home_name=VALUES(home_name), "
                + "created_at=CURRENT_TIMESTAMP";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, server);
            statement.setString(3, home);
            statement.executeUpdate();
        }
    }

    public String consumePendingTeleport(UUID uuid, String server) throws SQLException {
        String sql = "SELECT home_name FROM " + pendingTable + " WHERE uuid=? AND target_server=?";
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, server);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                String home = result.getString(1);
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM " + pendingTable + " WHERE uuid=?")) {
                    delete.setString(1, uuid.toString());
                    delete.executeUpdate();
                }
                return home;
            }
        }
    }

    @Override
    public void close() {
        // Connections are short-lived and are closed after each operation.
    }
}
