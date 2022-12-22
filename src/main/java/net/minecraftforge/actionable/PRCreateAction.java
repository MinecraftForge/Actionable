package net.minecraftforge.actionable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import net.minecraftforge.actionable.util.enums.Action;
import net.minecraftforge.actionable.util.DiffUtils;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.Jsons;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PRCreateAction {
    private static final Set<Action> HANDLED = EnumSet.of(Action.OPENED, Action.SYNCHRONIZE);

    public static void run(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable {
        final Action action = Action.get(payload);
        if (!HANDLED.contains(action)) return; // Only run on PR open

        final GitHub gitHub = gitHubGetter.get();
        final ObjectReader reader = GitHubAccessor.objectReader(gitHub);

        final GHPullRequest pullRequest = reader.forType(GHPullRequest.class).readValue(payload.get("pull_request"));
        final GHRepository repository = reader.forType(GHRepository.class).readValue(payload.get("repository"));
        GitHubAccessor.wrapUp(pullRequest, repository);

        final GHOrganization organization = gitHub.getOrganization(repository.getOwnerName());

        if (action == Action.OPENED) {
            onCreate(gitHub, organization, pullRequest);
        } else if (action == Action.SYNCHRONIZE) {
            onSync(gitHub, organization, repository, pullRequest);
        }
    }

    private static void onSync(GitHub gitHub, GHOrganization organization, GHRepository repository, GHPullRequest pullRequest) throws IOException {
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
                }""", repository.getOwnerName(), repository.getName(), pullRequest.getNumber());

        final JsonNode json = Jsons.at(queryJson, "data.repository.pullRequest");
        final FunctionalInterfaces.SupplierException<Set<GHUser>> triagers = FunctionalInterfaces.memoize(() -> organization
                .getTeamBySlug(GithubVars.TRIAGE_TEAM.get())
                .getMembers());
        PushAction.checkConflict(triagers, gitHub, json);
    }

    private static void onCreate(GitHub gitHub, GHOrganization organization, GHPullRequest pullRequest) throws IOException {
        // We split into steps to not crash if someone does one of the steps manually
        final List<FunctionalInterfaces.RunnableException> steps = new ArrayList<>();

        steps.add(() -> GitHubAccessor.addLabel(pullRequest, "Triage"));
        steps.add(() -> {
            final String prTitle = pullRequest.getTitle();
            if (prTitle.startsWith("[") && prTitle.contains("]")) {
                final String[] prFullVersion = prTitle.substring(1, prTitle.indexOf("]")).split("\\.");
                if (prFullVersion.length < 2) return;
                final String prVersion = prFullVersion[0] + "." + prFullVersion[1];
                GitHubAccessor.addLabel(pullRequest, prVersion);
            }
        });
        steps.add(() -> {
            final List<String> newFiles = DiffUtils.detectNewFiles(GitHubAccessor.getDiff(pullRequest).split("\n"));

            if (newFiles.stream().anyMatch(it -> it.contains("/event/") /* Check for new files in an event package */)) {
                GitHubAccessor.addLabel(pullRequest, "New Event");
                GitHubAccessor.addLabel(pullRequest, "Feature");
            }
            if (newFiles.stream().anyMatch(it -> it.contains("/client/") /* Check for new files in a client package in order to add the Rendering label - this isn't perfect, but it works */)) {
                GitHubAccessor.addLabel(pullRequest, "Rendering");
            }
        });

        if (!pullRequest.isDraft()) steps.add(() -> pullRequest.requestTeamReviewers(List.of(
                organization.getTeamBySlug(GithubVars.TRIAGE_TEAM.get())
        )));

        steps.add(() -> addToProject(gitHub, organization, GithubVars.TRIAGE_PROJECT.get(), pullRequest));

        steps.forEach(runnableException -> {
            try {
                runnableException.run();
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
