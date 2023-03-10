/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Jsons {
    public static JsonNode at(JsonNode node, String path) {
        final String[] paths = path.split("\\.");
        for (final String s : paths) {
            node = node.get(s);
        }
        return node;
    }

    public static Stream<JsonNode> stream(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false);
    }

    public static <T extends Record> T read(ObjectReader reader, JsonNode node, Class<? extends T> type) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final RecordComponent[] recordComponents = type.getRecordComponents();
        final Class<?>[] argTypes = new Class<?>[recordComponents.length];
        final Object[] args = new Object[recordComponents.length];
        for (int i = 0; i < recordComponents.length; i++) {
            final RecordComponent comp = recordComponents[i];
            final JsonNode json = node.get(comp.getName());
            argTypes[i] = comp.getType();

            if (json == null) {
                final Or annotation = comp.getAnnotation(Or.class);
                if (annotation == null) {
                    args[i] = null;
                } else {
                    final JsonNode sub = node.get(annotation.fieldName());
                    args[i] = sub == null ? null : reader.forType(annotation.type()).readValue(sub);
                }
            } else {
                args[i] = reader.forType(comp.getType()).readValue(json);
            }
        }
        return type.getDeclaredConstructor(argTypes).newInstance(args);
    }
}
