/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.util.enums;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;

public enum Action {
    OPENED,
    CREATED,
    REOPENED,
    CLOSED,

    EDITED,
    SUBMITTED,
    READY_FOR_REVIEW,
    SYNCHRONIZE,
    LABELED,
    UNLABELED,

    UNKNOWN;

    public static Action get(JsonNode payload) {
        try {
            return valueOf(payload.get("action").asText().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
