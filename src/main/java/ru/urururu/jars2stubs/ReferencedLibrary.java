package ru.urururu.jars2stubs;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public class ReferencedLibrary {
    Map<String, ReferencedClass> referencedClasses = new TreeMap<>();

    public ReferencedClass referenceType(String className) {
        if (className.startsWith("[")) {
            throw new IllegalArgumentException(className);
        }
        if (className.endsWith("[]")) {
            return referenceType(className.substring(0, className.length() - 2));
        }

        int nestedClassSeparator = className.lastIndexOf('$');
        if (nestedClassSeparator != -1) {
            return referenceType(className.substring(0, nestedClassSeparator)).referenceType(className.substring(nestedClassSeparator + 1));
        }

        return referencedClasses.computeIfAbsent(className, name -> new ReferencedClass(this, name));
    }

    public Map<String, ReferencedClass> getReferencedClasses() {
        return referencedClasses;
    }
}
