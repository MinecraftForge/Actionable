package net.minecraftforge.actionable.commands.lib;

import net.minecraftforge.actionable.annotation.Value;
import net.minecraftforge.actionable.commands.Commands;
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
    IN_PULL_REQUEST(context -> context.issue().isPullRequest());

    private final Predicate<GHCommandContext> predicate;

    Requirement(Predicate<GHCommandContext> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(GHCommandContext context) {
        return predicate.test(context);
    }
}
