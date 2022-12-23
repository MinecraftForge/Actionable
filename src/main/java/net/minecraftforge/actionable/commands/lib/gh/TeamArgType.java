package net.minecraftforge.actionable.commands.lib.gh;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Locale;

public record TeamArgType(GitHub gitHub, String organization) implements ArgumentType<GHTeam> {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(value -> new LiteralMessage("Unknown team " + value));

    public static TeamArgType team(GitHub gitHub, String org) {
        return new TeamArgType(gitHub, org);
    }

    @Override
    public GHTeam parse(StringReader reader) throws CommandSyntaxException {
        final String name = reader.readUnquotedString();
        final String[] split = name.split("/");
        try {
            return gitHub.getOrganization(organization).getTeamBySlug(split[split.length - 1].toLowerCase(Locale.ROOT));
        } catch (IOException exception) {
            throw UNKNOWN.createWithContext(reader, name);
        }
    }
}
