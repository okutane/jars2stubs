package ru.urururu.jars2stubs;

import com.sun.org.apache.bcel.internal.classfile.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public class Application {
    Set<String> visitedClasses = new LinkedHashSet<String>();
    Map<String, ReferencedClass> referencedClasses = new TreeMap<>();
    Set<String> localVarClasses = new LinkedHashSet<>();
    Set<String> usedClasses = new LinkedHashSet<String>();

    public static void main(String... args) throws IOException {
        File in = new File(args[0]);
        File out = new File(args[1]);

        Application application = new Application();
        application.run(in, out);
    }

    private void run(File root, File out) throws IOException {
        scan(root);
        visitedClasses.forEach(clazz -> referencedClasses.remove(clazz));

        System.out.println("referencedClasses.size() = " + referencedClasses.size());

        LinkedHashSet<String> onlyVarTypes = new LinkedHashSet<>(localVarClasses);
        onlyVarTypes.removeAll(referencedClasses.keySet());
        System.out.println("onlyVarTypes.size() = " + onlyVarTypes.size());

        referencedClasses.values().forEach(clazz -> ClassDumper.dump(out, clazz));
    }

    private void scan(File root) throws IOException {
        for (File file : root.listFiles()) {
            if (file.isDirectory()) {
                scan(file);
            } else if (file.getName().endsWith(".jar")) {
                scanJar(file);
            }
        }
    }

    private void scanJar(File file) throws IOException {
        JarFile jar = new JarFile(file);
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            // Is this a class?
            if (entry.getName().endsWith(".class")) {
                ClassParser parser = new ClassParser(file.getAbsolutePath(), entry.getName());
                JavaClass javaClass = parser.parse();
                scanClass(javaClass);
            }
        }
    }

    private void scanClass(final JavaClass javaClass) {
        visitedClasses.add(javaClass.getClassName());

        javaClass.accept(new DescendingVisitor(javaClass, new EmptyVisitor() {
            @Override
            public void visitConstantMethodref(ConstantMethodref obj) {
                String className = obj.getClass(javaClass.getConstantPool());
                ConstantNameAndType nameAndType = (ConstantNameAndType) javaClass.getConstantPool().getConstant(obj.getNameAndTypeIndex());
                String name = nameAndType.getName(javaClass.getConstantPool());
                String type = nameAndType.getSignature(javaClass.getConstantPool());
                referenceType(className).referenceMethod(name, type, false);
                super.visitConstantMethodref(obj);
            }

            @Override
            public void visitLocalVariable(LocalVariable obj) {
                String signature = obj.getSignature();
                try {
                    String type = Utility.signatureToString(signature, false);
                    localVarClasses.add(type);
                    //referenceType(type);
                } catch (ClassFormatException e) {
                    return;
                }
                super.visitLocalVariable(obj);
            }

            @Override
            public void visitCode(Code obj) {
                obj.getConstantPool().accept(new DescendingVisitor(null, this));
            }

            @Override
            public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj) {
                String className = obj.getClass(javaClass.getConstantPool());
                ConstantNameAndType nameAndType = (ConstantNameAndType) javaClass.getConstantPool().getConstant(obj.getNameAndTypeIndex());
                String name = nameAndType.getName(javaClass.getConstantPool());
                String type = nameAndType.getSignature(javaClass.getConstantPool());
                referenceType(className).referenceMethod(name, type, true);
            }
        }));
    }

    private ReferencedClass referenceType(String className) {
        return referencedClasses.computeIfAbsent(className, ReferencedClass::new);
    }
}