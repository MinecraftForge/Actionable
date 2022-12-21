package net.minecraftforge.actionable.commands.lib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;

public record RepoArgumentType(RepoGetter getter) implements ArgumentType<GHRepository> {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(value -> new LiteralMessage("Unknown repository " + value));

    public static RepoArgumentType repo(GitHub gitHub) {
        return new RepoArgumentType(gitHub::getRepository);
    }

    public static RepoArgumentType repo(GitHub gitHub, String org) {
        return new RepoArgumentType(it -> gitHub.getOrganization(org).getRepository(it));
    }

    @Override
    public GHRepository parse(StringReader reader) throws CommandSyntaxException {
        final String name = reader.readUnquotedString();
        try {
            return getter.get(name);
        } catch (IOException exception) {
            throw UNKNOWN.createWithContext(reader, name);
        }
    }

    public interface RepoGetter {
        GHRepository get(String name) throws IOException;
    }
}
