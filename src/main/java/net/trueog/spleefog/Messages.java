package net.trueog.spleefog;

import org.bukkit.command.CommandSender;

public final class Messages {

    private static final String PREFIX = "&8[&bSpleef&8] &r";

    private Messages() {

    }

    public static void send(CommandSender sender, String message) {

        sender.sendMessage(SpleefConfig.color(PREFIX + message));

    }

}
