/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import net.minecraftforge.actionable.Main;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.Label;
import net.minecraftforge.actionable.util.config.RepoConfig;
import net.minecraftforge.actionable.util.enums.MergeableState;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GHPullRequestReviewState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PushHandler implements EventHandler {
    private static final long PR_BASE_TIME = 3;
    private static final ScheduledThreadPoolExecutor SERVICE = new ScheduledThreadPoolExecutor(1, run -> new Thread(run, "Conflict Checker")); // Non - daemon

    static {
        SERVICE.setKeepAliveTime(2, TimeUnit.SECONDS);
        SERVICE.allowCoreThreadTimeOut(true);
    }

    @Override
    public void handle(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable {
        final String branch = getBranchName(GithubVars.REF.get());
        if (branch == null) return; // Only check for commits pushed to branches

        final GitHub gh = gitHubGetter.get();
        final ObjectReader reader = GitHubAccessor.objectReader(gh);
        final GHRepository repository = reader.forType(GHRepository.class).readValue(payload.get("repository"));
        final List<GHWorkflowRun> inProgress = repository
                .queryWorkflowRuns()
                .branch(branch)
                .event(GHEvent.PUSH)
                .status(GHWorkflowRun.Status.IN_PROGRESS)
                .list().iterator().nextPage();

        if (inProgress.stream().anyMatch(it -> it.getName().equals(GithubVars.WORKFLOW.get()))) return; // Early exit if there's another push event in progress
        checkPRConflicts(gh, repository, branch);
    }

    public static void checkPRConflicts(GitHub gitHub, GHRepository repository, String branchName) throws IOException {
        final JsonNode json = GitHubAccessor.graphQl(gitHub, """
                query {
                  repository(owner: "%s", name: "%s") {
                    pullRequests(first: 100, after: null, states: OPEN, baseRefName: "%s") {
                      nodes {
                        mergeable
                        number
                        permalink
                        title
                        updatedAt
                        labels(first: 100) {
                          nodes {
                            name
                          }
                        }
                      }
                      pageInfo {
                        endCursor
                        hasNextPage
                      }
                    }
                  }
                }""", repository.getOwnerName(), repository.getName(), branchName);

        // TODO handle pagination in the future
        final JsonNode nodes = Jsons.at(json, "data.repository.pullRequests.nodes");
        final long unknownAmount = Jsons.stream(nodes).filter(it -> MergeableState.valueOf(it.get("mergeable").asText()) == MergeableState.UNKNOWN).count();
        if (unknownAmount > 0) {
            // If we don't know the status of one or more PRs, give GitHub some time to think.
            SERVICE.schedule(() -> {
                try {
                    checkPRConflicts(gitHub, repository, branchName);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }, PR_BASE_TIME * unknownAmount, TimeUnit.SECONDS);
        }

        final FunctionalInterfaces.SupplierException<Set<GHUser>> triagers = FunctionalInterfaces.memoize(() ->
                RepoConfig.INSTANCE.triage() == null ? Set.of() : gitHub.getOrganization(repository.getOwnerName())
                    .getTeamBySlug(RepoConfig.INSTANCE.triage().teamName())
                    .getMembers());
        for (int i = 0; i < nodes.size(); i++) {
            checkConflict(triagers, gitHub, nodes.get(i));
        }
    }

    @Nullable
    private static String getBranchName(String ref) {
        if (ref.startsWith("refs/heads/")) {
            return ref.substring("refs/heads/".length());
        }
        return null;
    }

    public static void checkConflict(FunctionalInterfaces.SupplierException<Set<GHUser>> triagers, GitHub gitHub, JsonNode node) throws IOException {
        final boolean hasLabel = Jsons.stream(Jsons.at(node, "labels.nodes")).anyMatch(it -> it.get("name").asText().equalsIgnoreCase(Label.NEEDS_REBASE.getLabelName()));
        System.out.println("Has label: " + hasLabel);
        final MergeableState state = MergeableState.valueOf(node.get("mergeable").asText());
        System.out.println("Has state: " + state);
        if (hasLabel && state == MergeableState.CONFLICTING) return; // We have conflicts and the PR has the label already

        final int number = node.get("number").intValue();
        final String permalink = node.get("permalink").asText();
        final GHRepository repo = gitHub.getRepository(permalink.substring("https://github.com/".length(), permalink.indexOf("/pull/")));
        final GHPullRequest pr = repo.getPullRequest(number);
        if (hasLabel && state == MergeableState.MERGEABLE) {
            // We don't have conflicts but the PR has the label... remove it.
            Label.NEEDS_REBASE.removeAndIgnore(pr);

            final List<GHUser> toRequest = new ArrayList<>();
            for (final GHPullRequestReview review : pr.listReviews()) {
                if (triagers.get().contains(review.getUser()) && review.getState() == GHPullRequestReviewState.APPROVED) {
                    toRequest.add(review.getUser());
                }
            }
            if (!toRequest.isEmpty()) pr.requestReviewers(toRequest);
        } else if (state == MergeableState.CONFLICTING && pr.getLabels().stream().noneMatch(it -> it.getName().equalsIgnoreCase(Label.NEEDS_REBASE.getLabelName()))) {
            // We have conflicts but the PR doesn't have the label... add it.
            Label.NEEDS_REBASE.add(pr);

            // Clear assignees
            pr.setAssignees(List.of());

            // And remove the assigned label
            Label.ASSIGNED.removeAndIgnore(pr);

            pr.comment("@%s, this pull request has conflicts, please resolve them for this PR to move forward.".formatted(pr.getUser().getLogin()));
        }
    }
}
