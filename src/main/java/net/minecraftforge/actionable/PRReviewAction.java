package net.minecraftforge.actionable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.enums.Action;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReviewState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PRReviewAction {
    public static void run(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable {
        final Action action = Action.get(payload);
        if (action != Action.SUBMITTED) return;

        final GitHub gitHub = gitHubGetter.get();
        final ObjectReader reader = GitHubAccessor.objectReader(gitHub);

        final Context ctx = Jsons.read(reader, payload, Context.class);
        GitHubAccessor.wrapUp(ctx.pull_request, ctx.repository);

        final JsonNode comment = Jsons.at(payload, "review.body");
        if (comment != null && comment.asText().contains("[skip]")) return;

        final Set<String> labels = ctx.pull_request.getLabels()
                .stream().map(it -> it.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        final GHTeam triageTeam = ctx.organization.getTeamBySlug(GithubVars.TRIAGE_TEAM.get());
        if (ctx.sender.isMemberOf(triageTeam) && GHPullRequestReviewState.valueOf(Jsons.at(payload, "review.state").asText().toUpperCase(Locale.ROOT)) == GHPullRequestReviewState.APPROVED) {
            final GithubVars.AssignTeams teams = GithubVars.LABELS_TO_TEAMS.get();
            String team = null;
            for (final var entry : teams.byLabel().entrySet()) {
                if (labels.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                    team = entry.getValue();
                    break;
                }
            }
            if (team == null) team = teams.defaultTeam();

            if (team != null) {
                GitHubAccessor.addLabel(ctx.pull_request, "Assigned");
                GitHubAccessor.removeLabel(ctx.pull_request, "Triage");

                final GHUser author = ctx.pull_request.getUser();
                final List<GHUser> toAssign = ctx.organization.getTeamBySlug(team.toLowerCase(Locale.ROOT))
                        .getMembers().stream().filter(it -> !it.equals(author)).toList();
                ctx.pull_request.addAssignees(toAssign);
            }
        }
    }

    public record Context(GHUser sender, GHRepository repository, GHPullRequest pull_request, GHOrganization organization) {}
}
