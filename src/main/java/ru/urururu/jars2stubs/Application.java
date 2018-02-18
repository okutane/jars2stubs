package ru.urururu.jars2stubs;

import com.sun.org.apache.bcel.internal.classfile.*;
import com.sun.org.apache.bcel.internal.classfile.Deprecated;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public class Application {
    Set<String> visitedClasses = new LinkedHashSet<String>();
    Set<String> usedClasses = new LinkedHashSet<String>();

    public static void main(String... args) throws IOException {
        File root = new File(".");

        Application application = new Application();
        application.run(root);
    }

    private void run(File root) throws IOException {
        scan(root);
        System.out.println("visitedClasses.size() = " + visitedClasses.size());
        System.out.println("visitedClasses = " + visitedClasses);
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

    private void scanClass(JavaClass javaClass) {
        visitedClasses.add(javaClass.getClassName());

        //javaClass.getMethods()[0].accept();

        javaClass.getConstantPool().accept(new DescendingVisitor(javaClass, new Visitor() {
            public void visitCode(Code obj) {

            }

            public void visitCodeException(CodeException obj) {

            }

            public void visitConstantClass(ConstantClass obj) {
                visitedClasses.add(obj.toString());
            }

            public void visitConstantDouble(ConstantDouble obj) {

            }

            public void visitConstantFieldref(ConstantFieldref obj) {

            }

            public void visitConstantFloat(ConstantFloat obj) {

            }

            public void visitConstantInteger(ConstantInteger obj) {

            }

            public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj) {

            }

            public void visitConstantLong(ConstantLong obj) {

            }

            public void visitConstantMethodref(ConstantMethodref obj) {

            }

            public void visitConstantNameAndType(ConstantNameAndType obj) {

            }

            public void visitConstantPool(ConstantPool obj) {

            }

            public void visitConstantString(ConstantString obj) {

            }

            public void visitConstantUtf8(ConstantUtf8 obj) {

            }

            public void visitConstantValue(ConstantValue obj) {

            }

            public void visitDeprecated(Deprecated obj) {

            }

            public void visitExceptionTable(ExceptionTable obj) {

            }

            public void visitField(Field obj) {

            }

            public void visitInnerClass(InnerClass obj) {

            }

            public void visitInnerClasses(InnerClasses obj) {

            }

            public void visitJavaClass(JavaClass obj) {

            }

            public void visitLineNumber(LineNumber obj) {

            }

            public void visitLineNumberTable(LineNumberTable obj) {

            }

            public void visitLocalVariable(LocalVariable obj) {

            }

            public void visitLocalVariableTable(LocalVariableTable obj) {

            }

            public void visitLocalVariableTypeTable(LocalVariableTypeTable obj) {

            }

            public void visitMethod(Method obj) {

            }

            public void visitSignature(Signature obj) {

            }

            public void visitSourceFile(SourceFile obj) {

            }

            public void visitSynthetic(Synthetic obj) {

            }

            public void visitUnknown(Unknown obj) {

            }

            public void visitStackMap(StackMap obj) {

            }

            public void visitStackMapEntry(StackMapEntry obj) {

            }
        }));
    }
}