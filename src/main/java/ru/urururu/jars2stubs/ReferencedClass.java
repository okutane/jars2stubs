package ru.urururu.jars2stubs;

import java.util.*;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public class ReferencedClass {
    final ReferencedLibrary parentLibrary;
    private final String name;
    private final Map<String, ReferencedField> referencedFields = new LinkedHashMap<>();
    private final Map<String, ReferencedMethod> referencedMethods = new LinkedHashMap<>();
    private final Map<String, ReferencedClass> referencedClasses = new LinkedHashMap<>();
    private ReferencedClass superclass;
    private ReferencedClass[] interfaces = new ReferencedClass[0];
    private final Set<Trait> traits = new LinkedHashSet<>();

    public ReferencedClass(ReferencedLibrary parentLibrary, String name) {
        this.parentLibrary = parentLibrary;
        this.name = name;
    }

    public ReferencedField referenceField(String name, String type) {
        return referencedFields.computeIfAbsent(name, k -> new ReferencedField(this, name, type));
    }

    public ReferencedMethod referenceMethod(String name, String type, boolean isInterface) {
        if (isInterface) {
            traits.add(Trait.Interface);
        }

        return referencedMethods.computeIfAbsent(methodKey(name, type), k -> new ReferencedMethod(this, name, type, isInterface));
    }

    public ReferencedClass referenceType(String name) {
        return referencedClasses.computeIfAbsent(name, k -> new ReferencedClass(parentLibrary, name));
    }

    public void referenceSuperclass(String superclassName, String... interfaceNames) {
        if (superclass != null) {
            throw new IllegalStateException("superclass already set");
        }

        superclass = parentLibrary.referenceType(superclassName);
        interfaces = Arrays.stream(interfaceNames).map(parentLibrary::referenceType).toArray(ReferencedClass[]::new);
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

    public Map<String, ReferencedClass> getReferencedClasses() {
        return referencedClasses;
    }

    public ReferencedClass getSuperclass() {
        return superclass;
    }

    public ReferencedClass[] getInterfaces() {
        return interfaces;
    }

    @Override
    public String toString() {
        return name + ':' + traits;
    }

    public String getSimpleName() {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    enum Trait {
        Interface,
        Exception,
        Implemented,
        ;
    }
}
