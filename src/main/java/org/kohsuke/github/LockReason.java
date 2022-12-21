package org.kohsuke.github;

import java.util.Locale;

public enum LockReason {
    OFF_TOPIC {
        @Override
        public String toString() {
            return "off-topic";
        }
    },
    TOO_HEATED {
        @Override
        public String toString() {
            return "too heated";
        }
    },
    RESOLVED,
    SPAM;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ROOT);
    }
}
