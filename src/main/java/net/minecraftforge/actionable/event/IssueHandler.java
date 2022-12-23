package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import net.minecraftforge.actionable.util.GithubVars;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.enums.Action;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;

public class IssueHandler extends ByActionEventHandler<IssueHandler.Payload> {

    public IssueHandler() {
        super(
                Payload.class, payload -> GitHubAccessor.wrapUp(payload.issue, payload.repository),
                payloadRegistrar -> payloadRegistrar
                        .register(Action.LABELED, IssueHandler::onLabeled)
        );
    }

    private static void onLabeled(GitHub gitHub, Payload payload, JsonNode payloadJson) throws IOException {
        if (Jsons.at(payloadJson, "label.name").asText().equalsIgnoreCase(GithubVars.FORUM_LABEL.get())) {
            payload.issue.comment("""
                    :wave: We use the issue tracker exclusively for final bug reports and feature requests.  
                    However, this issue appears to be better suited for the [Forge Support Forums](https://forums.minecraftforge.net/) or [Forge Discord](https://discord.gg/UvedJ9m).  
                    Please create a new topic on the support forum with this issue or ask in the `#tech-support` channel in the Discord server, and the conversation can continue there.""".trim());
            GitHubAccessor.edit(payload.issue())
                    .edit("state", "closed")
                    .edit("state_reason", "not_planned")
                    .send();
            payload.issue.lock();
        }
    }

    public record Payload(GHIssue issue, GHRepository repository, GHOrganization organization) {}
}
