/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package org.kohsuke.github;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.api.MinimizeCommentMutation;
import com.github.api.type.CustomType;
import com.github.api.type.ReportedContentClassifiers;
import net.minecraftforge.actionable.util.ApolloReader;
import org.apache.commons.io.input.ReaderInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.function.InputStreamFunction;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GitHubAccessor {
    private static final Map<GHRepository, Set<String>> EXISTING_LABELS = new ConcurrentHashMap<>();
    @SuppressWarnings("DataFlowIssue")
    public static final ApolloClient CLIENT = ApolloClient.builder()
            .serverUrl("https://api.github.com/graphql")
            .callFactory(request -> null)
            .addCustomTypeAdapter(CustomType.URI, new CustomTypeAdapter<URI>() {

                @NotNull
                @Override
                public CustomTypeValue<?> encode(URI uri) {
                    return new CustomTypeValue.GraphQLString(uri.toString());
                }

                @Override
                public URI decode(@NotNull CustomTypeValue<?> customTypeValue) {
                    return URI.create(customTypeValue.value.toString());
                }
            }).build();
    public static final ApolloReader READER = ApolloReader.ofClient(CLIENT);

    public static ObjectReader objectReader(GitHub gitHub) {
        return GitHubClient.getMappingObjectReader(gitHub);
    }

    public static void wrapUp(GHIssueComment comment, GHIssue owner) {
        comment.wrapUp(owner);
    }

    public static void wrapUp(GHPullRequest pr, GHRepository repository) {
        pr.wrapUp(repository);
    }

    public static void wrapUp(GHIssue issue, GHRepository repository) {
        issue.wrap(repository);
    }

    public static void lock(GHIssue issue, @Nullable LockReason reason) throws IOException {
        if (reason == null) {
            issue.lock();
        } else {
            issue.root().createRequest().method("PUT").withUrlPath(issue.getIssuesApiRoute() + "/lock")
                    .inBody().with("lock_reason", reason.toString()).send();
        }
    }

    public static void merge(GHPullRequest pr, String title, String message, GHPullRequest.MergeMethod method) throws IOException {
        pr.root().createRequest()
                .method("PUT")
                .with("commit_message", message == null ? "" : message)
                .with("commit_title", title)
                .with("sha", pr.getHead().getSha())
                .with("merge_method", method)
                .withUrlPath(pr.getApiRoute() + "/merge")
                .send();
    }

    public static JsonNode graphQl(GitHub gitHub, String query, Object... args) throws IOException {
        return gitHub.createRequest()
                .method("POST")
                .inBody()
                .with("query", query.formatted(args))
                .withUrlPath("/graphql")
                .fetch(JsonNode.class);
    }

    public static <T> T graphQl(GitHub gitHub, String query, InputStreamFunction<T> isF) throws IOException {
        return gitHub.createRequest()
                .method("POST")
                .inBody()
                .with(new ReaderInputStream(new StringReader(query), StandardCharsets.UTF_8))
                .withUrlPath("/graphql")
                .fetchStream(isF);
    }

    @SuppressWarnings("unchecked")
    public static <T> T graphQl(GitHub gitHub, Operation<?, T, ?> call) throws IOException {
        final Response<T> response = graphQl(gitHub, call.composeRequestBody().utf8(), in -> (Response<T>) READER.read(call, in));
        final T res = response.getData();
        if (res == null) {
            throw new ApolloException(Objects.<List<Error>>requireNonNullElse(response.getErrors(), List.of()).stream()
                    .map(Error::toString).collect(Collectors.joining("; ")));
        } else {
            return res;
        }
    }

    public static GHArtifact[] getArtifacts(GitHub gitHub, String repo, long runId) throws IOException {
        return gitHub.createRequest()
                .withUrlPath("/repos/" + repo + "/actions/runs/" + runId, "artifacts")
                .fetchStream(input -> {
                    final ObjectReader mapper = objectReader(gitHub);
                    final JsonNode node = mapper.readTree(input);
                    input.close();
                    return mapper.readValue(node.get("artifacts"), GHArtifact[].class);
                });
    }

    public static IssueEdit edit(GHIssue issue) {
        final Requester request = issue.root().createRequest().method("PATCH")
                .inBody().withUrlPath(issue.getApiRoute());
        return new IssueEdit() {
            @Override
            public IssueEdit edit(String key, Object value) {
                request.with(key, value);
                return this;
            }

            @Override
            public void send() throws IOException {
                request.send();
            }
        };
    }


    public static void minimize(GHIssueComment comment, ReportedContentClassifiers classifiers) throws IOException {
        graphQl(comment.root(), new MinimizeCommentMutation(classifiers, comment.getNodeId()));
    }

    public static void removeLabel(GHIssue issue, String label) throws IOException {
        if (issue.getLabels().stream().anyMatch(it -> it.getName().equalsIgnoreCase(label))) {
            issue.removeLabel(label);
        }
    }

    public static void addLabel(GHIssue issue, String label) throws IOException {
        if (issue.getLabels().stream().noneMatch(it -> it.getName().equalsIgnoreCase(label)) &&
                getExistingLabels(issue.getRepository()).stream().anyMatch(it -> it.equalsIgnoreCase(label))) {
            issue.addLabels(label);
        }
    }

    public static Set<String> getExistingLabels(GHRepository repository) throws IOException {
        Set<String> ex = EXISTING_LABELS.get(repository);
        if (ex != null) return ex;
        ex = repository.listLabels().toList().stream().map(GHLabel::getName).collect(Collectors.toSet());
        EXISTING_LABELS.put(repository, ex);
        return ex;
    }

    public static String getDiff(GHPullRequest pr) throws IOException {
        return pr.root()
                .createRequest()
                .withUrlPath(pr.getApiRoute())
                .withHeader("Accept", "application/vnd.github.v3.diff")
                .fetchStream(input -> new String(input.readAllBytes()));
    }

    public interface IssueEdit {
        IssueEdit edit(String key, Object value);

        void send() throws IOException;
    }
}
