/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package org.kohsuke.github;

import net.minecraftforge.actionable.annotation.Value;

import java.util.Locale;

public enum LockReason {
    @Value(names = {"off-topic", "off topic"}, description = "Lock as off-topic")
    OFF_TOPIC {
        @Override
        public String toString() {
            return "off-topic";
        }
    },

    @Value(names = {"too-heated", "too heated"}, description = "Lock as too-heated")
    TOO_HEATED {
        @Override
        public String toString() {
            return "too heated";
        }
    },

    @Value(names = "resolved", description = "Lock as resolved")
    RESOLVED,

    @Value(names = "spam", description = "Lock as spam")
    SPAM;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ROOT);
    }
}
