package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraftforge.actionable.Main;
import net.minecraftforge.actionable.commands.Commands;
import net.minecraftforge.actionable.commands.lib.CommandManager;
import net.minecraftforge.actionable.commands.lib.GHCommandContext;
import net.minecraftforge.actionable.util.RepoConfig;
import org.kohsuke.github.GitHub;

public class IssueCommentHandler implements EventHandler {
    @Override
    public void handle(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable {
        final GitHub gh = gitHubGetter.get();
        if (RepoConfig.INSTANCE.commands() == null) return;

        final CommandDispatcher<GHCommandContext> dispatcher = new CommandDispatcher<>();
        Commands.register(gh, dispatcher);

        new CommandManager(
                RepoConfig.INSTANCE.commands(),
                gh, dispatcher
        ).run(payload);
    }
}
