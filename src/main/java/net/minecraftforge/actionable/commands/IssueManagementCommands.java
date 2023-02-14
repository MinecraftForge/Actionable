/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.commands;

import net.minecraftforge.actionable.annotation.Argument;
import net.minecraftforge.actionable.annotation.Command;
import net.minecraftforge.actionable.annotation.StringType;
import net.minecraftforge.actionable.annotations.Require;
import net.minecraftforge.actionable.commands.lib.GHCommandContext;
import net.minecraftforge.actionable.commands.lib.Requirement;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHubAccessor;
import org.kohsuke.github.LockReason;

import java.io.IOException;

import static net.minecraftforge.actionable.commands.Commands.randomHex;

public class IssueManagementCommands {
    public static final String GROUP = "issue_management";

    @Require({Requirement.CAN_MANAGE_ISSUE, Requirement.IN_PULL_REQUEST})
    @Command(name = {"move", "transfer"}, category = GROUP, description = "Moves the issue this command is run in to another repository.")
    public static void move(GHCommandContext context, @Argument(description = "the repository to move the issue to") GHRepository repo) throws IOException {
        GitHubAccessor.graphQl(
                context.gitHub(),
                """
                        mutation {
                          transferIssue(input: {
                            clientMutationId: "%s",
                            repositoryId: "%s",
                            issueId: "%s"
                          }) {
                            issue {
                              number
                            }
                          }
                        }""".formatted(
                        randomHex(),
                        repo.getNodeId(),
                        context.issue().getNodeId()
                ));
    }

    @Require(Requirement.CAN_MANAGE_ISSUE)
    @Command(name = "title", category = GROUP, description = "Set the title of the issue.")
    public static void title(GHCommandContext ctx, @Argument(stringType = StringType.GREEDY_STRING, description = "the new title of the issue") String title) throws IOException {
        ctx.issue().setTitle(title);
    }

    @Require(Requirement.CAN_MANAGE_ISSUE)
    @Command(name = "lock", category = GROUP, description = "Lock the issue.")
    public static void lock(GHCommandContext ctx, @Argument(stringType = StringType.GREEDY_STRING, optional = true, description = "the reason for locking the issue") @Nullable LockReason reason) throws IOException {
        GitHubAccessor.lock(ctx.issue(), reason);
    }

    @Require(Requirement.CAN_MANAGE_ISSUE)
    @Command(name = "clock", category = GROUP, description = "Lock and close the issue.")
    public static int clock(GHCommandContext ctx, @Argument(stringType = StringType.GREEDY_STRING, optional = true, description = "the reason for locking the issue. If `resolved`, the close reason will be `completed`, otherwise it will be `not_planned`") @Nullable LockReason reason) throws IOException {
        GitHubAccessor.edit(ctx.issue())
                .edit("state", "closed")
                .edit("state_reason", reason == LockReason.RESOLVED ? "completed" : "not_planned")
                .send();
        GitHubAccessor.lock(ctx.issue(), reason);
        return Command.SINGLE_SUCCESS;
    }
}
