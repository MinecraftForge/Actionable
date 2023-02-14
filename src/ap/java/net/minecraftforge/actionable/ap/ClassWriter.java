/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.ap;

import javax.lang.model.element.Modifier;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassWriter {
    private final String className;
    private final ImportController imports;
    private final StringWriter contents = new StringWriter();
    private final Writer bodyWriter = new Writer(new PrintWriter(contents), 0);

    public ClassWriter(String className) {
        this.className = className;
        this.imports = new ImportController(className);
    }

    public ImportController imports() {
        return imports;
    }

    public Writer bodyWriter() {
        return bodyWriter;
    }

    public void annotate(Class<? extends Annotation> annotation, String data) {
        bodyWriter.println("@" + imports.considerImport(annotation.getName()) + "(" + data + ")");
    }

    public void enterClass(Set<Modifier> modifiers, String name, Set<String> implementsX, Set<String> extendsX) {
        bodyWriter.print(modifiers.stream().map(Modifier::toString).collect(Collectors.joining(" ")));
        bodyWriter.print(" class ");
        bodyWriter.print(name);

        if (!extendsX.isEmpty()) {
            bodyWriter.print(" extends ");
            bodyWriter.print(extendsX.stream().map(imports::considerImport).collect(Collectors.joining(", ")));
        }

        if (!implementsX.isEmpty()) {
            bodyWriter.print(" implements ");
            bodyWriter.print(implementsX.stream().map(imports::considerImport).collect(Collectors.joining(", ")));
        }

        bodyWriter.enterScopeln();
    }

    public void exitClass() {
        bodyWriter.exitScope();
    }

    public void write(PrintWriter out) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            final String pkg = className.substring(0, lastDot);
            out.print("package ");
            out.print(pkg);
            out.println(";");
            out.println();
        }

        final String imports = this.imports.finish();
        if (!imports.isEmpty()) {
            out.print(imports);
            out.println();
            out.println();
        }

        out.print(contents);
    }
}
