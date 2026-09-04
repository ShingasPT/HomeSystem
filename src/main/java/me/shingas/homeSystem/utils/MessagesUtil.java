package me.shingas.homeSystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.plugin.java.JavaPlugin;

public class MessagesUtil {

    private final JavaPlugin plugin;

    public MessagesUtil(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String get(String path) {
        return plugin.getConfig().getString("homes.messages." + path, "<red>Missing message: " + path + "</red>");
    }

    public Component msg(String path, String... replacements) {
        String message = get(path);

        for (int i = 0; i < replacements.length; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];

            message = message.replace("<" + key + ">", value);
            message = message.replace("%" + key + "%", value);
        }

        String prefix = plugin.getConfig().getString("Prefix", "");

        TagResolver tags = TagResolver.resolver(
                Placeholders.p("prefix", prefix)
        );

        return MM.mm(message, tags);
    }
}
