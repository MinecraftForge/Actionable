/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.commands.lib;

import net.minecraftforge.actionable.annotation.Value;
import net.minecraftforge.actionable.commands.Commands;
import net.minecraftforge.actionable.util.Label;
import org.kohsuke.github.GHPermissionType;

import java.util.function.Predicate;

public enum Requirement implements Predicate<GHCommandContext> {
    @Value(description = "The user running the command must be part of the triage team.")
    IS_TRIAGE(Commands.isTriage()),
    @Value(description = "The user running the command must have writer permissions to the repository the command is run in.")
    HAS_WRITE_PERMS(Commands.hasPermission(GHPermissionType.WRITE)),
    @Value(description = "The user running the command must either be part of the triage team or have write permissions to the repository the command is run in.")
    CAN_MANAGE_ISSUE(Commands.isTriage().or(Commands.hasPermission(GHPermissionType.WRITE))),
    @Value(description = "The command may only be run in a pull request, and NOT an issue.")
    IN_PULL_REQUEST(context -> context.issue().isPullRequest()),

    @Value(description = "The issue the command is run on must have the LTS label.")
    IS_LTS_PR(context -> Label.LTS_BACKPORT.issueHasLabel(context.issue())),
    @Value(description = "The person running the command must be a LTS team member.")
    IS_LTS_TEAM(Commands.isLTSMember()),

    @Value(description = "The person running the command must be a LTS team member, and the PR must have the LTS label OR the user running the command must have write permissions to the repository.")
    CAN_MERGE_LTS(HAS_WRITE_PERMS.or(IS_LTS_PR.and(IS_LTS_TEAM))),
    ;

    private final Predicate<GHCommandContext> predicate;

    Requirement(Predicate<GHCommandContext> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(GHCommandContext context) {
        return predicate.test(context);
    }
}
