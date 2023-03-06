/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.util.config;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import net.minecraftforge.actionable.util.Label;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record RepoConfig(
    FeaturePredicate features,
    Map<String, String> labels,
    Map<String, LabelLock> labelLocks,

    @Nullable Triage triage,
    LinkedHashMap<String, TeamLike> labelTeams,

    @Nullable Commands commands
) {
    public static final RepoConfig DEFAULT = new RepoConfig(
            FeaturePredicate.DEFAULT,
            Stream.of(Label.values()).collect(Collectors.toMap(a -> a.id, a -> a.defaultName)),
            Map.of(
                    "Forum", new LabelLock(true, null, true, """
                    :wave: We use the issue tracker exclusively for final bug reports and feature requests.  
                    However, this issue appears to be better suited for the [Forge Support Forums](https://forums.minecraftforge.net/) or [Forge Discord](https://discord.gg/UvedJ9m).  
                    Please create a new topic on the support forum with this issue or ask in the `#tech-support` channel in the Discord server, and the conversation can continue there.""".trim()),
                    "Spam", new LabelLock(true, "spam", true, null)
            ),
            new Triage("triage", 4),
            new LinkedHashMap<>(),

            new Commands(List.of("/"), false, true, true)
    );
    public static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));

    public static RepoConfig INSTANCE;

    public static RepoConfig getOrCommit(GHRepository repository, String path, String branch, String targetRepo) throws IOException {
        path = path + "/" + targetRepo + ".yaml";
        try (final var is = repository.getFileContent(path, branch).read()) {
            return MAPPER.readValue(is, RepoConfig.class);
        } catch (IOException ignored) {} // The file probably doesn't exist or is not reachable
        System.out.printf("Could not find config in repository %s, for repository %s, at path %s@%s.\n", repository.getFullName(), targetRepo, path, branch);
        writeConfig(DEFAULT, repository, path, branch, targetRepo);
        return DEFAULT;
    }

    public static void writeConfig(RepoConfig config, GHRepository repository, String path, String branch, String target) throws IOException {
        repository.createContent()
                .path(path)
                .branch(branch)
                .content(MAPPER.writeValueAsBytes(config))
                .message("Create configuration for repository " + target)
                .commit();
    }

    public RepoConfig sanitize() {
        return new RepoConfig(
                Objects.requireNonNullElse(features, FeaturePredicate.DEFAULT),
                Objects.requireNonNullElse(labels, Map.of()),
                Objects.requireNonNullElse(labelLocks, Map.of()),
                triage(),
                Objects.requireNonNullElseGet(labelTeams, LinkedHashMap::new),
                commands()
        );
    }

    public boolean featureEnabled(String featureName) {
        return features.test(featureName);
    }

    public record LabelLock(
            boolean lock,
            @Nullable String lockReason,
            boolean close,
            @Nullable String message
    ) {}

    public record Triage(
            String teamName,
            int projectId
    ) {}

    public record Commands(
            List<String> prefixes,
            boolean allowEdits,
            boolean minimizeComment,
            boolean reactToComment
    ) {}

    public record ConfigLocation(String repository, String directory, String branch) {}

    @JsonSerialize(using = TeamLike.Serializer.class)
    @JsonDeserialize(using = TeamLike.Deserializer.class)
    public sealed interface TeamLike {
        record Team(String team) implements TeamLike {

            @Override
            public Set<GHUser> getUsers(GitHub gitHub, GHOrganization organization) throws IOException {
                return organization.getTeamBySlug(team).getMembers();
            }

            @Override
            public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(team);
            }
        }

        record Users(List<String> users) implements TeamLike {

            @Override
            public Set<GHUser> getUsers(GitHub gitHub, GHOrganization organization) throws IOException {
                final Set<GHUser> users = new HashSet<>();
                for (final String name : this.users) users.add(gitHub.getUser(name));
                return users;
            }

            @Override
            public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeArray(users.toArray(String[]::new), 0, users.size());
            }
        }

        Set<GHUser> getUsers(GitHub gitHub, GHOrganization organization) throws IOException;

        void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException;

        final class Serializer extends JsonSerializer<TeamLike> {

            @Override
            public void serialize(TeamLike value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                value.serialize(gen, serializers);
            }
        }

        final class Deserializer extends JsonDeserializer<TeamLike> {
            @Override
            public TeamLike deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                final JsonNode node = p.readValueAsTree();
                if (node == null) return null;
                if (node instanceof ArrayNode arrayNode) {
                    return new Users(StreamSupport.stream(arrayNode.spliterator(), false).map(JsonNode::asText).toList());
                }
                return new Team(node.asText());
            }
        }
    }
}
