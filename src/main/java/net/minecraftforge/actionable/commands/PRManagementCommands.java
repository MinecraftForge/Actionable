package net.minecraftforge.actionable.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraftforge.actionable.commands.lib.GHCommandContext;
import net.minecraftforge.actionable.util.Label;
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
import static net.minecraftforge.actionable.commands.Commands.isTriage;
import static net.minecraftforge.actionable.commands.Commands.literal;
import static net.minecraftforge.actionable.util.FunctionalInterfaces.wrap;

public class PRManagementCommands {
    public static void register(GitHub gh, CommandDispatcher<GHCommandContext> dispatcher) {
        final Predicate<GHCommandContext> canManage = isTriage().or(hasPermission(GHPermissionType.WRITE));

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
                .then(argument("issue", StringArgumentType.greedyString())
                        .executes(wrap(ctx -> {
                            final String body = ctx.getSource().issue().getBody();
                            final String bodyN = body == null || body.isBlank() ? "" : body + "\n";
                            final String toClose = StringArgumentType.getString(ctx, "issue").trim();
                            ctx.getSource().issue().setBody(
                                    bodyN + (toClose.startsWith("#") ? toClose : "#" + toClose)
                            );
                        }))));

        dispatcher.register(literal("assign")
                .requires(canManage)
                .then(argument("team", StringArgumentType.greedyString())
                        .executes(wrap(ctx -> {
                            final GHIssue issue = ctx.getSource().issue();
                            final GHUser author = issue.getUser();
                            final GHTeam team = ctx.getSource().gitHub()
                                    .getOrganization(issue.getRepository().getOwnerName())
                                    .getTeamBySlug(parseTeam(StringArgumentType.getString(ctx, "team")).trim());

                            // We don't want to assign the PR author to their own PR
                            issue.setAssignees(team.getMembers().stream()
                                    .filter(it -> !it.equals(author)).limit(10).toList());

                            Label.ASSIGNED.addAndIgnore(issue);
                            Label.TRIAGE.removeAndIgnore(issue);
                        }))));
    }

    private static String parseTeam(String input) {
        return input.substring(input.lastIndexOf("/") + 1).toLowerCase(Locale.ROOT);
    }

}
