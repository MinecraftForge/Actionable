/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraftforge.actionable.commands.lib.GHCommandContext;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.Label;
import net.minecraftforge.actionable.util.config.RepoConfig;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Random;
import java.util.function.Predicate;

public class Commands {
    public static void register(GitHub gh, CommandDispatcher<GHCommandContext> dispatcher) {
        try {
            Class.forName("net.minecraftforge.actionable.commands.CommandRegistrar")
                    .getDeclaredMethod("registerCommands", GitHub.class, CommandDispatcher.class)
                    .invoke(null, gh, dispatcher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isInTeam(GHCommandContext context, String team) throws IOException {
        final String[] split = team.split("/");
        final String org = split.length == 1 ? GithubVars.REPOSITORY_OWNER.get() : split[0];
        final String name = split.length == 1 ? split[0] : split[1];
        return context.gitHub().getOrganization(org)
                .getTeamBySlug(name).hasMember(context.user());
    }

    public static Predicate<GHCommandContext> isTriage() {
        if (RepoConfig.INSTANCE.triage() == null) return e -> false;
        return FunctionalInterfaces.wrapPred(ctx -> isInTeam(ctx, RepoConfig.INSTANCE.triage().teamName()));
    }

    public static Predicate<GHCommandContext> isLTSMember() {
        return FunctionalInterfaces.wrapPred(ctx -> {
            final RepoConfig.TeamLike team = RepoConfig.INSTANCE.labelTeams().get(Label.LTS_BACKPORT.getLabelName());
            return team != null && team.isMember(
                    ctx.gitHub(), ctx.gitHub().getOrganization(GithubVars.REPOSITORY_OWNER.get()),
                    ctx.user()
            );
        });
    }

    public static Predicate<GHCommandContext> hasPermission(GHPermissionType permission) {
        return FunctionalInterfaces.wrapPred(ctx -> ctx.issue().getRepository()
                .hasPermission(ctx.user(), permission));
    }

    public static String randomHex() {
        return Integer.toHexString(new Random().nextInt(1_000_000_000));
    }
}
