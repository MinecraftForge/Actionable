package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraftforge.actionable.Main;
import net.minecraftforge.actionable.commands.Commands;
import net.minecraftforge.actionable.commands.lib.CommandManager;
import net.minecraftforge.actionable.commands.lib.GHCommandContext;
import net.minecraftforge.actionable.util.GithubVars;
import org.kohsuke.github.GitHub;

public class IssueCommentHandler implements EventHandler {
    @Override
    public void handle(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable {
        final GitHub gh = gitHubGetter.get();

        final CommandDispatcher<GHCommandContext> dispatcher = new CommandDispatcher<>();
        Commands.register(gh, dispatcher);

        new CommandManager(
                GithubVars.COMMAND_PREFIXES.get(),
                GithubVars.ALLOW_COMMANDS_IN_EDITS.get(),
                gh, dispatcher
        ).run(payload);
    }
}
