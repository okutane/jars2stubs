package ru.urururu.jars2stubs;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public class ReferencedField {
    private final ReferencedClass parentClass;
    private final String name;
    private final String type;
    private final Map<String, Boolean> traits = new HashMap<>();

    public ReferencedField(ReferencedClass parentClass, String name, String type) {
        this.parentClass = parentClass;
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getFlags() {
        if (parentClass.getTraits().contains(ReferencedClass.Trait.Interface)) {
            return "";
        }

        return "public";
    }

    public String getInitializer() {
        return "null";
    }

    public void referenceTrait(String name, boolean value) {
        Boolean old = traits.put(name, value);
        if (old != null && old != value) {
            throw new IllegalArgumentException(this.name + " " + name);
        }
    }

    public Map<String, Boolean> getTraits() {
        return traits;
    }
}
