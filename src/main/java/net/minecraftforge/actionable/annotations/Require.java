package net.minecraftforge.actionable.annotations;

import net.minecraftforge.actionable.commands.lib.Requirement;

public @interface Require {
    Requirement[] value();
}
