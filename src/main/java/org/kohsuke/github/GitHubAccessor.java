package org.kohsuke.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.enums.ReportedContentClassifiers;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GitHubAccessor {
    private static final Map<GHRepository, Set<String>> EXISTING_LABELS = new ConcurrentHashMap<>();

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

    public static void lock(GHIssue issue, LockReason reason) throws IOException {
        issue.root().createRequest().method("PUT").withUrlPath(issue.getIssuesApiRoute() + "/lock")
                .inBody().with("lock_reason", reason.toString()).send();
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

    public static void minimize(GHIssueComment comment, ReportedContentClassifiers reason) throws IOException {
        graphQl(comment.root(), """
             mutation {
                minimizeComment(input: {classifier: %s, subjectId: "%s"}) {
                  minimizedComment {
                    isMinimized
                  }
                }
              }
                """.formatted(
                reason.name(), comment.getNodeId()
        ));
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
