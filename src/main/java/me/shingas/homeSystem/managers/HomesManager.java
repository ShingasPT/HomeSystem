package me.shingas.homeSystem.managers;

import me.shingas.homeSystem.data.HomeDatabase;
import me.shingas.homeSystem.data.HomeEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomesManager {

    private final JavaPlugin plugin;
    private final HomeDatabase database;
    private final Map<String, Integer> worldLimitCache = new ConcurrentHashMap<>();

    public HomesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.database = new HomeDatabase(plugin);
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private <T> T databaseCall(DatabaseCall<T> call, T fallback) {
        try {
            return call.run();
        } catch (SQLException e) {
            plugin.getLogger().severe("Home database operation failed: " + e.getMessage());
            return fallback;
        }
    }

    @FunctionalInterface
    private interface DatabaseCall<T> {
        T run() throws SQLException;
    }

    public void load() {
        // The database is initialized by the constructor.
    }

    public void save() {
        // Writes are committed immediately by HomeDatabase.
    }

    public void close() {
        database.close();
    }

    public synchronized void setHome(Player player, String name) {
        Location loc = player.getLocation();
        HomeEntry entry = new HomeEntry(player.getWorld().getName(), loc.getX(), loc.getY(),
                loc.getZ(), loc.getYaw(), loc.getPitch(),
                plugin.getConfig().getString("server-name", "vsmp"));
        try {
            database.saveHome(player.getUniqueId(), normalize(name), entry);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save home", e);
        }
    }

    public synchronized boolean deleteHome(UUID uuid, String name) {
        return databaseCall(() -> database.deleteHome(uuid, normalize(name)), false);
    }

    public synchronized HomeEntry getHomeEntry(UUID uuid, String name) {
        return databaseCall(() -> database.getHome(uuid, normalize(name)), null);
    }

    public synchronized Location getHome(UUID uuid, String name) {
        HomeEntry entry = getHomeEntry(uuid, name);
        if (entry == null) return null;

        World world = Bukkit.getWorld(entry.world());
        if (world == null) return null;
        return new Location(world, entry.x(), entry.y(), entry.z(), entry.yaw(), entry.pitch());
    }

    public synchronized Set<String> getHomeNames(UUID uuid) {
        List<String> names = databaseCall(() -> database.getHomeNames(uuid), Collections.emptyList());
        return new TreeSet<>(names);
    }

    public synchronized int getHomeCount(UUID uuid) {
        return databaseCall(() -> database.getHomeCount(uuid), 0);
    }

    public int getMaxHomes(Player player) {
        if (player.hasPermission("vsmp.homes.admin")) return Integer.MAX_VALUE;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("homes.multiple-homes");
        if (section == null) return 1;

        int max = section.getInt("default", 1);
        for (String group : section.getKeys(false)) {
            if (!group.equalsIgnoreCase("default")
                    && player.hasPermission("vsmp.homes." + group)) {
                max = Math.max(max, section.getInt(group));
            }
        }
        return max;
    }

    public boolean canCreateNewHome(Player player, String name) {
        if (homeExists(player.getUniqueId(), name)) return true;
        return getHomeCount(player.getUniqueId()) < getMaxHomes(player);
    }

    public boolean homeExists(UUID uuid, String name) {
        return getHomeEntry(uuid, name) != null;
    }

    public int getWorldLimit(World world) {
        return worldLimitCache.computeIfAbsent(world.getName(),
                name -> plugin.getConfig().getInt("homes.world-limits." + name, -1));
    }

    public int getHomesInWorld(UUID uuid, World world) {
        return databaseCall(() -> database.getHomesInWorld(uuid, world.getName()), 0);
    }

    public void setPendingTeleport(UUID uuid, String server, String home) {
        try {
            database.setPendingTeleport(uuid, server, normalize(home));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save pending home teleport", e);
        }
    }

    public String consumePendingTeleport(UUID uuid, String server) {
        return databaseCall(() -> database.consumePendingTeleport(uuid, server), null);
    }
}
