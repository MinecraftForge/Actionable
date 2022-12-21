package net.minecraftforge.actionable.commands.lib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record EnumArgumentType<T extends Enum<T>>(StringArgumentType stringArg, EnumGetter<T> getter) implements ArgumentType<T> {
    private static final DynamicCommandExceptionType EXCEPTION = new DynamicCommandExceptionType(value -> new LiteralMessage("Unknown value " + value));

    public static <T extends Enum<T>> EnumArgumentType<T> lowerCaseEnum(StringArgumentType str, Class<T> clazz, Map<String, T> aliases) {
        final Map<String, T> values = new HashMap<>(aliases);
        for (T enumConstant : clazz.getEnumConstants()) {
            values.put(enumConstant.name().toLowerCase(Locale.ROOT), enumConstant);
        }
        return new EnumArgumentType<>(str, val -> values.get(val.toLowerCase(Locale.ROOT)));
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        final String stringVal = stringArg.parse(reader);
        final T val = getter.parse(stringVal);
        if (val == null) {
            throw EXCEPTION.createWithContext(reader, stringArg);
        }
        return val;
    }

    public interface EnumGetter<T> {
        @Nullable
        T parse(String val);
    }
}
