package net.minecraftforge.actionable.util;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;

public record Label(String name) {
    public static final Label ASSIGNED = new Label("Assigned");
    public static final Label FEATURE = new Label("Feature");
    public static final Label NEW_EVENT = new Label("New Event");
    public static final Label RENDERING = new Label("Rendering");
    public static final Label LTS_BACKPORT = new Label("LTS Backport");
    public static final Label TRIAGE = new Label("Triage");

    public void add(GHIssue issue) throws IOException {
        GitHubAccessor.addLabel(issue, name);
    }

    public void addAndIgnore(GHIssue issue) {
        try {
            add(issue);
        } catch (IOException ignored) {}
    }

    public void remove(GHIssue issue) throws IOException {
        GitHubAccessor.removeLabel(issue, name);
    }

    public void removeAndIgnore(GHIssue issue) {
        try {
            remove(issue);
        } catch (IOException ignored) {}
    }
}
