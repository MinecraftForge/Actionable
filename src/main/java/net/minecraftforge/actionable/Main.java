package net.minecraftforge.actionable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraftforge.actionable.event.EventHandler;
import net.minecraftforge.actionable.event.IssueCommentHandler;
import net.minecraftforge.actionable.event.PRHandler;
import net.minecraftforge.actionable.event.PRReviewHandler;
import net.minecraftforge.actionable.util.AuthUtil;
import net.minecraftforge.actionable.util.GitHubEvent;
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
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.minecraftforge.actionable.util.GitHubEvent.ISSUE_COMMENT;
import static net.minecraftforge.actionable.util.GitHubEvent.PULL_REQUEST;
import static net.minecraftforge.actionable.util.GitHubEvent.PULL_REQUEST_REVIEW;
import static net.minecraftforge.actionable.util.GitHubEvent.PUSH;

public record Main(
        Map<GitHubEvent, Supplier<EventHandler>> eventHandlers
) {

    public static void main(String[] args) throws Throwable {
        final Map<GitHubEvent, Supplier<EventHandler>> handlers = new EnumMap<>(GitHubEvent.class);

        {
            handlers.put(PUSH, PushAction::new);
            handlers.put(ISSUE_COMMENT, IssueCommentHandler::new);
            handlers.put(PULL_REQUEST, PRHandler::new);
            handlers.put(PULL_REQUEST_REVIEW, PRReviewHandler::new);
        }

        new Main(handlers).run();
    }

    public void run() throws Throwable {
        final Supplier<EventHandler> handler = eventHandlers.get(GithubVars.EVENT.get());
        if (handler != null) {
            handler.get().handle(this::buildApi, this.payload());
        }
    }

    private GitHub buildApi() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        final PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(AuthUtil.parsePKCS8(GithubVars.GH_APP_KEY.get())));
        final String appId = GithubVars.GH_APP_NAME.get();

        final AuthorizationProvider authorizationProvider = AuthUtil.jwt(appId, key, app ->
                app.getInstallationByOrganization(GithubVars.REPOSITORY_OWNER.get())
                        .createToken().create());

        return new GitHubBuilder()
                .withAuthorizationProvider(authorizationProvider)
                .build();
    }

    private JsonNode payload() throws IOException {
        try (final InputStream in = Files.newInputStream(Path.of(GithubVars.EVENT_PATH.get()))) {
            return new ObjectMapper().readTree(in);
        }
    }

    public interface GitHubGetter {
        GitHub get() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException;
    }
}
