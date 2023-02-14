/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import net.minecraftforge.actionable.util.DiffUtils;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.Label;
import net.minecraftforge.actionable.util.RepoConfig;
import net.minecraftforge.actionable.util.enums.Action;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PRHandler extends ByActionEventHandler<PRHandler.Payload> {

    public PRHandler() {
        super(
                Payload.class, payload -> GitHubAccessor.wrapUp(payload.pull_request, payload.repository),
                registrar -> registrar
                        .register(Action.OPENED, PRHandler::onCreate)
                        .register(Action.SYNCHRONIZE, PRHandler::onSync)
                        .register(Action.READY_FOR_REVIEW, (gitHub, payload, payloadJson) -> {
                            if (RepoConfig.INSTANCE.triage() == null) return;
                            final GHTeam team = payload.organization.getTeamBySlug(RepoConfig.INSTANCE.triage().teamName());
                            if (!payload.pull_request.getRequestedTeams().contains(team)) {
                                payload.pull_request.requestTeamReviewers(List.of(team));
                            }
                        })

                        .register(Action.LABELED, IssueHandler::onLabelLock)
                        .register(Action.UNLABELED, IssueHandler::onLabelLockRemove)
        );
    }

    public record Payload(GHPullRequest pull_request, GHRepository repository, GHOrganization organization) implements IssueHandler.IssuePayload {
        @Override
        public GHIssue issue() {
            return pull_request;
        }
    }

    private static void onSync(GitHub gitHub, Payload payload, JsonNode $) throws IOException {
        final JsonNode queryJson = GitHubAccessor.graphQl(gitHub, """
                query {
                  repository(owner: "%s", name: "%s") {
                    pullRequest(number: %s) {
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
                  }
                }""", payload.repository.getOwnerName(), payload.repository.getName(), payload.pull_request.getNumber());

        final JsonNode json = Jsons.at(queryJson, "data.repository.pullRequest");
        final FunctionalInterfaces.SupplierException<Set<GHUser>> triagers = FunctionalInterfaces.memoize(() ->
                RepoConfig.INSTANCE.triage() == null ? Set.of() : payload.organization
                    .getTeamBySlug(RepoConfig.INSTANCE.triage().teamName())
                    .getMembers());
        PushHandler.checkConflict(triagers, gitHub, json);
    }

    private static void onCreate(GitHub gitHub, Payload payload, JsonNode $) throws IOException {
        final GHOrganization organization = payload.organization;
        final GHPullRequest pullRequest = payload.pull_request;

        // We split into steps to not crash if someone does one of the steps manually
        final List<FunctionalInterfaces.RunnableException> steps = new ArrayList<>();

        steps.add(() -> Label.TRIAGE.add(pullRequest));
        steps.add(() -> {
            final String prVersion = getPRVersion(pullRequest);
            if (prVersion != null) {
                final String latestLabel = RepoConfig.INSTANCE.labels().get(Label.LATEST.getId());
                if (latestLabel != null) {
                    if (!latestLabel.equals(prVersion)) {
                        Label.LTS_BACKPORT.addAndIgnore(pullRequest);
                    }
                } else if (payload.repository.getDefaultBranch() != null) {
                    // If there's no latest label configured, assume that everything which ISN'T targeted to the default branch is an LTS Backport
                    final String latest = getMajorVersionFrom(payload.repository.getDefaultBranch());
                    if (latest != null && !latest.equals(prVersion)) {
                        Label.LTS_BACKPORT.addAndIgnore(pullRequest);
                    }
                }
                GitHubAccessor.addLabel(pullRequest, prVersion);
            }
        });

        steps.add(() -> {
            final String between = getBetweenAtStart(pullRequest.getTitle(), '[', ']');
            if (between != null && between.toLowerCase(Locale.ROOT).contains("rfc")) {
                Label.RFC.add(pullRequest);
            }
        });

        steps.add(() -> {
            final List<String> newFiles = DiffUtils.detectNewFiles(GitHubAccessor.getDiff(pullRequest).split("\n"));

            if (newFiles.stream().anyMatch(it -> it.contains("/event/") /* Check for new files in an event package */)) {
                Label.FEATURE.addAndIgnore(pullRequest);
                Label.NEW_EVENT.addAndIgnore(pullRequest);
            }

            /*
            Removed for now: not everything in the client package is related to rendering
            if (newFiles.stream().anyMatch(it -> it.contains("/client/"))) {
                Label.RENDERING.addAndIgnore(pullRequest);
            }
            */
        });

        if (RepoConfig.INSTANCE.triage() != null) {
            if (!pullRequest.isDraft()) steps.add(() -> pullRequest.requestTeamReviewers(List.of(
                    organization.getTeamBySlug(RepoConfig.INSTANCE.triage().teamName())
            )));

            steps.add(() -> addToProject(gitHub, organization, RepoConfig.INSTANCE.triage().projectId(), pullRequest));
        }

        steps.forEach(runnable -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                System.err.println("Encountered exception running PR create action step: " + exception);
                exception.printStackTrace();
            }
        });
    }

    static String getProjectId(GitHub gitHub, GHOrganization organization, int projectId) throws IOException {
        final JsonNode idQuery = GitHubAccessor.graphQl(gitHub, """
                query{
                    organization(login: "%s"){
                      projectV2(number: %s) {
                        id
                      }
                    }
                  }""".formatted(
                organization.getLogin(),
                projectId
        ));
        return Jsons.at(idQuery, "data.organization.projectV2.id").asText();
    }

    private static void addToProject(GitHub gitHub, GHOrganization organization, int projectURL, GHPullRequest pullRequest) throws IOException {
        GitHubAccessor.graphQl(gitHub, """
            mutation {
            addProjectV2ItemById(input: {projectId: "%s" contentId: "%s"}) {
                item {
                  id
                }
              }
            }""",
            getProjectId(gitHub, organization, projectURL), pullRequest.getNodeId()
        );
    }

    @Nullable
    private static String getPRVersion(GHPullRequest pullRequest) {
        final String prTitle = pullRequest.getTitle();
        final String[] prFullVersion;
        if (prTitle.startsWith("[") && prTitle.contains("]")) {
            prFullVersion = prTitle.substring(1, prTitle.indexOf("]")).split("\\.");
        } else {
            prFullVersion = pullRequest.getBase().getRef().split("\\.");
        }
        if (prFullVersion.length >= 2) {
            return prFullVersion[0] + "." + prFullVersion[1];
        }
        return null;
    }

    @Nullable
    private static String getMajorVersionFrom(String version) {
        final String[] spl = version.split("\\.");
        if (spl.length >= 2) return spl[0] + spl[1];
        return null;
    }

    @Nullable
    private static String getBetweenAtStart(String str, char start, char end) {
        if (!str.startsWith(Character.toString(start))) return null;
        final int endIdx = str.indexOf(end);
        if (endIdx < 0) return null;
        return str.substring(1, endIdx);
    }
}
