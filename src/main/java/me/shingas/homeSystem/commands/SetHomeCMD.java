package me.shingas.homeSystem.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.shingas.homeSystem.managers.HomesManager;
import me.shingas.homeSystem.utils.MM;
import me.shingas.homeSystem.utils.MessagesUtil;
import me.shingas.homeSystem.utils.Placeholders;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

public class SetHomeCMD implements BasicCommand {

    private final JavaPlugin plugin;
    private final HomesManager homesManager;
    private final MessagesUtil messages;

    public SetHomeCMD(JavaPlugin plugin, HomesManager homesManager, MessagesUtil messages) {
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
            player.sendMessage(MM.mm("<prefix> <red>Usage: /sethome <name></red>",
                    Placeholders.p("prefix", prefix)));
            return;
        }

        String name = args[0].toLowerCase();

        if (!name.matches("^[a-zA-Z0-9_-]{1,16}$")) {
            player.sendMessage(MM.mm("<prefix> <red>Home name must be 1-16 characters and only contain letters, numbers, _ or -.</red>",
                    Placeholders.p("prefix", prefix)));
            return;
        }

        String serverName = plugin.getConfig().getString("server-name", "vsmp");
        if (plugin.getConfig().getStringList("sethome-blocked-servers").stream()
                .anyMatch(server -> server.equalsIgnoreCase(serverName))) {
            player.sendMessage(MM.mm("<prefix> <red>You cannot set homes on this server.</red>",
                    Placeholders.p("prefix", prefix)));
            return;
        }

        // Allow admins using /ignoreclaims to bypass this restriction
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        boolean ignoringClaims = playerData != null && playerData.ignoreClaims;

        if (!ignoringClaims) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);

            if (claim != null) {
                Supplier<String> denialReason = claim.checkPermission(player, ClaimPermission.Build, null);

                if (denialReason != null) {
                    player.sendMessage(messages.msg("no-claim-permission"));
                    return;
                }
            }
        }

        World world = player.getWorld();
        int worldLimit = homesManager.getWorldLimit(world);

        // blocked world
        if (worldLimit == 0) {
            player.sendMessage(MM.mm("<prefix> <red>You cannot set homes in this world.</red>",
                    Placeholders.p("prefix", prefix)));
            return;
        }

        int currentInWorld = homesManager.getHomesInWorld(player.getUniqueId(), world);

        // world override applies
        if (worldLimit > -1) {
            if (currentInWorld >= worldLimit) {
                player.sendMessage(MM.mm("<prefix> <red>You can only set " +
                                worldLimit + " home" +
                                (worldLimit == 1 ? "" : "s") +
                                " in this world.</red>",
                        Placeholders.p("prefix", prefix)));
                return;
            }
        }

        if (!homesManager.canCreateNewHome(player, name)) {
            player.sendMessage(messages.msg("home-limit"));
            return;
        }

        homesManager.setHome(player, name);
        player.sendMessage(messages.msg("home-set", "home", name));
    }

}
