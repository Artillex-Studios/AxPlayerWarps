package com.artillexstudios.axplayerwarps.utils;

import java.util.List;

public class SimpleRegex {

    public static boolean matches(List<String> list, String cmd) {
        for (String string : list) {
            if (string.isBlank()) continue;
            RegexType regexType = RegexType.EQUALS;
            boolean starts = string.charAt(0) == '*';
            boolean ends = string.charAt(string.length() - 1) == '*';
            if (starts && ends) {
                string = string.substring(1, string.length() - 1);
                regexType = RegexType.CONTAINS;
            }
            else if (starts) {
                string = string.substring(1);
                regexType = RegexType.STARTS_WITH;
            }
            else if (ends) {
                string = string.substring(0, string.length() - 1);
                regexType = RegexType.ENDS_WITH;
            }

            boolean result = switch (regexType) {
                case CONTAINS -> cmd.contains(string);
                case STARTS_WITH -> cmd.endsWith(string);
                case ENDS_WITH -> cmd.startsWith(string);
                case EQUALS -> string.equals(cmd);
            };
            if (result) return true;
        }
        return false;
    }

    private enum RegexType {
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        EQUALS
    }
}
