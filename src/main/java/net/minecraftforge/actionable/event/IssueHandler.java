package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.Or;
import net.minecraftforge.actionable.util.RepoConfig;
import net.minecraftforge.actionable.util.enums.Action;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;
import org.kohsuke.github.LockReason;

import java.io.IOException;
import java.util.Locale;

public class IssueHandler extends ByActionEventHandler<IssueHandler.Payload> {

    public IssueHandler() {
        super(
                Payload.class, payload -> GitHubAccessor.wrapUp(payload.issue, payload.repository),
                payloadRegistrar -> payloadRegistrar
                        .register(Action.LABELED, IssueHandler::onLabelLock)
                        .register(Action.UNLABELED, IssueHandler::onLabelLockRemove)
        );
    }

    public static void onLabelLock(GitHub gitHub, IssuePayload payload, JsonNode payloadJson) throws IOException {
        final RepoConfig.LabelLock lock = RepoConfig.INSTANCE.labelLocks().get(Jsons.at(payloadJson, "label.name").asText());
        if (lock == null) return;

        if (lock.message() != null) {
            payload.issue().comment(lock.message());
        }

        if (lock.close()) {
            GitHubAccessor.edit(payload.issue())
                    .edit("state", "closed")
                    .edit("state_reason", "not_planned")
                    .send();
        }

        if (lock.lock()) {
            if (lock.lockReason() == null) {
                payload.issue().lock();
            } else {
                GitHubAccessor.lock(payload.issue(), LockReason.valueOf(lock.lockReason().toUpperCase(Locale.ROOT)));
            }
        }
    }

    public static void onLabelLockRemove(GitHub gitHub, IssuePayload payload, JsonNode payloadJson) throws IOException {
        final RepoConfig.LabelLock lock = RepoConfig.INSTANCE.labelLocks().get(Jsons.at(payloadJson, "label.name").asText());
        if (lock == null) return;

        if (lock.lock()) payload.issue().unlock();
        if (lock.close()) payload.issue().reopen();
    }

    public record Payload(@Or(fieldName = "pull_request", type = GHPullRequest.class) GHIssue issue, GHRepository repository, GHOrganization organization) implements IssuePayload {}

    public interface IssuePayload {
        GHIssue issue();
    }
}
