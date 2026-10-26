package ru.nothingflare.simplechat.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PATTERN_3 = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
    private static final boolean SUPPORTS_HEX;

    static {
        boolean hex = false;
        try {
            Class<?> chatColorClass = Class.forName("net.md_5.bungee.api.ChatColor");
            chatColorClass.getMethod("of", String.class);
            hex = true;
        } catch (Throwable ignored) {
            hex = false;
        }
        SUPPORTS_HEX = hex;
    }

    private ColorUtil() {}

    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        if (message.indexOf('#') != -1) {
            if (SUPPORTS_HEX) {
                if (message.contains("&#")) {
                    message = applyHex(message, HEX_PATTERN_1);
                }
                if (message.contains("<#")) {
                    message = applyHex(message, HEX_PATTERN_2);
                }
                if (message.contains("{#")) {
                    message = applyHex(message, HEX_PATTERN_3);
                }
            } else {
                if (message.contains("&#")) {
                    message = HEX_PATTERN_1.matcher(message).replaceAll("");
                }
                if (message.contains("<#")) {
                    message = HEX_PATTERN_2.matcher(message).replaceAll("");
                }
                if (message.contains("{#")) {
                    message = HEX_PATTERN_3.matcher(message).replaceAll("");
                }
            }
        }

        if (message.indexOf('&') != -1) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        return message;
    }

    public static List<String> color(List<String> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<String> colored = new ArrayList<>(list.size());
        for (String s : list) {
            colored.add(color(s));
        }
        return colored;
    }

    private static String applyHex(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        StringBuilder sb = new StringBuilder(message.length());
        while (matcher.find()) {
            String hex = matcher.group(1);
            try {
                net.md_5.bungee.api.ChatColor hexColor = net.md_5.bungee.api.ChatColor.of("#" + hex);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(hexColor.toString()));
            } catch (Throwable ignored) {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
