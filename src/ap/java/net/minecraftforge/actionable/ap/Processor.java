/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.actionable.ap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraftforge.actionable.annotation.Argument;
import net.minecraftforge.actionable.annotation.Command;
import net.minecraftforge.actionable.annotation.Generated;
import net.minecraftforge.actionable.annotation.Value;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("net.minecraftforge.actionable.annotation.Command")
public class Processor extends AbstractProcessor {
    private NameUtils names;
    private TypeMirror enumType;
    private TypeMirror requireType;
    private final Map<TypeMirror, Map<String, String>> enumValues = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.names = new NameUtils(processingEnv.getTypeUtils(), processingEnv.getElementUtils());

        this.enumType = names.types.erasure(processingEnv.getElementUtils().getTypeElement("java.lang.Enum").asType());
        this.requireType = processingEnv.getElementUtils().getTypeElement("net.minecraftforge.actionable.annotations.Require").asType();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) return false;

        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Command.class);
        final List<ExecutableElement> toGenerate = elements.stream()
                .filter(it -> it.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(it -> it.getAnnotation(Generated.class) == null).toList();
        if (toGenerate.isEmpty()) return false;

        final Path templatesPath = Path.of(processingEnv.getOptions().get("templatesLocation"));
        try {
            final String packageName = "net.minecraftforge.actionable.commands";
            final String className = packageName + ".CommandRegistrar";
            final ClassWriter cw = new ClassWriter(className);
            final ImportController.AndNames imports = cw.imports().new AndNames(names);

            cw.annotate(Generated.class, "");
            cw.enterClass(Set.of(Modifier.PUBLIC, Modifier.FINAL), "CommandRegistrar", Set.of(), Set.of());

            cw.bodyWriter().print("public static void registerCommands(org.kohsuke.github.GitHub gh, " + imports.getTypeName(CommandDispatcher.class) + "<net.minecraftforge.actionable.commands.lib.GHCommandContext> dispatcher)")
                    .enterScopeln();
            toGenerate.forEach(mtd -> cw.bodyWriter().println("register$" + mtd.getSimpleName() + "(gh, dispatcher);"));
            cw.bodyWriter().exitScopeln();

            final Map<String, StringBuilder> docs = new HashMap<>();
            docs.put("misc", new StringBuilder().append("# Misc Commands\n"));
            docs.put("pr_management", new StringBuilder().append("# PR Management Commands\n"));
            docs.put("issue_management", new StringBuilder().append("# Issue Management Commands\n"));

            final Map<String, String> requirements = new HashMap<>();
            for (final var method : toGenerate) {
                writeRegister(requirements::put, docs::get, cw, imports, method);
            }

            cw.bodyWriter()
                .pr("    public static <T> com.mojang.brigadier.builder.RequiredArgumentBuilder<net.minecraftforge.actionable.commands.lib.GHCommandContext, T> argument(final String name, final com.mojang.brigadier.arguments.ArgumentType<T> type) {\n" +
                        "        return com.mojang.brigadier.builder.RequiredArgumentBuilder.argument(name, type);\n" +
                        "    }\n");

            cw.exitClass();

            final JavaFileObject builderFile = processingEnv.getFiler()
                    .createSourceFile(className);
            try (final PrintWriter pr = new PrintWriter(builderFile.openWriter())) {
                cw.write(pr);
            }

            for (final var entry : docs.entrySet()) {
                write("docs/commands/available_commands/" + entry.getKey() + ".md", entry.getValue());
            }

            final StringBuilder arguments = new StringBuilder();
            writeEnums(arguments);

            write("docs/commands/arguments.md", Files.readString(templatesPath.resolve("arguments.md")).replace("<enums>", arguments.toString()));

            final StringBuilder reqs = new StringBuilder()
                    .append("# Command Requirements\n")
                    .append("Below you can find a list of requirements a command may have. A command with requirements will only run when it meets all its requirements.  \n");
            requirements.forEach((name, desc) -> reqs.append("### `").append(name).append("`\n")
                    .append(desc).append("  \n"));
            write("docs/commands/requirements.md", reqs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    private void write(String path, Object text) throws IOException {
        try (final PrintWriter writer = new PrintWriter(processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", path).openWriter())) {
            writer.print(text);
        }
    }

    private void writeEnums(StringBuilder docsPage) {
        final Set<String> enumsWrote = new HashSet<>();
        for (final TypeMirror enumValue : enumValues.keySet()) {
            final TypeElement type = (TypeElement) names.types.asElement(enumValue);
            if (!enumsWrote.add(type.getQualifiedName().toString())) continue;

            docsPage.append("### `").append(type.getSimpleName()).append("`\n");
            docsPage.append("Possible values:  \n");
            type.getEnclosedElements().stream()
                    .filter(it -> it.getKind() == ElementKind.ENUM_CONSTANT)
                    .forEach(element -> {
                        final Value annotation = element.getAnnotation(Value.class);
                        if (annotation == null) return;
                        docsPage.append("- ")
                                .append(Stream.of(annotation.names())
                                        .map(s -> "`" + s + "`")
                                        .collect(Collectors.joining(", ")))
                                .append(": ")
                                .append(annotation.description())
                                .append("  \n");
                    });
        }
    }

    private void writeRegister(BiConsumer<String, String> requirements, Function<String, StringBuilder> docBuilders, ClassWriter cw, ImportController.AndNames imports, ExecutableElement method) {
        final Command cmd = method.getAnnotation(Command.class);
        final StringBuilder docsBuilder = docBuilders.apply(cmd.category());
        docsBuilder.append("## `").append(cmd.name()[0]).append("`").append("\n");
        docsBuilder.append(cmd.description()).append("  \n\n");
        if (cmd.name().length > 1) {
            docsBuilder.append("> Aliases: ").append(Stream.of(Arrays.copyOfRange(cmd.name(), 1, cmd.name().length))
                    .map(it -> "`" + it + "`").collect(Collectors.joining(", "))).append("  \n");
        }

        final StringBuilder paramsDesc = new StringBuilder();
        docsBuilder.append("> Syntax: `").append("/").append(cmd.name()[0]);
        List<String> syntaxArgs = new ArrayList<>();
        boolean hadPreviouslyOptional = false;
        for (final VariableElement var : method.getParameters()) {
            final Argument arg = var.getAnnotation(Argument.class);
            if (arg == null) continue;
            if (arg.optional() || !arg.defaultValue().isBlank()) {
                syntaxArgs.add("[" + var.getSimpleName() + "]");
                hadPreviouslyOptional = true;
            } else {
                if (hadPreviouslyOptional) {
                    syntaxArgs.add("[<" + var.getSimpleName() + ">]");
                } else {
                    syntaxArgs.add("<" + var.getSimpleName() + ">");
                }
            }
            writeParamDesc(arg, paramsDesc, var);
        }
        if (!syntaxArgs.isEmpty()) docsBuilder.append(" ");
        docsBuilder.append(String.join(" ", syntaxArgs)).append("`  \n\n");
        if (!paramsDesc.isEmpty()) {
            docsBuilder.append("Parameters:  \n").append(paramsDesc).append("  \n");
        }

        final Optional<String> require = method.getAnnotationMirrors().stream().filter(it -> names.types.isSameType(requireType, it.getAnnotationType().asElement().asType()))
                .findFirst().map(annotation -> {
                    @SuppressWarnings("unchecked") final var requirement = (List<AnnotationValue>) annotation.getElementValues().entrySet().stream()
                            .filter(it -> it.getKey().getSimpleName().toString().equals("value"))
                            .findFirst().map(Map.Entry::getValue).orElseThrow().getValue();
                    docsBuilder.append("> Requirements: ").append(requirement.stream().map(it -> {
                        final String name = ((VariableElement) it.getValue()).getSimpleName()
                                .toString().toLowerCase(Locale.ROOT);
                        return "[`" + name + "`](../requirements#" + name + ")";
                    }).collect(Collectors.joining(", "))).append("  \n\n");
                    StringBuilder req = null;
                    for (final AnnotationValue val : requirement) {
                        final VariableElement var = (VariableElement) val.getValue();
                        final String ref = imports.getTypeName(var.getEnclosingElement().asType()) + "." + var.getSimpleName();

                        final Value value = var.getAnnotation(Value.class);
                        if (value != null) {
                            requirements.accept(var.getSimpleName().toString().toLowerCase(Locale.ROOT), value.description());
                        }

                        if (req == null) req = new StringBuilder(ref);
                        else req.append(".and(").append(ref).append(")");
                    }
                    return ".requires(" + req + ")";
                });

        final Writer writer = cw.bodyWriter();

        final String commandCtx = cw.imports().considerImport("net.minecraftforge.actionable.commands.lib.GHCommandContext");
        writer.print("public static void register$" + cmd.name()[0] + "(org.kohsuke.github.GitHub gh, " + imports.getTypeName(CommandDispatcher.class) + "<" + commandCtx + "> dispatcher)")
                .enterScopeln();

        final String registerCode = "dispatcher.register(" + imports.getTypeName(LiteralArgumentBuilder.class) + ".<" + commandCtx + ">literal(\"" + cmd.name()[0] + "\")";
        writer.println(cmd.name().length == 1 ? registerCode : ("final var root = " + registerCode));

        require.ifPresent(requires -> writer.increaseIndent(4).println(requires).decreaseIndent(4));

        int indentableParams = 0;
        final List<String> parameters = new ArrayList<>();

        final Map<String, String> optionalParams = method.getParameters().stream()
                .filter(it -> it.getAnnotation(Argument.class) != null && !it.getAnnotation(Argument.class).defaultValue().isBlank())
                .collect(Collectors.toMap(it -> it.getSimpleName().toString(), parameter -> {
                    final String value = parameter.getAnnotation(Argument.class).defaultValue();
                    if (names.types.isSubtype(parameter.asType(), enumType)) {
                        return imports.getTypeName(parameter.asType()) + "." + getEnumMapping(parameter.asType()).getOrDefault(value, value);
                    }
                    return value;
                }));
        final boolean isThrowing = !method.getThrownTypes().isEmpty();
        final boolean returnsInt = method.getReturnType().getKind() == TypeKind.INT;
        final Function<String, String> execute = isThrowing || returnsInt ?
                s -> cw.imports().considerImport("net.minecraftforge.actionable.util.FunctionalInterfaces") + "." + (returnsInt ? "throwingCommand" : "wrap") + "(" + s + ")" : Function.identity();

        for (int i = 0; i < method.getParameters().size(); i++) {
            final var param = method.getParameters().get(i);

            final String paramType = names.getTypeName(param.asType());
            switch (paramType) {
                case "com.mojang.brigadier.context.CommandContext" -> {
                    parameters.add("ctx");
                    continue;
                }
                case "net.minecraftforge.actionable.commands.lib.GHCommandContext" -> {
                    parameters.add("ctx.getSource()");
                    continue;
                }
                case "org.kohsuke.github.GHPullRequest" -> {
                    parameters.add("ctx.getSource().pullRequest()");
                    continue;
                }
                case "org.kohsuke.github.GitHub" -> {
                    parameters.add("ctx.getSource().gitHub()");
                    continue;
                }
                case "org.kohsuke.github.GHIssue" -> {
                    parameters.add("ctx.getSource().issue()");
                    continue;
                }
            }

            indentableParams++;
            final Argument annotation = param.getAnnotation(Argument.class);

            if (!annotation.defaultValue().isBlank() || annotation.optional()) {
                // Assume everything before is optional
                writer.increaseIndent(4).println(".executes(" + execute.apply("ctx -> " + cw.imports()
                                .considerImport(names.getTypeName(method.getEnclosingElement().asType())) + "." +
                                method.getSimpleName() + "(" + Stream.concat(
                                parameters.stream(),
                                method.getParameters().subList(i, method.getParameters().size()).stream()
                                        .map(p -> optionalParams.getOrDefault(p.getSimpleName().toString(), null))
                        ).collect(Collectors.joining(", ")) + "))"))
                        .decreaseIndent(4);
            }

            writer.increaseIndent(4);
            writeArgument(paramType, annotation, param, imports, cw);
            parameters.add("ctx.getArgument(\"" + param.getSimpleName() + "\", " + cw.imports().considerImport(paramType) + ".class)");
        }
        writer.increaseIndent(4).println(".executes(" + execute.apply("ctx -> " + cw.imports()
                        .considerImport(names.getTypeName(method.getEnclosingElement().asType())) + "." +
                        method.getSimpleName() + "(" + String.join(", ", parameters) + "))"))
                .decreaseIndent(4);

        writer.decreaseIndent(4 * indentableParams);

        writer.println(")".repeat(indentableParams + 1) + ";");

        for (int i = 1; i < cmd.name().length; i++) {
            writer.println("dispatcher.register(" + imports.getTypeName(LiteralArgumentBuilder.class) + ".<" + commandCtx + ">literal(\"" + cmd.name()[i] + "\")");
            writer.increaseIndent(4);
            require.ifPresent(writer::println);
            writer.println(".redirect(root)");
            writer.decreaseIndent(4);
            writer.println(");");
        }

        writer.exitScopeln();
    }

    private void writeArgument(String paramType, Argument annotation, VariableElement parameter, ImportController.AndNames imports, ClassWriter cw) {
        final var writer = cw.bodyWriter().print(".then(argument(" + "\"" + parameter.getSimpleName() + "\", ");
        switch (paramType) {
            case "int", "java.lang.Integer" -> writer.pr(imports.getTypeName(IntegerArgumentType.class) + ".integer()");
            case "java.lang.String" -> writer.pr(imports.getTypeName(StringArgumentType.class) + "." + annotation.stringType() + "()");
            case "org.kohsuke.github.GHRepository" -> writer.pr(cw.imports().considerImport("net.minecraftforge.actionable.commands.lib.gh.RepoArgumentType") + ".inCurrentOrg(gh)");
            default -> {
                if (names.types.isSubtype(parameter.asType(), enumType)) {
                    writer.pr(cw.imports().considerImport("net.minecraftforge.actionable.commands.lib.EnumArgumentType")
                            + ".enumArg(" + imports.getTypeName(StringArgumentType.class) + "." + annotation.stringType() + "(), "
                            + cw.imports().considerImport(paramType) + ".class)");
                    getEnumMapping(parameter.asType());
                }
            }
        }
        writer.pr(")").println();
    }

    private void writeParamDesc(Argument annotation, StringBuilder builder, VariableElement param) {
        final TypeElement asElem = (TypeElement) names.types.asElement(param.asType());
        final String simpleName = asElem == null ? param.asType().toString() : asElem.getSimpleName().toString();
        final String withLink = names.types.isSubtype(param.asType(), enumType) ?
                "[" + simpleName + "](../arguments#" + simpleName.toLowerCase(Locale.ROOT) + ")" : simpleName;
        builder.append("- `").append(param.getSimpleName()).append("` (")
                .append(withLink).append("): ").append(annotation.description()).append("  \n");
    }

    private Map<String, String> getEnumMapping(TypeMirror Enum) {
        return enumValues.computeIfAbsent(Enum, $ -> {
            final Map<String, String> map = new HashMap<>();
            names.types.asElement(Enum).getEnclosedElements()
                .stream().filter(it -> it.getKind() == ElementKind.ENUM_CONSTANT)
                .forEach(element -> {
                    final String fname = element.getSimpleName().toString();
                    final Value an = element.getAnnotation(Value.class);
                    if (an != null) {
                        for (String name : an.names()) {
                            map.put(name, fname);
                        }
                    }
                });
            return map;
        });
    }

    private static void considerCopying(ClassWriter cw, Class<? extends Annotation> annotation, String value) {
        if (value != null) {
            annotateString(cw, annotation, value);
        }
    }

    private static void annotateString(ClassWriter cw, Class<? extends Annotation> annotation, String value) {
        cw.annotate(annotation, "\"" + value.replace("\"", "\\\"") + "\"");
    }
}
