package me.shingas.homeSystem.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.shingas.homeSystem.managers.HomesManager;
import me.shingas.homeSystem.utils.MM;
import me.shingas.homeSystem.utils.MessagesUtil;
import me.shingas.homeSystem.utils.Placeholders;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HomesCMD implements BasicCommand {

    private final JavaPlugin plugin;
    private final HomesManager homesManager;
    private final MessagesUtil messages;

    public HomesCMD(JavaPlugin plugin, HomesManager homesManager, MessagesUtil messages) {
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

        UUID uuid = player.getUniqueId();

        Set<String> homes = homesManager.getHomeNames(uuid);
        if (homes.isEmpty()) {
            player.sendMessage(MM.mm("<prefix> <red>You have no homes set.</red>",
                    Placeholders.p("prefix", prefix)));
            return;
        }

        List<String> formatted = new ArrayList<>();

        for (String home : homes) {
            Location loc = homesManager.getHome(uuid, home);
            if (loc == null) continue;

            formatted.add(
                    "<newline><gold>" + home +
                            " <dark_gray>»</dark_gray> " +
                            "<gold>X: <yellow>" + loc.getBlockX() + " " +
                            "<gold>Y: <yellow>" + loc.getBlockY() + " " +
                            "<gold>Z: <yellow>" + loc.getBlockZ() + " " +
                            "<dark_gray>(<gold>" + loc.getWorld().getName() + "<dark_gray>)"
            );
        }

        player.sendMessage(messages.msg(
                "home-list-header",
                "homes",
                String.join("", formatted)
        ));
    }

}
