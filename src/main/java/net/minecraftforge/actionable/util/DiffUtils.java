/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.util;

import java.util.ArrayList;
import java.util.List;

public class DiffUtils {
    private static final String NO_OLD_FILE = "--- /dev/null";
    private static final String FILE_ADDED_MARKER = "+++ ";

    public static List<String> detectNewFiles(String[] diffByLine) {
        final List<String> newFiles = new ArrayList<>();
        for (int i = 0; i < diffByLine.length; i++) {
            final String line = diffByLine[i];
            if (line.equals(NO_OLD_FILE)) {
                newFiles.add(diffByLine[i + 1].substring(FILE_ADDED_MARKER.length() + 1));
                i++; // Skip the add marker
            }
        }
        return newFiles;
    }
}
