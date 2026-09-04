package me.shingas.homeSystem.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.shingas.homeSystem.managers.HomesManager;
import me.shingas.homeSystem.data.HomeEntry;
import me.shingas.homeSystem.utils.MM;
import me.shingas.homeSystem.utils.MessagesUtil;
import me.shingas.homeSystem.utils.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class HomeCMD implements BasicCommand {

    private final JavaPlugin plugin;
    private final HomesManager homesManager;
    private final MessagesUtil messages;

    public HomeCMD(JavaPlugin plugin, HomesManager homesManager, MessagesUtil messages) {
        this.plugin = plugin;
        this.homesManager = homesManager;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack ctx, String[] args) {
        CommandSender sender = ctx.getSender();
        String prefix = plugin.getConfig().getString("Prefix");

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }

        if (args.length < 1) {
            player.sendMessage(MM.mm("<prefix> <red>Usage: /home <name></red>",
                    Placeholders.p("prefix", prefix)));
            return;
        }

        String homeName;
        UUID targetUUID = player.getUniqueId();

        // Staff: player:home
        if (player.hasPermission("vsmp.homes.staff") && args[0].contains(":")) {
            String[] parts = args[0].split(":", 2);

            if (parts.length != 2) {
                player.sendMessage(MM.mm("<prefix> <red>Invalid format. Use /home player:home</red>",
                        Placeholders.p("prefix", prefix)));
                return;
            }

            String targetPlayerName = parts[0];
            homeName = parts[1].toLowerCase();

            targetUUID = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
        } else {
            homeName = args[0].toLowerCase();
        }

        HomeEntry entry = homesManager.getHomeEntry(targetUUID, homeName);
        if (entry == null) {
            player.sendMessage(messages.msg("home-not-found"));
            return;
        }

        String currentServer = plugin.getConfig().getString("server-name", "vsmp");
        if (entry.server() != null && !entry.server().equalsIgnoreCase(currentServer)) {
            if (!(plugin instanceof me.shingas.homeSystem.HomeSystem homeSystem)) {
                player.sendMessage(messages.msg("proxy-unavailable"));
                return;
            }
            homesManager.setPendingTeleport(player.getUniqueId(), entry.server(), homeName);
            homeSystem.connectToServer(player, entry.server());
            return;
        }

        Location location = homesManager.getHome(targetUUID, homeName);
        if (location == null) {
            player.sendMessage(messages.msg("home-world-unavailable"));
            return;
        }

        player.setVelocity(new Vector(0, 0, 0));
        player.teleport(location);
        player.sendMessage(messages.msg("home-teleported", "home", homeName));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack ctx, String[] args) {
        if (!(ctx.getSender() instanceof Player player)) return Collections.emptyList();

        if (args.length < 1) {
            return homesManager.getHomeNames(player.getUniqueId())
                    .stream()
                    .sorted()
                    .toList();
        }

        // /home <tab>
        if (args.length == 1) {
            String input = args[0];

            // STAFF: /home player:
            if ((player.hasPermission("vsmp.homes.staff") || player.isOp()) && input.contains(":")) {
                String[] parts = input.split(":", 2);

                String targetName = parts[0];

                UUID targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();

                // If typing only "ShingasPT:" -> show ALL homes
                if (parts.length == 1 || parts[1].isEmpty()) {
                    return homesManager.getHomeNames(targetUUID).stream()
                            .sorted()
                            .map(home -> targetName + ":" + home)
                            .toList();
                }

                // If typing "ShingasPT:ho" -> filter homes
                return homesManager.getHomeNames(targetUUID).stream()
                        .filter(home -> home.toLowerCase().startsWith(parts[1].toLowerCase()))
                        .sorted()
                        .map(home -> targetName + ":" + home)
                        .toList();
            }

            // NORMAL: own homes
            return homesManager.getHomeNames(player.getUniqueId());
        }

        return Collections.emptyList();
    }


}
