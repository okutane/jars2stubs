package ru.urururu.jars2stubs;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

/**
 * @author <a href="mailto:dmatveev@roox.ru">Dmitry Matveev</a>
 */
public class ClassDumper {
    private static TypeName defaultExceptionSuperclass = ClassName.get(Exception.class);

    public static void dump(Map<String, File> outs, ReferencedClass clazz) {
        outs.entrySet().stream().filter(e -> clazz.getName().startsWith(e.getKey())).findFirst().ifPresent(e -> dump(e.getValue(), clazz));
    }

    public static void dump(File out, ReferencedClass clazz) {
        String[] parts = clazz.getName().split("\\.");
        for (String part : parts) {
            out.mkdirs();
            out = new File(out, part);
        }
        out = new File(out.getAbsolutePath() + ".java");

        if (out.exists()) {
            System.err.println(out + " exists");
            //return;
        }

        int classSeparatorPos = clazz.getName().lastIndexOf('.');
        String packageName = clazz.getName().substring(0, classSeparatorPos);
        String simpleName = clazz.getName().substring(classSeparatorPos + 1);

        JavaFile.Builder javaFile = JavaFile.builder(packageName, buildType(clazz, simpleName, null).addModifiers(Modifier.PUBLIC).build());

        try (Writer stringWriter = Files.newBufferedWriter(out.toPath(), Charset.forName("UTF-8"))) {
            javaFile.build().writeTo(stringWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getDefaultValue(String typeName) {
        switch (typeName) {
            case "boolean":
                return "false";
            case "char":
            case "float":
            case "double":
            case "byte":
            case "short":
            case "int":
            case "long":
                return "0";
            default:
                return "null";
        }
    }

    private static TypeSpec.Builder buildType(ReferencedClass clazz, String simpleName, ReferencedClass parent) {
        TypeSpec.Builder typeSpec;
        if (clazz.getTraits().contains(ReferencedClass.Trait.Interface)) {
            typeSpec = TypeSpec.interfaceBuilder(simpleName);
        } else {
            typeSpec = TypeSpec.classBuilder(simpleName);
        }

        if (clazz.getTraits().contains(ReferencedClass.Trait.Exception)) {
            typeSpec.superclass(defaultExceptionSuperclass);
        } else if (clazz.getSuperclass() != null) {
            typeSpec.superclass(toTypeName(clazz.getSuperclass().getName()));
        } else {
            // we don't have impl for that clazz, superclass and interfaces should be inferred from called methods / used fields
        }
        Arrays.stream(clazz.getInterfaces()).forEach(iface -> typeSpec.addSuperinterface(toTypeName(iface.getName())));

        for (ReferencedMethod referencedMethod : clazz.getReferencedMethods().values()) {
            MethodSpec.Builder methodSpec;
            if (referencedMethod.getName().equals("<init>")) {
                methodSpec = MethodSpec.constructorBuilder();
            } else {
                methodSpec = MethodSpec.methodBuilder(referencedMethod.getName()).returns(toTypeName(referencedMethod.getReturnType()));
            }

            if (clazz.getTraits().contains(ReferencedClass.Trait.Interface)) {
                methodSpec.addModifiers(Modifier.ABSTRACT);
            } else {
                if (!referencedMethod.getName().equals("<init>")) {
                    if (referencedMethod.getTraits().getOrDefault("static", false)) {
                        methodSpec.addModifiers(Modifier.STATIC);
                    }

                    CodeBlock.Builder builder = CodeBlock.builder();
                    builder.add("//todo generated\n");
                    switch (referencedMethod.getReturnType()) {
                        case "void":
                            break;
                        default:
                            builder.addStatement("return " + getDefaultValue(referencedMethod.getReturnType()));
                    }
                    methodSpec.addCode(builder.build());
                }
            }

            methodSpec.addModifiers(Modifier.PUBLIC);

            int i = 0;
            for (String parameterType : referencedMethod.getParameterTypes()) {
                methodSpec.addParameter(toTypeName(parameterType), "arg" + i++);
            }

            typeSpec.addMethod(methodSpec.build());
        }
        for (ReferencedField referencedField : clazz.getReferencedFields().values()) {
            FieldSpec.Builder fieldSpec = FieldSpec.builder(toTypeName(referencedField.getType()), referencedField.getName());
            fieldSpec.addModifiers(Modifier.PUBLIC);

            if (clazz.getTraits().contains(ReferencedClass.Trait.Interface)) {
                fieldSpec.addModifiers(Modifier.FINAL);
                fieldSpec.initializer(getDefaultValue(referencedField.getType()));
            }

            if (referencedField.getTraits().getOrDefault("static", false) && parent == null) {
                fieldSpec.addModifiers(Modifier.STATIC);
            }

            typeSpec.addField(fieldSpec.build());
        }
        for (ReferencedClass inner : clazz.getReferencedClasses().values()) {
            typeSpec.addType(buildType(inner, inner.getSimpleName(), clazz).build());
        }

        if (parent != null && parent.getTraits().contains(ReferencedClass.Trait.Interface)) {
            typeSpec.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        }

        typeSpec.addModifiers(Modifier.PUBLIC);

        return typeSpec;
    }

    private static TypeName toTypeName(String codeName) {
        if (codeName.endsWith("[]")) {
            return ArrayTypeName.of(toTypeName(codeName.substring(0, codeName.length() - 2)));
        }

        switch (codeName) {
            case "void":
                return TypeName.VOID;
            case "boolean":
                return TypeName.BOOLEAN;
            case "byte":
                return TypeName.BYTE;
            case "short":
                return TypeName.SHORT;
            case "int":
                return TypeName.INT;
            case "long":
                return TypeName.LONG;
            case "char":
                return TypeName.CHAR;
            case "float":
                return TypeName.FLOAT;
            case "double":
                return TypeName.DOUBLE;
            default:
                int lastIndexOf = codeName.lastIndexOf('.');

                if (lastIndexOf == -1) {
                    throw new IllegalStateException(codeName);
                }

                String packageName = codeName.substring(0, lastIndexOf);
                String[] simpleName = codeName.substring(lastIndexOf + 1).split("\\$");
                return ClassName.get(packageName, simpleName[0], Arrays.copyOfRange(simpleName, 1, simpleName.length));
        }
    }
}
