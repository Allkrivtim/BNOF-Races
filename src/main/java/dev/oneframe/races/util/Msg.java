package dev.oneframe.races.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public final class Msg {

    private Msg() {
    }

    /** Plain (non-italic) display name component, for item names that shouldn't render italic. */
    public static Component itemName(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }

    public static void info(CommandSender to, String text) {
        to.sendMessage(Component.text(text, NamedTextColor.GRAY));
    }

    public static void ok(CommandSender to, String text) {
        to.sendMessage(Component.text(text, NamedTextColor.GREEN));
    }

    public static void error(CommandSender to, String text) {
        to.sendMessage(Component.text(text, NamedTextColor.RED));
    }

    public static void header(CommandSender to, String text) {
        to.sendMessage(Component.text(text, NamedTextColor.GOLD));
    }
}
