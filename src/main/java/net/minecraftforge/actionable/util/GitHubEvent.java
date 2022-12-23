package net.minecraftforge.actionable.util;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum GitHubEvent {
    ISSUE_COMMENT,
    PULL_REQUEST,
    PULL_REQUEST_REVIEW,
    PUSH,

    UNKNOWN;

    public static final Map<String, GitHubEvent> BY_ID = Stream.of(values())
            .collect(Collectors.toMap(it -> it.toString().toLowerCase(Locale.ROOT), Function.identity()));
}
