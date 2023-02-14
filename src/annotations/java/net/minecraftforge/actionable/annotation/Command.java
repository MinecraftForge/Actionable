/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.annotation;

public @interface Command {
    int SINGLE_SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;

    String[] name();

    String description() default "";

    String category() default "misc";
}
