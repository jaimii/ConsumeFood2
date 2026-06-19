package me.msicraft.consumefood2.Utils;

import me.msicraft.API.Common;
import me.msicraft.consumefood2.ConsumeFood2;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtil {

    public enum FoodType {
        VANILLAFOOD, CUSTOMFOOD
    }

    private MessageUtil() {
    }

    public static String getConfigMessage(String path, boolean applyColorCodes) {
        if (!applyColorCodes) {
            return ConsumeFood2.getPlugin().getMessageData().getConfig().getString(path, null);
        }
        String message = ConsumeFood2.getPlugin().getMessageData().getConfig().getString(path, null);
        if (message != null) {
            return Common.getInstance().translateColorCodes(message);
        }
        return null;
    }

    public static void sendMessage(CommandSender sender, String messagePath) {
        String message = getConfigMessage(messagePath, true);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
        }
    }

    public static void sendErrorMessage(FoodType foodType, String errorType, String invalidKey, String... extraMessage) {
        Component prefix = ConsumeFood2.PREFIX;
        switch (foodType) {
            case VANILLAFOOD -> {
                Bukkit.getConsoleSender().sendMessage(prefix.append(Component.text("=====VanillaFood " + errorType + "=====", NamedTextColor.YELLOW)));
                Bukkit.getConsoleSender().sendMessage(prefix.append(Component.text("Invalid Material: " + invalidKey, NamedTextColor.YELLOW)));
                for (String em : extraMessage) {
                    Bukkit.getConsoleSender().sendMessage(prefix.append(Component.text(em, NamedTextColor.YELLOW)));
                }
            }
            case CUSTOMFOOD -> {
                Bukkit.getConsoleSender().sendMessage(prefix.append(Component.text("=====CustomFood " + errorType + "=====", NamedTextColor.YELLOW)));
                Bukkit.getConsoleSender().sendMessage(prefix.append(Component.text("Invalid InternalName: " + invalidKey, NamedTextColor.YELLOW)));
                for (String em : extraMessage) {
                    Bukkit.getConsoleSender().sendMessage(prefix.append(Component.text(em, NamedTextColor.YELLOW)));
                }
            }
        }
    }

}