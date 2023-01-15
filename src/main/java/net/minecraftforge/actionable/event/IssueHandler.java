package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.Label;
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
                        .register(Action.OPENED, IssueHandler::onCreate)
                        .register(Action.LABELED, IssueHandler::onLabelLock)
                        .register(Action.UNLABELED, IssueHandler::onLabelLockRemove)
        );
    }

    private static void onCreate(GitHub gitHub, Payload payload) throws IOException {
        final String issueTitle = payload.issue.getTitle();
        if (issueTitle.startsWith("[") && issueTitle.contains("]")) {
            final String fullVersion = issueTitle.substring(1, issueTitle.indexOf("]"));

            if (fullVersion.toLowerCase(Locale.ROOT).contains("rfc")) {
                Label.RFC.addAndIgnore(payload.issue);
            }

            final String[] fullVersionSplit = fullVersion.split("\\.");
            if (fullVersionSplit.length < 2) return;
            final String issueVersion = fullVersionSplit[0] + "." + fullVersionSplit[1];
            GitHubAccessor.addLabel(payload.issue, issueVersion);
        }

        if (payload.issue.getLabels().stream().anyMatch(it -> it.getName().equalsIgnoreCase("bug"))) {
            Label.TRIAGE.add(payload.issue);
        }
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
