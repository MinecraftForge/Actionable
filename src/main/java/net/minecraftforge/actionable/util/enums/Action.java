package net.minecraftforge.actionable.util.enums;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;

public enum Action {
    OPENED,
    CREATED,
    EDITED,
    SYNCHRONIZE;

    public static Action get(JsonNode payload) {
        return valueOf(payload.get("action").asText().toUpperCase(Locale.ROOT));
    }
}
