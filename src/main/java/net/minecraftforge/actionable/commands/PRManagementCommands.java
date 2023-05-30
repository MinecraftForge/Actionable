/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.commands;

import net.minecraftforge.actionable.annotation.Argument;
import net.minecraftforge.actionable.annotation.Command;
import net.minecraftforge.actionable.annotation.StringType;
import net.minecraftforge.actionable.annotations.Require;
import net.minecraftforge.actionable.commands.lib.Requirement;
import net.minecraftforge.actionable.util.Label;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;
import java.util.Locale;

public class PRManagementCommands {
    public static final String GROUP = "pr_management";

    @Require(Requirement.CAN_MANAGE_ISSUE)
    @Command(name = "closes", category = GROUP, description = "Append `Closes <issue>` to the body of an issue.")
    public static void closes(GHIssue issue, @Argument(stringType = StringType.GREEDY_STRING, description = "the number of the issue to close. It may be preceded by a `#`.") String toClose) throws IOException {
        toClose = toClose.trim();
        final String body = issue.getBody();
        final String bodyN = body == null || body.isBlank() ? "" : body + "\n";
        issue.setBody(
                bodyN + "Closes " + (toClose.startsWith("#") ? toClose : "#" + toClose)
        );
    }

    @Require(Requirement.CAN_MANAGE_ISSUE)
    @Command(name = "assign", category = GROUP, description = "Assign a team of people to an issue / PR.")
    public static void assign(GitHub gitHub, GHIssue issue, @Argument(stringType = StringType.GREEDY_STRING, description = "the team to assign. Can be a reference like `triage` or a mention like `@minecraftforge/triage`.") String team) throws IOException {
        final GHUser author = issue.getUser();
        final GHTeam theTeam = gitHub
                .getOrganization(issue.getRepository().getOwnerName())
                .getTeamBySlug(parseTeam(team).trim());

        // We don't want to assign the PR author to their own PR
        issue.setAssignees(theTeam.getMembers().stream()
                .filter(it -> !it.equals(author)).limit(10).toList());

        Label.ASSIGNED.addAndIgnore(issue);
        Label.TRIAGE.removeAndIgnore(issue);
    }

    @Require({Requirement.CAN_MERGE_LTS, Requirement.IN_PULL_REQUEST})
    @Command(name = {"shipit", "merge"}, category = GROUP, description = "Merge the pull request.")
    public static void shipit(GHPullRequest pr) throws IOException {
        final String title = pr.getTitle() + " (#" + pr.getNumber() + ")";
        GitHubAccessor.merge(pr, title, null, GHPullRequest.MergeMethod.SQUASH);
        pr.comment(":shipit:");
    }

    private static String parseTeam(String input) {
        return input.substring(input.lastIndexOf("/") + 1).toLowerCase(Locale.ROOT);
    }

}
