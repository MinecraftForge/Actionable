/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FunctionalInterfaces {
    public static <T> Command<T> wrap(ConsException<CommandContext<T>> consumer) {
        return context -> {
            try {
                consumer.accept(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return Command.SINGLE_SUCCESS;
        };
    }

    public static <T> Command<T> throwingCommand(CommandException<T> command) {
        return context -> {
            try {
                return command.run(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> Predicate<T> wrapPred(PredException<T> pred) {
        return context -> {
            try {
                return pred.test(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static void ignoreExceptions(RunnableException runnable) {
        try {
            runnable.run();
        } catch (IOException ignored) {}
    }

    public static <T> SupplierException<T> memoize(SupplierException<T> supplier) {
        return new SupplierException<>() {
            T value;
            @Override
            public T get() throws IOException {
                if (value == null) value = supplier.get();
                return value;
            }
        };
    }

    public static <T> Supplier<T> memoizeSup(Supplier<T> supplier) {
        return new Supplier<>() {
            T value;
            @Override
            public T get() {
                if (value == null) value = supplier.get();
                return value;
            }
        };
    }

    public interface ConsException<T> {
        void accept(T t) throws Exception;
    }

    public interface PredException<T> {
        boolean test(T t) throws Exception;
    }

    public interface RunnableException {
        void run() throws IOException;
    }

    public interface SupplierException<T> {
        T get() throws IOException;
    }

    @FunctionalInterface
    public interface CommandException<T> {
        int run(CommandContext<T> context) throws Exception;
    }
}
