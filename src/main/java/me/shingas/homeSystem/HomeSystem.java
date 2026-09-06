package me.shingas.homeSystem;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.shingas.homeSystem.commands.*;
import me.shingas.homeSystem.managers.HomesManager;
import me.shingas.homeSystem.utils.MessagesUtil;
import me.shingas.homeSystem.data.HomeEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class HomeSystem extends JavaPlugin implements Listener {

    private HomesManager homesManager;
    private MessagesUtil messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info("Loading HomeSystem...");
        homesManager = new HomesManager(this);
        messages = new MessagesUtil(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getPluginManager().registerEvents(this, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("sethome", new SetHomeCMD(this, homesManager, messages));
            event.registrar().register("home", new HomeCMD(this, homesManager, messages));
            event.registrar().register("delhome", new DelHomeCMD(this, homesManager, messages));
            event.registrar().register("homes", new HomesCMD(this, homesManager, messages));
        });

        getLogger().info("HomeSystem has started.");
    }

    @Override
    public void onDisable() {
        if (homesManager != null) {
            homesManager.close();
        }
        getLogger().info("HomeSystem is offline.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String server = getConfig().getString("server-name", "vsmp");
        String pendingHome = homesManager.consumePendingTeleport(player.getUniqueId(), server);
        if (pendingHome == null) return;

        HomeEntry entry = homesManager.getHomeEntry(player.getUniqueId(), pendingHome);
        if (entry == null) {
            player.sendMessage(messages.msg("home-not-found"));
            return;
        }

        Location location = homesManager.getHome(player.getUniqueId(), pendingHome);
        if (location == null) {
            player.sendMessage(messages.msg("home-world-unavailable"));
            return;
        }

        teleportHome(player, location, pendingHome);
    }

    public void teleportHome(Player player, Location location, String homeName) {
        player.setVelocity(new Vector(0, 0, 0));
        player.teleportAsync(location).thenAccept(success -> player.getScheduler().run(this, task -> {
            if (success) {
                player.sendMessage(messages.msg("home-teleported", "home", homeName));
            } else {
                player.sendMessage(messages.msg("home-world-unavailable"));
            }
        }, () -> {}));
    }

    public void connectToServer(Player player, String server) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF("Connect");
            output.writeUTF(server);
            player.sendPluginMessage(this, "BungeeCord", bytes.toByteArray());
        } catch (IOException e) {
            getLogger().severe("Could not send proxy server switch: " + e.getMessage());
            player.sendMessage(messages.msg("proxy-unavailable"));
        }
    }
}
