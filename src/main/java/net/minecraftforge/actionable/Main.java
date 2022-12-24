package net.minecraftforge.actionable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraftforge.actionable.event.EventHandler;
import net.minecraftforge.actionable.event.IssueCommentHandler;
import net.minecraftforge.actionable.event.IssueHandler;
import net.minecraftforge.actionable.event.PRHandler;
import net.minecraftforge.actionable.event.PRReviewHandler;
import net.minecraftforge.actionable.event.PushHandler;
import net.minecraftforge.actionable.util.AuthUtil;
import net.minecraftforge.actionable.util.GitHubEvent;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.RepoConfig;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.minecraftforge.actionable.util.GitHubEvent.ISSUES;
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
            handlers.put(PUSH, PushHandler::new);
            handlers.put(ISSUES, IssueHandler::new);
            handlers.put(ISSUE_COMMENT, IssueCommentHandler::new);
            handlers.put(PULL_REQUEST, PRHandler::new);
            handlers.put(PULL_REQUEST_REVIEW, PRReviewHandler::new);
        }

        new Main(handlers).run();
    }

    public record Thingy(RepoConfig.TeamLike team) {}
    public record ThingyNested(Thingy thingy) {}

    public void run() throws Throwable {
        final Supplier<EventHandler> handler = eventHandlers.get(GithubVars.EVENT.get());
        if (handler != null) {
            handler.get().handle(GitHubGetter.memoize(() -> {
                final GitHub gh = buildApi();
                setupConfig(gh);
                return gh;
            }), this.payload());
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

    private void setupConfig(GitHub gitHub) throws IOException {
        final RepoConfig.ConfigLocation location = GithubVars.CONFIG_DIRECTORY.get();
        final RepoConfig unsanitized = RepoConfig.getOrCommit(
                gitHub.getRepository(location.repository()),
                location.directory(), location.branch(),
                GithubVars.REPOSITORY.get()
        );
        RepoConfig.INSTANCE = new RepoConfig(
                unsanitized.labels() == null ? Map.of() : unsanitized.labels(),
                unsanitized.labelLocks() == null ? Map.of() : unsanitized.labelLocks(),
                unsanitized.triage(),
                unsanitized.labelTeams() == null ? new LinkedHashMap<>() : unsanitized.labelTeams(),
                unsanitized.commands()
        );
    }

    private JsonNode payload() throws IOException {
        try (final InputStream in = Files.newInputStream(Path.of(GithubVars.EVENT_PATH.get()))) {
            return new ObjectMapper().readTree(in);
        }
    }

    public interface GitHubGetter {
        GitHub get() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException;

        static GitHubGetter memoize(GitHubGetter getter) {
            return new GitHubGetter() {
                GitHub gh;
                @Override
                public GitHub get() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
                    if (gh == null) gh = getter.get();
                    return gh;
                }
            };
        }
    }
}