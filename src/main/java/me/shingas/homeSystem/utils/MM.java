package me.shingas.homeSystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

public class MM {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MM() {}

    public static Component mm(String raw) {
        return MM.deserialize(raw);
    }

    public static Component mm(String raw, TagResolver... resolvers) {
        return MM.deserialize(raw, resolvers);
    }

    public static String raw(Component msg) { return MM.serialize(msg); }

    public static void broadcast(String raw) { Bukkit.broadcast(mm(raw)); }

    public static void broadcast(String raw, TagResolver... resolvers) { Bukkit.broadcast(mm(raw, resolvers)); }

    public static String convertLegacy(String legacy) {
        return MiniMessage.miniMessage().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(legacy)
        );
    }

}
