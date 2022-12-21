package net.minecraftforge.actionable.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Jsons {
    public static JsonNode at(JsonNode node, String path) {
        final String[] paths = path.split("\\.");
        for (final String s : paths) {
            node = node.get(s);
        }
        return node;
    }

    public static Stream<JsonNode> stream(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false);
    }
}
