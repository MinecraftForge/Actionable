/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.annotations;

import net.minecraftforge.actionable.commands.lib.Requirement;

public @interface Require {
    Requirement[] value();
}
