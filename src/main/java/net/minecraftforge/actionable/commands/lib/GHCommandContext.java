package net.minecraftforge.actionable.commands.lib;

import com.fasterxml.jackson.databind.JsonNode;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;

public record GHCommandContext(GitHub gitHub, GHIssueComment comment, GHIssue issue, JsonNode payload) {
    public GHUser user() throws IOException  {
        GitHubAccessor.wrapUp(comment, null); // No need to request the user from GH
        final GHUser user = comment.getUser();
        GitHubAccessor.wrapUp(comment, issue);
        return user;
    }

    public GHRepository repository() {
        return issue().getRepository();
    }

    public GHPullRequest pullRequest() throws IOException {
        return repository().getPullRequest(issue.getNumber());
    }
}
