package net.minecraftforge.actionable.util;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public static final Var<GitHubEvent> EVENT = new Var<>(Type.GITHUB, "EVENT_NAME", it -> GitHubEvent.BY_ID.getOrDefault(it, GitHubEvent.UNKNOWN));

    public static final Var<String> GH_APP_KEY = var(Type.INPUT, "GH_APP_KEY");
    public static final Var<String> GH_APP_NAME = var(Type.INPUT, "GH_APP_NAME");
    public static final Var<String> TRIAGE_TEAM = var(Type.INPUT, "TRIAGE_TEAM");
    public static final Var<String> LATEST_VERSION = var(Type.INPUT, "LATEST_VERSION");
    public static final Var<Integer> TRIAGE_PROJECT = new Var<>(Type.INPUT, "TRIAGE_PROJECT", Integer::parseInt);
    public static final Var<Boolean> ALLOW_COMMANDS_IN_EDITS = new Var<>(Type.INPUT, "ALLOW_COMMANDS_IN_EDITS", Boolean::parseBoolean);
    public static final Var<AssignTeams> LABELS_TO_TEAMS = new Var<>(Type.INPUT, "LABELS_TO_TEAMS", it -> {
        final String[] split = it.trim().split(",");
        final Map<String, AssignTeams.TeamLike> teams = Stream.of(split).map(s -> s.trim().split("->"))
                .collect(Collectors.toMap(ar -> ar[0].trim(), ar -> {
                    final String value = ar[1].trim();
                    if (value.startsWith("u:")) {
                        return AssignTeams.TeamLike.users(Stream.of(value.substring(2).split("\\+"))
                                .map(String::trim).toList());
                    }
                    return AssignTeams.TeamLike.team(value);
                }));
        return new AssignTeams(teams.get("default"), teams);
    });
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

    public record AssignTeams(TeamLike defaultTeam, Map<String, TeamLike> byLabel) {
        public interface TeamLike {
            Set<GHUser> getUsers(GitHub gitHub, GHOrganization organization) throws IOException;

            static TeamLike team(String name) {
                return (github, organization) -> organization.getTeamBySlug(name).getMembers();
            }

            static TeamLike users(List<String> names) {
                return (gitHub, organization) -> {
                    final Set<GHUser> users = new HashSet<>();
                    for (final String name : names) users.add(gitHub.getUser(name));
                    return users;
                };
            }
        }
    }
}
