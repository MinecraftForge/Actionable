/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.util;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHubAccessor;

import java.io.IOException;
import java.util.Locale;

public enum Label {
    LTS_BACKPORT("lts", "LTS Backport"),
    NEEDS_REBASE("needs_rebase", "Needs Rebase"),
    RENDERING("Rendering"),
    NEW_EVENT("new_event", "New Event"),
    FEATURE("Feature"),
    ASSIGNED("Assigned"),
    TRIAGE("Triage"),
    RFC("RFC"),

    LATEST("latest");

    final String id, defaultName;

    Label(String id, String defaultName) {
        this.id = id;
        this.defaultName = defaultName;
    }

    Label(String name) {
        this(name.toLowerCase(Locale.ROOT), name);
    }

    public void add(GHIssue issue) throws IOException {
        GitHubAccessor.addLabel(issue, getLabelName());
    }

    public void addAndIgnore(GHIssue issue) {
        try {
            add(issue);
        } catch (IOException ignored) {
        }
    }

    public void remove(GHIssue issue) throws IOException {
        GitHubAccessor.removeLabel(issue, getLabelName());
    }

    public void removeAndIgnore(GHIssue issue) {
        try {
            remove(issue);
        } catch (IOException ignored) {
        }
    }

    public String getId() {
        return id;
    }

    public String getLabelName() {
        return RepoConfig.INSTANCE.labels().getOrDefault(id, defaultName);
    }
}
