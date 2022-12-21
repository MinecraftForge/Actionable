package net.minecraftforge.actionable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import net.minecraftforge.actionable.util.Action;
import net.minecraftforge.actionable.util.DiffUtils;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.Jsons;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PRCreateAction {
    public static void run(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable {
        final Action action = Action.get(payload);
        if (action != Action.OPENED) return; // Only run on PR open

        final GitHub gitHub = gitHubGetter.get();
        final ObjectReader reader = GitHubAccessor.objectReader(gitHub);

        final GHPullRequest pullRequest = reader.forType(GHPullRequest.class).readValue(payload.get("pull_request"));
        final GHRepository repository = reader.forType(GHRepository.class).readValue(payload.get("repository"));
        GitHubAccessor.wrapUp(pullRequest, repository);

        final GHOrganization organization = gitHub.getOrganization(repository.getOwnerName());

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

        steps.add(() -> pullRequest.requestTeamReviewers(List.of(
                organization.getTeamByName(GithubVars.TRIAGE_TEAM.get())
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
