package ru.urururu.jars2stubs;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public class ReferencedField {
    private final ReferencedClass parentClass;
    private final String name;
    private final String type;

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
}
