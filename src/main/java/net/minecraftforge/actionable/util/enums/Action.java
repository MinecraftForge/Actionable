package net.minecraftforge.actionable.util.enums;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;

public enum Action {
    OPENED,
    CREATED,
    EDITED,
    SUBMITTED,
    READY_FOR_REVIEW,
    SYNCHRONIZE,

    UNKNOWN;

    public static Action get(JsonNode payload) {
        try {
            return valueOf(payload.get("action").asText().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
