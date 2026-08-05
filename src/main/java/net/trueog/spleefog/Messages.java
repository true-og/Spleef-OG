package net.trueog.spleefog;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

// The single text pipeline for the plugin.
// Operator-authored strings may be written in either MiniMessage or the legacy ampersand codes the shipped config.yml
// already uses. Existing configs therefore keep working untouched, and an operator can move a value to MiniMessage
// whenever they like.
public final class Messages {

    private static final Pattern LEGACY_CODE = Pattern.compile("[&§]([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_HEX = Pattern.compile("[&§]#([0-9a-f]{6})", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> LEGACY_TO_TAG = Map.ofEntries(Map.entry("0", "<black>"),
            Map.entry("1", "<dark_blue>"), Map.entry("2", "<dark_green>"), Map.entry("3", "<dark_aqua>"),
            Map.entry("4", "<dark_red>"), Map.entry("5", "<dark_purple>"), Map.entry("6", "<gold>"),
            Map.entry("7", "<gray>"), Map.entry("8", "<dark_gray>"), Map.entry("9", "<blue>"),
            Map.entry("a", "<green>"), Map.entry("b", "<aqua>"), Map.entry("c", "<red>"),
            Map.entry("d", "<light_purple>"), Map.entry("e", "<yellow>"), Map.entry("f", "<white>"),
            Map.entry("k", "<obfuscated>"), Map.entry("l", "<bold>"), Map.entry("m", "<strikethrough>"),
            Map.entry("n", "<underlined>"), Map.entry("o", "<italic>"), Map.entry("r", "<reset>"));
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component PREFIX = Component.text().append(Component.text("[", NamedTextColor.DARK_GRAY))
            .append(Component.text("Spleef", NamedTextColor.AQUA))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY)).build();

    private Messages() {

    }

    // Parses a configured string. Legacy codes are rewritten into MiniMessage tags
    // first and the whole string is then
    // deserialised once, so a value may mix both notations and an uppercase code
    // such as &E still works. Codes that are
    // not recognised are left exactly as the author wrote them.
    public static Component parse(String message) {

        if (message == null) {

            return Component.empty();

        }

        return MINI_MESSAGE.deserialize(toTags(message));

    }

    private static String toTags(String message) {

        String hexed = LEGACY_HEX.matcher(message)
                .replaceAll(match -> Matcher.quoteReplacement("<#" + match.group(1).toLowerCase(Locale.ROOT) + ">"));
        return LEGACY_CODE.matcher(hexed).replaceAll(match -> {

            String tag = LEGACY_TO_TAG.get(match.group(1).toLowerCase(Locale.ROOT));
            return Matcher.quoteReplacement(tag == null ? match.group(0) : tag);

        });

    }

    // Parses a configured string for use as an item display name. Minecraft
    // italicises custom names by default, so the
    // decoration is switched off unless the author asked for it.
    public static Component parseItemName(String message) {

        Component parsed = parse(message);
        return parsed.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET
                ? parsed.decoration(TextDecoration.ITALIC, false)
                : parsed;

    }

    public static void send(CommandSender sender, String message) {

        send(sender, parse(message));

    }

    public static void send(CommandSender sender, Component message) {

        sender.sendMessage(PREFIX.append(message));

    }

    // Builds a grey body, the shape used by nearly every message here.
    public static TextComponent.Builder body() {

        return Component.text().color(NamedTextColor.GRAY);

    }

    public static Component value(String text) {

        return Component.text(text, NamedTextColor.AQUA);

    }

    public static Component name(String text) {

        return Component.text(text, NamedTextColor.WHITE);

    }

    public static Component good(String text) {

        return Component.text(text, NamedTextColor.GREEN);

    }

    public static Component bad(String text) {

        return Component.text(text, NamedTextColor.RED);

    }

    public static Component warn(String text) {

        return Component.text(text, NamedTextColor.YELLOW);

    }

    public static Component grey(String text) {

        return Component.text(text, NamedTextColor.GRAY);

    }

}
