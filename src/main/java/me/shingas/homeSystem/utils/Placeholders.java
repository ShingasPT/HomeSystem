package me.shingas.homeSystem.utils;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class Placeholders {

    private Placeholders() {}

    public static TagResolver p(String key, String value) {
        return Placeholder.parsed(key, value == null ? "" : value);
    }

}
