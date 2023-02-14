/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.event;

import com.fasterxml.jackson.databind.JsonNode;
import net.minecraftforge.actionable.Main;
import net.minecraftforge.actionable.util.Jsons;
import net.minecraftforge.actionable.util.enums.Action;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubAccessor;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class ByActionEventHandler<T extends Record> implements EventHandler {
    protected final Class<T> payloadType;
    protected final Consumer<T> preAction;
    protected final Map<Action, Handler<T>> handlers = new EnumMap<>(Action.class);

    protected ByActionEventHandler(Class<T> payloadType, Consumer<T> preAction, Consumer<Registrar<T>> registrar) {
        this.payloadType = payloadType;
        this.preAction = preAction;
        registrar.accept(new Registrar<>() {
            @Override
            public Registrar<T> register(Action action, Handler<T> handler) {
                handlers.put(action, handler);
                return this;
            }
        });
    }

    @Override
    public final void handle(Main.GitHubGetter gitHubGetter, JsonNode payload) throws Throwable {
        final Handler<T> handler = handlers.get(Action.get(payload));
        if (handler != null) {
            final GitHub gitHub = gitHubGetter.get();
            final T payloadT = Jsons.read(GitHubAccessor.objectReader(gitHub), payload, payloadType);
            preAction.accept(payloadT);
            handler.handle(
                    gitHub, payloadT, payload
            );
        }
    }

    public interface Handler<T> {
        void handle(GitHub gitHub, T payload, JsonNode payloadJson) throws Throwable;
    }

    public interface JsonlessHandler<T> {
        void handle(GitHub gitHub, T payload) throws Throwable;
    }

    public interface Registrar<T> {
        Registrar<T> register(Action action, Handler<T> handler);

        default Registrar<T> register(Action action, JsonlessHandler<T> handler) {
            return register(action, (gitHub, payload, payloadJson) -> handler.handle(gitHub, payload));
        }

        @SuppressWarnings("unchecked")
        default Registrar<T> register(Action action, Handler<T>... handlers) {
            this.register(action, (gitHub, payload, payloadJson) -> {
                for (final Handler<T> handler : handlers) {
                    handler.handle(gitHub, payload, payloadJson);
                }
            });
            return this;
        }
    }
}
