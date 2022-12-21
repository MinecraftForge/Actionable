package net.minecraftforge.actionable.util;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GithubVars {

    public static final Var<String> REPOSITORY = var(Type.GITHUB, "REPOSITORY");
    public static final Var<String> REPOSITORY_OWNER = var(Type.GITHUB, "REPOSITORY_OWNER");
    public static final Var<String> EVENT_PATH = var(Type.GITHUB, "EVENT_PATH");
    public static final Var<String> REF = var(Type.GITHUB, "REF");
    public static final Var<GithubEvent> EVENT = new Var<>(Type.GITHUB, "EVENT_NAME", it -> GithubEvent.BY_ID.getOrDefault(it, GithubEvent.UNKNOWN));

    public static final Var<String> GH_APP_KEY = var(Type.INPUT, "GH_APP_KEY");
    public static final Var<String> GH_APP_NAME = var(Type.INPUT, "GH_APP_NAME");
    public static final Var<String> TRIAGE_TEAM = var(Type.INPUT, "TRIAGE_TEAM");
    public static final Var<Integer> TRIAGE_PROJECT = new Var<>(Type.INPUT, "TRIAGE_PROJECT", Integer::parseInt);
    public static final Var<Boolean> ALLOW_COMMANDS_IN_EDITS = new Var<>(Type.INPUT, "ALLOW_COMMANDS_IN_EDITS", Boolean::parseBoolean);
    public static final Var<Set<String>> COMMAND_PREFIXES = new Var<>(Type.INPUT, "COMMAND_PREFIXES", it -> Stream.of(it.split(","))
            .map(s -> s.replace("<ws>", " ")).collect(Collectors.toSet()));

    private static Var<String> var(Type type, String key) {
        return new Var<>(type, key, Function.identity());
    }

    public static final class Var<Z> implements Supplier<Z> {
        private Z value;
        private final String key;
        private final Function<String, Z> mapper;
        private Var(Type type, String key, Function<String, Z> mapper) {
            this.key = type + "_" + key;
            this.mapper = mapper;
        }

        public Z get() {
            if (value == null) value = mapper.apply(System.getenv(key));
            return value;
        }
    }

    public enum Type {
        INPUT,
        GITHUB
    }
}
