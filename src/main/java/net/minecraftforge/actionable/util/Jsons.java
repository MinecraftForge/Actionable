package net.minecraftforge.actionable.util;

import com.fasterxml.jackson.databind.JsonNode;

public class Jsons {
    public static JsonNode at(JsonNode node, String path) {
        final String[] paths = path.split("\\.");
        for (final String s : paths) {
            node = node.get(s);
        }
        return node;
    }
}
