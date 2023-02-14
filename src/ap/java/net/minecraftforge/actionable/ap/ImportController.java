/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.ap;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImportController {

    private final Map<String, String> fqnToSimple = new HashMap<>();
    private final String[] ownerPackage;

    private final Set<String> imports = new HashSet<>();

    public ImportController(String ownerClass) {
        final String[] split = ownerClass.split("\\.");
        this.ownerPackage = Arrays.copyOfRange(split, 0, split.length - 1);
    }

    public String considerImport(Name name) {
        return considerImport(name.toString());
    }

    public String considerImport(String name) {
        final String already = fqnToSimple.get(name);
        if (already != null) return already;

        final String[] onDot = name.split("\\.");
        if (onDot.length == 1) { // Primitive or top-level package
            fqnToSimple.put(name, name); return name;
        }

        final String last = onDot[onDot.length - 1];
        if (
                (onDot.length == 3 && onDot[0].equals("java") && onDot[1].equals("lang")) ||
                (onDot.length == (ownerPackage.length + 1) && Arrays.equals(ownerPackage, Arrays.copyOfRange(onDot, 0, onDot.length - 1)))
        ) {
            if (fqnToSimple.containsValue(last)) {
                fqnToSimple.put(name, name);
                return name;
            }
            fqnToSimple.put(name, last);
            return last;
        }

        if (fqnToSimple.containsValue(last)) {
            fqnToSimple.put(name, name);
            return name;
        } else {
            imports.add(name);
            fqnToSimple.put(name, last);
            return last;
        }
    }

    public String finish() {
        return imports.stream().sorted()
                .map(im -> "import " + im + ";")
                .collect(Collectors.joining("\n"));
    }

    public final class AndNames {
        private final NameUtils nameUtils;

        public AndNames(NameUtils nameUtils) {
            this.nameUtils = nameUtils;
        }

        public String getTypeName(TypeElement element) {
            return considerImport(element.getQualifiedName());
        }

        public String getTypeName(TypeMirror typeMirror) {
            if (typeMirror instanceof PrimitiveType) return typeMirror.toString();
            return considerImport(nameUtils.getTypeName(typeMirror));
        }

        public String getTypeName(Class<?> clazz) {
            return considerImport(clazz.getCanonicalName());
        }

        public String getTypeNameAndGenerics(TypeMirror typeMirror) {
            if (typeMirror instanceof PrimitiveType) return typeMirror.toString();
            final String baseName = considerImport(nameUtils.getTypeName(typeMirror));
            final List<? extends TypeMirror> generics = ((DeclaredType) typeMirror).getTypeArguments();
            final String genericsStr = generics.isEmpty() ? "" :
                    "<" + generics.stream().map(this::getGenericName).collect(Collectors.joining(", ")) + ">";
            return baseName + genericsStr;
        }

        public String getGenericName(TypeMirror typeMirror) {
            if (!(typeMirror instanceof DeclaredType)) return nameUtils.getTypeName(typeMirror);
            final List<? extends TypeMirror> generics = ((DeclaredType) typeMirror).getTypeArguments();
            if (generics.isEmpty()) {
                return nameUtils.getTypeName(typeMirror);
            }
            return nameUtils.getTypeName(typeMirror) + "<" + generics.stream().map(this::getGenericName).collect(Collectors.joining(", ")) + ">";
        }
    }
}
