/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import net.minecraftforge.actionable.Main;

public interface EventHandler {
    void handle(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable;
}
