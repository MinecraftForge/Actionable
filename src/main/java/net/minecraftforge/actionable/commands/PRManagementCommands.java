package net.minecraftforge.actionable.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraftforge.actionable.commands.lib.GHCommandContext;
import net.minecraftforge.actionable.util.GithubVars;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.util.Locale;
import java.util.function.Predicate;

import static net.minecraftforge.actionable.commands.Commands.argument;
import static net.minecraftforge.actionable.commands.Commands.hasPermission;
import static net.minecraftforge.actionable.commands.Commands.isInTeam;
import static net.minecraftforge.actionable.commands.Commands.literal;
import static net.minecraftforge.actionable.util.FunctionalInterfaces.wrap;

public class PRManagementCommands {
    public static void register(GitHub gh, CommandDispatcher<GHCommandContext> dispatcher) {
        final Predicate<GHCommandContext> canManage = isInTeam(GithubVars.TRIAGE_TEAM.get()).or(hasPermission(GHPermissionType.WRITE));

        dispatcher.register(literal("shipit")
                .requires(hasPermission(GHPermissionType.WRITE).and(ctx -> ctx.issue().isPullRequest()))
                .executes(wrap(ctx -> {
                    final GHPullRequest pr = ctx.getSource().pullRequest();
                    final String title = pr.getTitle() + " (#" + pr.getNumber() + ")";
                    GitHubAccessor.merge(pr, title, null, GHPullRequest.MergeMethod.SQUASH);
                    ctx.getSource().issue().comment(":shipit:");
                })));

        dispatcher.register(literal("closes")
                .requires(canManage.and(it -> it.issue().isPullRequest()))
                .then(argument("issue", StringArgumentType.word())
                        .executes(wrap(ctx -> {
                            final String body = ctx.getSource().issue().getBody();
                            final String bodyN = body == null || body.isBlank() ? "" : body + "\n";
                            final String toClose = StringArgumentType.getString(ctx, "issue");
                            ctx.getSource().issue().setBody(
                                    bodyN + (toClose.startsWith("#") ? toClose : "#" + toClose)
                            );
                        }))));

        dispatcher.register(literal("assign")
                .requires(canManage)
                .then(argument("team", StringArgumentType.word())
                        .executes(wrap(ctx -> {
                            final GHIssue issue = ctx.getSource().issue();
                            final GHUser author = issue.getUser();
                            final GHTeam team = ctx.getSource().gitHub()
                                    .getOrganization(ctx.getSource().issue().getRepository().getOwnerName())
                                    .getTeamBySlug(parseTeam(StringArgumentType.getString(ctx, "team")));

                            // We don't want to assign the PR author to their own PR
                            issue.setAssignees(team.getMembers().stream()
                                    .filter(it -> !it.equals(author)).limit(10).toList());

                            GitHubAccessor.addLabel(issue, "Assigned");
                            GitHubAccessor.removeLabel(issue, "Triage");
                        }))));
    }

    private static String parseTeam(String input) {
        return input.substring(input.indexOf("/") + 1).toLowerCase(Locale.ROOT);
    }

}
