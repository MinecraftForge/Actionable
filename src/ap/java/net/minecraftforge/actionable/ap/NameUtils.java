/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.ap;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class NameUtils {
    final Types types;
    private final Elements elements;

    public NameUtils(Types types, Elements elements) {
        this.types = types;
        this.elements = elements;
    }

    public String getTypeName(TypeMirror type) {
        if (type instanceof PrimitiveType) {
            return type.toString();
        }
        final TypeElement te = ((TypeElement) types.asElement(type));
        if (te != null) return te.getQualifiedName().toString();
        return type.toString();
    }

}
