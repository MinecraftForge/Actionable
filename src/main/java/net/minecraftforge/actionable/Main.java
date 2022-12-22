package net.minecraftforge.actionable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraftforge.actionable.commands.Commands;
import net.minecraftforge.actionable.commands.lib.CommandManager;
import net.minecraftforge.actionable.commands.lib.GHCommandContext;
import net.minecraftforge.actionable.util.AuthUtil;
import net.minecraftforge.actionable.util.GithubVars;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AuthorizationProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class Main {

    public static void main(String[] args) throws Throwable {
        switch (GithubVars.EVENT.get()) {
            case ISSUE_COMMENT -> runCommand();
            case PULL_REQUEST -> PRCreateAction.run(Main::buildApi, payload());
            case PULL_REQUEST_REVIEW -> PRReviewAction.run(Main::buildApi, payload());
            case PUSH -> PushAction.run(Main::buildApi, payload());
        }
    }

    private static void runCommand() throws Throwable {
        final JsonNode payload = payload();

        final GitHub gh = buildApi();

        final CommandDispatcher<GHCommandContext> dispatcher = new CommandDispatcher<>();
        Commands.register(gh, dispatcher);

        new CommandManager(
                GithubVars.COMMAND_PREFIXES.get(),
                GithubVars.ALLOW_COMMANDS_IN_EDITS.get(),
                gh, dispatcher
        ).run(payload);
    }

    private static GitHub buildApi() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        final PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(AuthUtil.parsePKCS8(GithubVars.GH_APP_KEY.get())));
        final String appId = GithubVars.GH_APP_NAME.get();

        final AuthorizationProvider authorizationProvider = AuthUtil.jwt(appId, key, app ->
                app.getInstallationByOrganization(GithubVars.REPOSITORY_OWNER.get())
                        .createToken().create());

        return new GitHubBuilder()
                .withAuthorizationProvider(authorizationProvider)
                .build();
    }

    private static JsonNode payload() throws IOException {
        try (final InputStream in = Files.newInputStream(Path.of(GithubVars.EVENT_PATH.get()))) {
            return new ObjectMapper().readTree(in);
        }
    }

    public interface GitHubGetter {
        GitHub get() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException;
    }
}
