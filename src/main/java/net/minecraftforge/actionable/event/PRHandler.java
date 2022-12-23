package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import net.minecraftforge.actionable.util.DiffUtils;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.Label;
import net.minecraftforge.actionable.util.enums.Action;
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
import java.util.Set;

public class PRHandler extends ByActionEventHandler<PRHandler.Payload> {

    public PRHandler() {
        super(
                Payload.class, payload -> GitHubAccessor.wrapUp(payload.pull_request, payload.repository),
                registrar -> registrar
                        .register(Action.OPENED, PRHandler::onCreate)
                        .register(Action.SYNCHRONIZE, PRHandler::onSync)
                        .register(Action.READY_FOR_REVIEW, (gitHub, payload, payloadJson) -> {
                            final GHTeam team = payload.organization.getTeamBySlug(GithubVars.TRIAGE_TEAM.get());
                            if (!payload.pull_request.getRequestedTeams().contains(team)) {
                                payload.pull_request.requestTeamReviewers(List.of(team));
                            }
                        })
                        .register(Action.LABELED, IssueHandler::onSpamLabel)
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
        final FunctionalInterfaces.SupplierException<Set<GHUser>> triagers = FunctionalInterfaces.memoize(() -> payload.organization
                .getTeamBySlug(GithubVars.TRIAGE_TEAM.get())
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
            final String prTitle = pullRequest.getTitle();
            if (prTitle.startsWith("[") && prTitle.contains("]")) {
                final String[] prFullVersion = prTitle.substring(1, prTitle.indexOf("]")).split("\\.");
                if (prFullVersion.length < 2) return;
                final String prVersion = prFullVersion[0] + "." + prFullVersion[1];
                GitHubAccessor.addLabel(pullRequest, prVersion);

                if (!GithubVars.LATEST_VERSION.get().isBlank() && !GithubVars.LATEST_VERSION.get().equals(prVersion)) {
                    Label.LTS_BACKPORT.add(pullRequest);
                }
            }
        });
        steps.add(() -> {
            final List<String> newFiles = DiffUtils.detectNewFiles(GitHubAccessor.getDiff(pullRequest).split("\n"));

            if (newFiles.stream().anyMatch(it -> it.contains("/event/") /* Check for new files in an event package */)) {
                Label.FEATURE.addAndIgnore(pullRequest);
                Label.NEW_EVENT.addAndIgnore(pullRequest);
            }
            if (newFiles.stream().anyMatch(it -> it.contains("/client/") /* Check for new files in a client package in order to add the Rendering label - this isn't perfect, but it works */)) {
                Label.RENDERING.addAndIgnore(pullRequest);
            }
        });

        if (!pullRequest.isDraft()) steps.add(() -> pullRequest.requestTeamReviewers(List.of(
                organization.getTeamBySlug(GithubVars.TRIAGE_TEAM.get())
        )));

        steps.add(() -> addToProject(gitHub, organization, GithubVars.TRIAGE_PROJECT.get(), pullRequest));

        steps.forEach(runnable -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                System.err.println("Encountered exception running PR create action step: " + exception);
                exception.printStackTrace();
            }
        });
    }

    private static void addToProject(GitHub gitHub, GHOrganization organization, int projectURL, GHPullRequest pullRequest) throws IOException {
        final JsonNode idQuery = GitHubAccessor.graphQl(gitHub, """
                query{
                    organization(login: "%s"){
                      projectV2(number: %s) {
                        id
                      }
                    }
                  }""".formatted(
                organization.getLogin(),
                projectURL
        ));
        final String projectId = Jsons.at(idQuery, "data.organization.projectV2.id").asText();
        GitHubAccessor.graphQl(gitHub, """
            mutation {
            addProjectV2ItemById(input: {projectId: "%s" contentId: "%s"}) {
                item {
                  id
                }
              }
            }""",
            projectId, pullRequest.getNodeId()
        );
    }
}
