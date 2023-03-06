/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.util.config;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

@JsonSerialize(using = FeaturePredicate.Serializer.class)
@JsonDeserialize(using = FeaturePredicate.Deserializer.class)
public record FeaturePredicate(Set<String> enabled, Set<String> disabled, boolean wildcard) implements Predicate<String> {
    public static final FeaturePredicate DEFAULT = new FeaturePredicate(Set.of(), Set.of(), true);

    @Override
    public boolean test(String s) {
        if (enabled.contains(s)) return true;
        else if (disabled.contains(s)) return false;
        return wildcard;
    }

    public static final class Serializer extends JsonSerializer<FeaturePredicate> {

        @Override
        public void serialize(FeaturePredicate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            final List<String> data = new ArrayList<>();
            if (value.wildcard) {
                data.add("*");
            }
            data.addAll(value.enabled.stream().map(it -> "+ " + it).toList());
            data.addAll(value.disabled.stream().map(it -> "- " + it).toList());
            if (data.size() == 1) {
                gen.writeString(data.get(0));
            } else {
                gen.writeArray(data.toArray(String[]::new), 0, data.size());
            }
        }
    }

    public static final class Deserializer extends JsonDeserializer<FeaturePredicate> {
        @Override
        public FeaturePredicate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            final JsonNode node = p.readValueAsTree();
            if (node == null) return null;
            final List<String> rules;
            if (node instanceof ArrayNode arrayNode) {
                rules = StreamSupport.stream(arrayNode.spliterator(), false).map(JsonNode::asText).toList();
            } else {
                rules = List.of(node.asText());
            }

            if (rules.size() == 1 && rules.get(0).equals("none")) {
                return new FeaturePredicate(Set.of(), Set.of(), false);
            }

            boolean wildcard = false;
            final Set<String> enabled = new HashSet<>();
            final Set<String> disabled = new HashSet<>();

            for (final String rule : rules) {
                if (rule.equals("*")) {
                    wildcard = true;
                } else if (rule.startsWith("-")) {
                    disabled.add(rule.substring(1).trim());
                } else if (rule.startsWith("+")) {
                    enabled.add(rule.substring(1).trim());
                }
            }

            return new FeaturePredicate(enabled, disabled, wildcard);
        }
    }
}
