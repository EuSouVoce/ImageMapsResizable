package net.craftcitizen.imagemaps.clcore.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class MessageUtil {
    static final private MessageSettings NULL_SETTINGS = new MessageSettings();
    static final private Map<String, MessageSettings> settings = new HashMap<String, MessageSettings>();

    private MessageUtil() {}

    public static void registerPlugin(final Plugin plugin, final BaseComponent prefix, final ChatColor normalColor,
            final ChatColor infoColor, final ChatColor warningColor, final ChatColor errorColor, final ChatColor debugColor) {
        MessageUtil.settings.put(plugin.getName(),
                new MessageSettings(prefix, normalColor, infoColor, warningColor, errorColor, debugColor));
    }

    @SuppressWarnings("deprecation")
    public static void sendMessage(final Plugin plugin, final CommandSender sender, final MessageLevel level, final String message) {
        sender.sendMessage(MessageUtil.settings.getOrDefault(plugin != null ? plugin.getName() : "", MessageUtil.NULL_SETTINGS)
                .formatMessage(level, message));
    }

    @SuppressWarnings("deprecation")
    public static void sendMessage(final Plugin plugin, final CommandSender sender, final MessageLevel level, final BaseComponent message) {
        sender.sendMessage(MessageUtil.settings.getOrDefault(plugin != null ? plugin.getName() : "", MessageUtil.NULL_SETTINGS)
                .formatMessage(level, message));
    }

    private static class MessageSettings {
        private final BaseComponent prefix;
        private final Map<MessageLevel, ChatColor> levelColors = new EnumMap<MessageLevel, ChatColor>(MessageLevel.class);

        MessageSettings() { this.prefix = new TextComponent(); }

        public MessageSettings(final BaseComponent prefix, final ChatColor normalColor, final ChatColor infoColor,
                final ChatColor warningColor, final ChatColor errorColor, final ChatColor debugColor) {
            this.prefix = prefix;
            this.levelColors.put(MessageLevel.NORMAL, normalColor);
            this.levelColors.put(MessageLevel.INFO, infoColor);
            this.levelColors.put(MessageLevel.WARNING, warningColor);
            this.levelColors.put(MessageLevel.ERROR, errorColor);
            this.levelColors.put(MessageLevel.DEBUG, debugColor);
        }

        public BaseComponent formatMessage(final MessageLevel level, final String message) {
            return this.formatMessage(level, (BaseComponent) new TextComponent(message));
        }

        public BaseComponent formatMessage(final MessageLevel level, final BaseComponent message) {
            final ChatColor levelColor = this.levelColors.get((Object) level);
            final TextComponent base = new TextComponent();
            if (levelColor != null) {
                base.setColor(levelColor);
            }
            base.addExtra(this.prefix);
            base.addExtra(" ");
            base.addExtra(message);
            return base;
        }
    }
}
