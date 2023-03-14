/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.api.GetProjectFieldsQuery;
import com.github.api.GetPullRequestQuery;
import com.github.api.UpdateProjectItemMutation;
import com.github.api.fragment.PullRequestInfo;
import com.github.api.type.ProjectV2FieldValue;
import net.minecraftforge.actionable.Main;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.Label;
import net.minecraftforge.actionable.util.config.RepoConfig;
import net.minecraftforge.actionable.util.enums.Action;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReviewState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public class PRReviewHandler implements EventHandler {

    @Override
    public void handle(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable {
        final Action action = Action.get(payload);
        if (action != Action.SUBMITTED) return;

        final GitHub gitHub = gitHubGetter.get();
        if (!RepoConfig.INSTANCE.featureEnabled("autoAssign") || RepoConfig.INSTANCE.triage() == null) return;
        final ObjectReader reader = GitHubAccessor.objectReader(gitHub);

        final Context ctx = Jsons.read(reader, payload, Context.class);
        GitHubAccessor.wrapUp(ctx.pull_request, ctx.repository);

        final JsonNode comment = Jsons.at(payload, "review.body");
        if (comment != null && comment.asText().contains("[skip]")) return;

        final GHTeam triageTeam = ctx.organization.getTeamBySlug(RepoConfig.INSTANCE.triage().teamName());
        if (ctx.sender.isMemberOf(triageTeam) && GHPullRequestReviewState.valueOf(Jsons.at(payload, "review.state").asText().toUpperCase(Locale.ROOT)) == GHPullRequestReviewState.APPROVED) {
            // Query a fresh PR instance as the triager may have done stuff in the meantime
            final GHPullRequest pr = ctx.repository.getPullRequest(ctx.pull_request.getNumber());
            final Set<String> labels = pr.getLabels().stream()
                    .map(it -> it.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

            RepoConfig.TeamLike team = null;
            for (final var entry : RepoConfig.INSTANCE.labelTeams().entrySet()) {
                if (labels.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                    team = entry.getValue();
                    break;
                }
            }
            if (team == null) team = RepoConfig.INSTANCE.labelTeams().get("default");

            if (team != null) {
                Label.ASSIGNED.addAndIgnore(pr);
                Label.TRIAGE.removeAndIgnore(pr);

                if (pr.getAssignees().isEmpty()) {
                    final GHUser author = ctx.pull_request.getUser();
                    final List<GHUser> toAssign = team.getUsers(gitHub, ctx.organization).stream().filter(it -> !it.equals(author)).toList();
                    ctx.pull_request.addAssignees(toAssign);
                }
            }

            FunctionalInterfaces.ignoreExceptions(() -> {
                final String triageProject = PRHandler.getProjectId(gitHub, ctx.organization, RepoConfig.INSTANCE.triage().projectId());
                final FieldData statusField = getSelectField(gitHub, triageProject, "Status");
                updateField(gitHub, triageProject, statusField, getPRNodeId(gitHub, ctx.pull_request, triageProject), "Assigned to Core");
            });
        }
    }

    private static FieldData getSelectField(GitHub gitHub, String projectId, String fieldName) throws IOException {
        return ((GetProjectFieldsQuery.AsProjectV2) GitHubAccessor.graphQl(gitHub, new GetProjectFieldsQuery(projectId))
                .node()).fields().nodes().stream()
                .filter(it -> it instanceof GetProjectFieldsQuery.AsProjectV2SingleSelectField)
                .map(GetProjectFieldsQuery.AsProjectV2SingleSelectField.class::cast)
                .filter(it -> it.name().equalsIgnoreCase(fieldName))
                .map(it -> new FieldData(it.id(), it.name(), it.options().stream()
                        .collect(Collectors.toMap(GetProjectFieldsQuery.Option::name, GetProjectFieldsQuery.Option::id))))
                .findFirst().orElseThrow();
    }

    private static void updateField(GitHub gitHub, String projectId, FieldData fieldData, String itemId, String value) throws IOException {
        GitHubAccessor.graphQl(gitHub, UpdateProjectItemMutation.builder()
                .fieldId(fieldData.id)
                .projectId(projectId)
                .itemId(itemId)
                .value(ProjectV2FieldValue.builder()
                        .singleSelectOptionId(fieldData.options.get(value))
                        .build())
                .build());
    }

    public static String getPRNodeId(GitHub gitHub, GHPullRequest pr, String projectId) throws IOException {
        return GitHubAccessor.graphQl(gitHub, new GetPullRequestQuery(pr.getRepository().getOwnerName(), pr.getRepository().getName(), pr.getNumber()))
                .repository().pullRequest().fragments().pullRequestInfo()
                .projectItems().nodes().stream().filter(it -> it.project().id().equals(projectId))
                .findFirst().map(PullRequestInfo.Node2::id).orElseThrow();
    }

    public record FieldData(String id, String name, Map<String, String> options) {}

    public record Context(GHUser sender, GHRepository repository, GHPullRequest pull_request, GHOrganization organization) {}
}
