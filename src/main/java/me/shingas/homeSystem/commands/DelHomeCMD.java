package me.shingas.homeSystem.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.shingas.homeSystem.managers.HomesManager;
import me.shingas.homeSystem.utils.MM;
import me.shingas.homeSystem.utils.MessagesUtil;
import me.shingas.homeSystem.utils.Placeholders;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;

public class DelHomeCMD implements BasicCommand {

    private final JavaPlugin plugin;
    private final HomesManager homesManager;
    private final MessagesUtil messages;

    public DelHomeCMD(JavaPlugin plugin, HomesManager homesManager, MessagesUtil messages) {
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
            player.sendMessage(MM.mm("<prefix> <red>Usage: /delhome <name></red>",
                    Placeholders.p("prefix", prefix)));
            return;
        }

        String name = args[0].toLowerCase();

        if (!homesManager.deleteHome(player.getUniqueId(), name)) {
            player.sendMessage(messages.msg("home-not-found"));
            return;
        }

        player.sendMessage(messages.msg("home-deleted", "home", name));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack ctx, String[] args) {
        if (!(ctx.getSender() instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length < 1) {
            return homesManager.getHomeNames(player.getUniqueId())
                    .stream()
                    .sorted()
                    .toList();
        }

        if (args.length == 1) {
            String current = args[0].toLowerCase();

            return homesManager.getHomeNames(player.getUniqueId())
                    .stream()
                    .filter(home -> home.startsWith(current))
                    .toList();
        }

        return Collections.emptyList();
    }
}
