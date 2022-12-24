package net.minecraftforge.actionable.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraftforge.actionable.commands.lib.GHCommandContext;
import net.minecraftforge.actionable.util.FunctionalInterfaces;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.RepoConfig;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Commands {
    public static void register(GitHub gh, CommandDispatcher<GHCommandContext> dispatcher) {
        PRManagementCommands.register(gh, dispatcher);
        IssueManagementCommands.register(gh, dispatcher);
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

    public static Predicate<GHCommandContext> hasPermission(GHPermissionType permission) {
        return FunctionalInterfaces.wrapPred(ctx -> ctx.issue().getRepository()
                .hasPermission(ctx.user(), permission));
    }

    public static LiteralArgumentBuilder<GHCommandContext> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<GHCommandContext, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static String randomHex() {
        return Integer.toHexString(new Random().nextInt(1_000_000_000));
    }
}