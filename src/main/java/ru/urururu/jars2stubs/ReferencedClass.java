package ru.urururu.jars2stubs;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public class ReferencedClass {
    private final String name;
    private final Map<String, ReferencedField> referencedFields = new LinkedHashMap<>();
    private final Map<String, ReferencedMethod> referencedMethods = new LinkedHashMap<>();
    private final Set<Trait> traits = new LinkedHashSet<>();

    public ReferencedClass(String name) {
        this.name = name;
    }

    public void referenceField(String name, String type) {
        referencedFields.put(name, new ReferencedField(this, name, type));
    }

    public ReferencedMethod referenceMethod(String name, String type, boolean isInterface) {
        if (isInterface) {
            traits.add(Trait.Interface);
        }

        return referencedMethods.computeIfAbsent(methodKey(name, type), k -> new ReferencedMethod(this, name, type));
    }

    private String methodKey(String name, String type) {
        return name + type;
    }

    public String getName() {
        return name;
    }

    public Set<Trait> getTraits() {
        return traits;
    }

    public Map<String, ReferencedMethod> getReferencedMethods() {
        return referencedMethods;
    }

    public Map<String, ReferencedField> getReferencedFields() {
        return referencedFields;
    }

    @Override
    public String toString() {
        return name + ':' + traits;
    }

    enum Trait {
        Interface
    }
}
