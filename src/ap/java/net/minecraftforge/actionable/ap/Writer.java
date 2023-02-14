/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.ap;

import java.io.PrintWriter;

public class Writer {
    public static final int SCOPE_INDENT_AMOUNT = 4;

    private final PrintWriter writer;
    private final int defaultIndent;

    public Writer(PrintWriter writer, int defaultIndent) {
        this.writer = writer;
        this.defaultIndent = defaultIndent;
    }

    private int indentAmount;
    public Writer increaseIndent(int amount) {
        this.indentAmount += amount;
        return this;
    }

    public Writer decreaseIndent(int amount) {
        this.indentAmount -= amount;
        return this;
    }

    public Writer indentWith(int amount) {
        this.indentAmount = amount;
        return this;
    }

    public Writer endIndent() {
        this.indentAmount = 0;
        return this;
    }

    public Writer pr(Object text) {
        writer.print(text);
        return this;
    }

    public Writer print(Object text) {
        writeIndent();
        writer.print(text);
        return this;
    }

    public Writer println(Object text) {
        writeIndent();
        writer.println(text);
        return this;
    }

    public Writer println() {
        writer.println();
        return this;
    }

    public Writer enterScope() {
        return println("{")
                .increaseIndent(SCOPE_INDENT_AMOUNT);
    }

    public Writer enterScopeln() {
        return println().enterScope();
    }

    public Writer exitScope() {
        return decreaseIndent(SCOPE_INDENT_AMOUNT)
                .println("}");
    }

    public Writer exitScopeln() {
        return exitScope().println();
    }

    public Writer writeIndent() {
        final int to = indentAmount + defaultIndent;
        if (to > 0) {
            writer.print(" ".repeat(to));
        }
        return this;
    }

    public int getIndentAmount() {
        return indentAmount;
    }
}
