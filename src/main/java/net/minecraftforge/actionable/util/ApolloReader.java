/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.util;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer;
import com.apollographql.apollo.response.OperationResponseParser;
import okio.Okio;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ApolloReader {
    private final ResponseNormalizer<Map<String, Object>> normalizer;
    private final Function<Operation, ResponseFieldMapper> responseFieldMapper;
    private final ScalarTypeAdapters scalarTypeAdapters;

    public static ApolloReader ofClient(ApolloClient client) {
        return new ApolloReader(client.getApolloStore().networkResponseNormalizer(), Operation::responseFieldMapper, get(client, "scalarTypeAdapters"));
    }

    private static <T> T get(Object obj, String name) {
        try {
            final Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ApolloReader(ResponseNormalizer<Map<String, Object>> normalizer, Function<Operation, ResponseFieldMapper> responseFieldMapper, ScalarTypeAdapters scalarTypeAdapters) {
        this.normalizer = normalizer;
        this.responseFieldMapper = responseFieldMapper;
        this.scalarTypeAdapters = scalarTypeAdapters;
    }

    public Response<?> read(Operation operation, InputStream stream) throws IOException {
        final OperationResponseParser parser = new OperationResponseParser(operation, responseFieldMapper.apply(operation), scalarTypeAdapters, normalizer);
        return parser.parse(Okio.buffer(Okio.source(stream)));
    }
}
