/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

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
import net.minecraftforge.actionable.util.config.RepoConfig;
import org.kohsuke.github.GHArtifact;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AuthorizationProvider;
import org.kohsuke.github.function.InputStreamFunction;

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
import java.util.zip.ZipInputStream;

import static net.minecraftforge.actionable.util.GitHubEvent.ISSUES;
import static net.minecraftforge.actionable.util.GitHubEvent.ISSUE_COMMENT;
import static net.minecraftforge.actionable.util.GitHubEvent.PULL_REQUEST_REVIEW;
import static net.minecraftforge.actionable.util.GitHubEvent.PULL_REQUEST_TARGET;
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
            handlers.put(PULL_REQUEST_TARGET, PRHandler::new);
            handlers.put(PULL_REQUEST_REVIEW, PRReviewHandler::new);
        }

        new Main(handlers).run();
    }

    public record Thingy(RepoConfig.TeamLike team) {}
    public record ThingyNested(Thingy thingy) {}

    public void run() throws Throwable {
        if (GithubVars.GH_APP_NAME.get() == null || GithubVars.GH_APP_NAME.get().isBlank() || GithubVars.GH_APP_KEY.get() == null || GithubVars.GH_APP_KEY.get().isBlank()) {
            System.out.println("No GitHub App ID or key was provided!");
            return;
        }

        final GitHubEvent event = GithubVars.EVENT.get();

        if (event == GitHubEvent.WORKFLOW_RUN) { // In the case of WORKFLOW_RUN, we need to simulate the payload
            final JsonNode payload = payload();
            final JsonNode run = payload.get("workflow_run");

            final Supplier<EventHandler> handler = eventHandlers.get(GitHubEvent.BY_ID.get(run.get("event").asText()));
            if (handler != null && run.get("conclusion").asText().equals("success")) {
                final long id = run.get("id").asLong();
                System.out.println("Running as workflow " + id + "...");
                final GitHub gh = buildApi();
                final GHArtifact[] artifacts = GitHubAccessor.getArtifacts(gh, GithubVars.REPOSITORY.get(), id);
                final JsonNode actualPayload = artifacts[0].download(firstEntry(input -> new ObjectMapper().readValue(input, JsonNode.class)));
                handler.get().handle(GitHubGetter.memoize(() -> {
                    setupConfig(gh);
                    return gh;
                }), actualPayload);
            }
        } else {
            final Supplier<EventHandler> handler = eventHandlers.get(event);
            if (handler != null) {
                handler.get().handle(GitHubGetter.memoize(() -> {
                    final GitHub gh = buildApi();
                    setupConfig(gh);
                    return gh;
                }), this.payload());
            }
        }
    }

    private static <Z> InputStreamFunction<Z> firstEntry(InputStreamFunction<Z> fun) {
        return input -> {
            try (final ZipInputStream zis = new ZipInputStream(input)) {
                zis.getNextEntry();
                return fun.apply(zis);
            }
        };
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
        RepoConfig.INSTANCE = unsanitized.sanitize();
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
