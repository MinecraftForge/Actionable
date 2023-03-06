/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.commands.lib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.config.RepoConfig;
import net.minecraftforge.actionable.util.enums.Action;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.enums.ReportedContentClassifiers;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;
import org.kohsuke.github.ReactionContent;

public record CommandManager(RepoConfig.Commands config, GitHub gitHub, CommandDispatcher<GHCommandContext> dispatcher) {
    public void run(JsonNode payload) throws Throwable {
        final Action action = Action.get(payload);
        if (!this.shouldRunForEvent(action)) return;

        final CommandData command = findCommand(Jsons.at(payload, "comment.body").asText());
        if (command == null) return;

        final ObjectReader reader = GitHubAccessor.objectReader(gitHub);

        final Payload pl = Jsons.read(reader, payload, Payload.class);
        GitHubAccessor.wrapUp(pl.issue, pl.repository);
        GitHubAccessor.wrapUp(pl.comment, pl.issue);

        final GHCommandContext ctx = new GHCommandContext(gitHub, pl.comment, pl.issue, payload);
        final ParseResults<GHCommandContext> results = dispatcher.parse(command.command(), ctx);

        // If the command does not fully parse, then return
        if (results.getReader().getRemainingLength() > 0) {
            return;
        }

        try {
            final int result = dispatcher.execute(results);
            if (result == Command.SINGLE_SUCCESS) {
                if (config.reactToComment()) {
                    FunctionalInterfaces.ignoreExceptions(() -> pl.comment.createReaction(ReactionContent.ROCKET));
                }
                if (command.commentOnlyCommand() && config.minimizeComment()) {
                    FunctionalInterfaces.ignoreExceptions(() -> GitHubAccessor.minimize(pl.comment, ReportedContentClassifiers.RESOLVED));
                }
            }
        } catch (Exception e) {
            System.err.println("Error while executing command: " + command);
            e.printStackTrace();

            if (e instanceof CommandSyntaxException exception) {
                //noinspection deprecation
                FunctionalInterfaces.ignoreExceptions(() -> pl.issue.comment("@%s, I encountered an exception executing that command: %s".formatted(
                        pl.comment.getUserName(), exception.getMessage()
                )));
            }

            if (config.reactToComment()) {
                FunctionalInterfaces.ignoreExceptions(() -> pl.comment.createReaction(ReactionContent.CONFUSED));
            }

            System.exit(1);
        }
    }

    public boolean shouldRunForEvent(final Action action) {
        if (action == Action.CREATED) return true;
        if (action == Action.EDITED) return config.allowEdits();
        return false;
    }

    public CommandData findCommand(String comment) {
        boolean commentOnlyCommand = false;
        String command = null;
        for (final String prefix : config.prefixes()) {
            System.out.println("Checking for commands with prefix '" + prefix + "'");
            if (comment.startsWith(prefix)) {
                // If at the start, consider the entire comment a command
                command = comment.substring(prefix.length());
                commentOnlyCommand = true;
            } else if (comment.contains(prefix)) {
                final var index = comment.indexOf(prefix);
                // If anywhere else, consider the line a command
                final var newLineIndex = comment.indexOf('\n', index);
                if (newLineIndex >= 0) {
                    command = comment.substring(index + prefix.length(), newLineIndex);
                } else {
                    command = comment.substring(index + prefix.length());
                }
            }

            if (command != null) {
                return new CommandData(commentOnlyCommand, command);
            }
        }

        return null;
    }

    public record CommandData(boolean commentOnlyCommand, String command) {}

    public record Payload(GHIssue issue, GHRepository repository, GHIssueComment comment) {}
}