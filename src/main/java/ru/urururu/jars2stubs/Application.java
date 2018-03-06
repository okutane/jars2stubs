package ru.urururu.jars2stubs;

import com.sun.org.apache.bcel.internal.Constants;
import com.sun.org.apache.bcel.internal.classfile.*;
import com.sun.org.apache.bcel.internal.generic.Type;
import com.sun.org.apache.bcel.internal.util.ByteSequence;

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
    Set<String> localVarClasses = new LinkedHashSet<>();
    Set<String> usedClasses = new LinkedHashSet<String>();
    ReferencedLibrary referencedLibrary = new ReferencedLibrary();

    public static void main(String... args) throws IOException {
        File in = new File(args[0]);
        LinkedHashMap<String, File> outs = new LinkedHashMap<>();
        for (int i = 1; i + 1 < args.length; i += 2) {
            outs.put(args[i], new File(args[i + 1]));
        }

        Application application = new Application();
        application.run(in, outs);
    }

    private void run(File root, Map<String, File> outs) throws IOException {
        scan(root);

        System.out.println("referencedClasses.size() = " + referencedLibrary.getReferencedClasses().size());

        LinkedHashSet<String> onlyVarTypes = new LinkedHashSet<>(localVarClasses);
        onlyVarTypes.removeAll(referencedLibrary.getReferencedClasses().keySet());
        System.out.println("onlyVarTypes.size() = " + onlyVarTypes.size());

        // todo post process referenced, but not found virtual methods!
        referencedLibrary.getReferencedClasses().entrySet().stream().filter(e -> visitedClasses.contains(e.getKey())).map(Map.Entry::getValue).forEach(clazz -> {
            clazz.getReferencedFields().values().stream().filter(f -> !Boolean.TRUE.equals(f.getTraits().get("implemented"))).forEach(f -> {
                ReferencedClass currentClass = clazz;
                while (currentClass.getSuperclass() != null) {
                    ReferencedClass superclass = currentClass.getSuperclass();
                    ReferencedField superField = superclass.referenceField(f.getName(), f.getType());
                    f.getTraits().forEach(superField::referenceTrait);
                    if (Boolean.TRUE.equals(superField.getTraits().get("implemented"))) {
                        return;
                    }
                    currentClass = superclass;
                }
            });
            clazz.getReferencedMethods().values().stream().filter(m -> !Boolean.TRUE.equals(m.getTraits().get("implemented"))).forEach(m -> {
                ReferencedClass currentClass = clazz;
                while (currentClass.getSuperclass() != null) {
                    ReferencedClass superclass = currentClass.getSuperclass();
                    ReferencedMethod superMethod = superclass.referenceMethod(m.getName(), m.getSignature(), m.isInterface());
                    m.getTraits().forEach(superMethod::referenceTrait);
                    if (Boolean.TRUE.equals(superMethod.getTraits().get("implemented"))) {
                        return;
                    }
                    currentClass = superclass;
                }
            });
        });

        referencedLibrary.getReferencedClasses().entrySet().stream().filter(e -> !visitedClasses.contains(e.getKey())).map(Map.Entry::getValue).forEach(clazz -> ClassDumper.dump(outs, clazz));
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
            boolean wide = false;

            @Override
            public void visitJavaClass(JavaClass obj) {
                ReferencedClass referencedClass = referencedLibrary.referenceType(obj.getClassName());

                referencedClass.referenceSuperclass(obj.getSuperclassName(), obj.getInterfaceNames());

                referencedClass.getTraits().add(ReferencedClass.Trait.Implemented);

                String[] interfaceNames = obj.getInterfaceNames();
                for (String interfaceName : interfaceNames) {
                    referencedLibrary.referenceType(interfaceName).getTraits().add(ReferencedClass.Trait.Interface);
                }
                super.visitJavaClass(obj);
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
            public void visitExceptionTable(ExceptionTable obj) {
                String[] exceptionNames = obj.getExceptionNames();
                for (String exceptionName : exceptionNames) {
                    referencedLibrary.referenceType(exceptionName).getTraits().add(ReferencedClass.Trait.Exception);
                }
                super.visitExceptionTable(obj);
            }

            @Override
            public void visitCodeException(CodeException obj) {
                super.visitCodeException(obj);
            }

            @Override
            public void visitMethod(Method obj) {
                ReferencedMethod referencedMethod = referencedLibrary.referenceType(javaClass.getClassName()).referenceMethod(obj.getName(), obj.getSignature(), false);
                referencedMethod.referenceTrait("implemented", true);

                Type returnType = obj.getReturnType();
                referencedLibrary.referenceType(returnType.toString());
                Type[] argumentTypes = obj.getArgumentTypes();
                for (Type argumentType : argumentTypes) {
                    referencedLibrary.referenceType(argumentType.toString());
                }
                super.visitMethod(obj);
            }

            @Override
            public void visitField(Field obj) {
                ReferencedField referencedField = referencedLibrary.referenceType(javaClass.getClassName()).referenceField(obj.getName(), obj.getSignature());
                referencedField.referenceTrait("implemented", true);
            }

            @Override
            public void visitCode(Code obj) {
                visitInstructions(obj.getCode(), obj.getConstantPool(), 0, -1);

                obj.getConstantPool().accept(new DescendingVisitor(null, this));
            }

            public void visitNextInstruction(ByteSequence bytes, ConstantPool constantPool, boolean verbose)
                    throws IOException {
                short opcode = (short) bytes.readUnsignedByte();
                int default_offset = 0, low, high, npairs;
                int index, vindex, constant;
                int[] match, jump_table;
                int no_pad_bytes = 0, offset;

                /* Special case: Skip (0-3) padding bytes, i.e., the
                 * following bytes are 4-byte-aligned
                 */
                if ((opcode == Constants.TABLESWITCH) || (opcode == Constants.LOOKUPSWITCH)) {
                    int remainder = bytes.getIndex() % 4;
                    no_pad_bytes = (remainder == 0) ? 0 : 4 - remainder;

                    for (int i = 0; i < no_pad_bytes; i++) {
                        byte b;

                        if ((b = bytes.readByte()) != 0)
                            System.err.println("Warning: Padding byte != 0 in " +
                                    Constants.OPCODE_NAMES[opcode] + ":" + b);
                    }

                    // Both cases have a field default_offset in common
                    default_offset = bytes.readInt();
                }

                switch (opcode) {
                    /* Table switch has variable length arguments.
                     */
                    case Constants.TABLESWITCH:
                        low = bytes.readInt();
                        high = bytes.readInt();

                        offset = bytes.getIndex() - 12 - no_pad_bytes - 1;

                        jump_table = new int[high - low + 1];
                        for (int i = 0; i < jump_table.length; i++) {
                            jump_table[i] = offset + bytes.readInt();
                        }

                        break;

                    /* Lookup switch has variable length arguments.
                     */
                    case Constants.LOOKUPSWITCH: {
                        npairs = bytes.readInt();
                        offset = bytes.getIndex() - 8 - no_pad_bytes - 1;

                        match = new int[npairs];
                        jump_table = new int[npairs];

                        for (int i = 0; i < npairs; i++) {
                            match[i] = bytes.readInt();

                            jump_table[i] = offset + bytes.readInt();
                        }
                    }
                    break;

                    /* Two address bytes + offset from start of byte stream form the
                     * jump target
                     */
                    case Constants.GOTO:
                    case Constants.IFEQ:
                    case Constants.IFGE:
                    case Constants.IFGT:
                    case Constants.IFLE:
                    case Constants.IFLT:
                    case Constants.JSR:
                    case Constants.IFNE:
                    case Constants.IFNONNULL:
                    case Constants.IFNULL:
                    case Constants.IF_ACMPEQ:
                    case Constants.IF_ACMPNE:
                    case Constants.IF_ICMPEQ:
                    case Constants.IF_ICMPGE:
                    case Constants.IF_ICMPGT:
                    case Constants.IF_ICMPLE:
                    case Constants.IF_ICMPLT:
                    case Constants.IF_ICMPNE:
                        bytes.readShort();
                        break;

                    /* 32-bit wide jumps
                     */
                    case Constants.GOTO_W:
                    case Constants.JSR_W:
                        bytes.readInt();
                        break;

                    /* Index byte references local variable (register)
                     */
                    case Constants.ALOAD:
                    case Constants.ASTORE:
                    case Constants.DLOAD:
                    case Constants.DSTORE:
                    case Constants.FLOAD:
                    case Constants.FSTORE:
                    case Constants.ILOAD:
                    case Constants.ISTORE:
                    case Constants.LLOAD:
                    case Constants.LSTORE:
                    case Constants.RET:
                        if (wide) {
                            vindex = bytes.readUnsignedShort();
                            wide = false; // Clear flag
                        } else {
                            vindex = bytes.readUnsignedByte();
                        }

                        break;

                    /*
                     * Remember wide byte which is used to form a 16-bit address in the
                     * following instruction. Relies on that the method is called again with
                     * the following opcode.
                     */
                    case Constants.WIDE:
                        wide = true;
                        break;

                    /* Array of basic type.
                     */
                    case Constants.NEWARRAY:
                        bytes.readByte();
                        break;

                    /* Access object/class fields.
                     */
                    case Constants.GETFIELD:
                    case Constants.GETSTATIC:
                    case Constants.PUTFIELD:
                    case Constants.PUTSTATIC:
                        index = bytes.readUnsignedShort();
                    {
                        ConstantFieldref fieldRef = (ConstantFieldref) constantPool.getConstant(index, Constants.CONSTANT_Fieldref);

                        String className = fieldRef.getClass(javaClass.getConstantPool());
                        ConstantNameAndType nameAndType = (ConstantNameAndType) javaClass.getConstantPool().getConstant(fieldRef.getNameAndTypeIndex());
                        String name = nameAndType.getName(javaClass.getConstantPool());
                        String type = nameAndType.getSignature(javaClass.getConstantPool());

                        ReferencedField referencedField = referencedLibrary.referenceType(className).referenceField(name, Utility.signatureToString(type, false));

                        if (opcode == Constants.GETSTATIC || opcode == Constants.PUTSTATIC) {
                            referencedField.referenceTrait("static", true);
                        } else {
                            referencedField.referenceTrait("static", false);
                        }
                        if (opcode == Constants.GETFIELD || opcode == Constants.GETSTATIC) {
                            referencedField.referenceTrait("readable", true);
                        }
                        if (opcode == Constants.PUTFIELD || opcode == Constants.PUTSTATIC) {
                            referencedField.referenceTrait("writable", true);
                        }
                    }

                    break;

                    /* Operands are references to classes in constant pool
                     */
                    case Constants.NEW:
                    case Constants.CHECKCAST:
                    case Constants.INSTANCEOF:
                        index = bytes.readUnsignedShort();
                        ConstantClass s = (ConstantClass) constantPool.getConstant(index, Constants.CONSTANT_Class);
                        Object constantValue = s.getConstantValue(constantPool);
                        String constantValue1 = (String) constantValue;
                        if (constantValue1.startsWith("L")) {
                            String s1 = Utility.signatureToString(constantValue1, false);
                        }
                        if (constantValue1.startsWith("[")) {
                            constantValue1 = Utility.signatureToString(constantValue1, false);
                        }
                        referencedLibrary.referenceType(constantValue1.replace('/', '.'));
                        break;

                    /* Operands are references to methods in constant pool
                     */
                    case Constants.INVOKESPECIAL:
                    case Constants.INVOKESTATIC:
                    case Constants.INVOKEVIRTUAL:
                        index = bytes.readUnsignedShort();

                    {
                        ConstantMethodref obj = (ConstantMethodref) constantPool.getConstant(index, Constants.CONSTANT_Methodref);
                        String className = obj.getClass(javaClass.getConstantPool());
                        ConstantNameAndType nameAndType = (ConstantNameAndType) javaClass.getConstantPool().getConstant(obj.getNameAndTypeIndex());
                        String name = nameAndType.getName(javaClass.getConstantPool());
                        String type = nameAndType.getSignature(javaClass.getConstantPool());
                        ReferencedMethod referencedMethod = referencedLibrary.referenceType(className).referenceMethod(name, type, false);

                        if (opcode == Constants.INVOKESTATIC) {
                            referencedMethod.referenceTrait("static", true);
                        } else {
                            referencedMethod.referenceTrait("static", false);
                        }
                    }
                    break;

                    case Constants.INVOKEINTERFACE:
                        index = bytes.readUnsignedShort();
                        bytes.readUnsignedByte(); // historical, redundant
                        bytes.readUnsignedByte();

                    {
                        ConstantInterfaceMethodref obj = (ConstantInterfaceMethodref) constantPool.getConstant(index, Constants.CONSTANT_InterfaceMethodref);
                        String className = obj.getClass(javaClass.getConstantPool());
                        ConstantNameAndType nameAndType = (ConstantNameAndType) javaClass.getConstantPool().getConstant(obj.getNameAndTypeIndex());
                        String name = nameAndType.getName(javaClass.getConstantPool());
                        String type = nameAndType.getSignature(javaClass.getConstantPool());
                        referencedLibrary.referenceType(className).referenceMethod(name, type, true);
                    }
                    break;

                    /* Operands are references to items in constant pool
                     */
                    case Constants.LDC_W:
                    case Constants.LDC2_W:
                        index = bytes.readUnsignedShort();
                        break;

                    case Constants.LDC:
                        index = bytes.readUnsignedByte();
                        break;

                    /* Array of references.
                     */
                    case Constants.ANEWARRAY:
                        index = bytes.readUnsignedShort();
                        break;

                    /* Multidimensional array of references.
                     */
                    case Constants.MULTIANEWARRAY: {
                        index = bytes.readUnsignedShort();
                        int dimensions = bytes.readUnsignedByte();
                    }
                    break;

                    /* Increment local variable.
                     */
                    case Constants.IINC:
                        if (wide) {
                            vindex = bytes.readUnsignedShort();
                            constant = bytes.readShort();
                            wide = false;
                        } else {
                            vindex = bytes.readUnsignedByte();
                            constant = bytes.readByte();
                        }
                        break;

                    default:
                        if (Constants.NO_OF_OPERANDS[opcode] > 0) {
                            for (int i = 0; i < Constants.TYPE_OF_OPERANDS[opcode].length; i++) {
                                switch (Constants.TYPE_OF_OPERANDS[opcode][i]) {
                                    case Constants.T_BYTE:
                                        bytes.readByte();
                                        break;
                                    case Constants.T_SHORT:
                                        bytes.readShort();
                                        break;
                                    case Constants.T_INT:
                                        bytes.readInt();
                                        break;

                                    default: // Never reached
                                        System.err.println("Unreachable default case reached!");
                                }
                            }
                        }
                }
            }

            public void visitInstructions(byte[] code, ConstantPool constant_pool, int index, int length) {
                ByteSequence stream = new ByteSequence(code);

                try {
                    for (int i = 0; i < index; i++) // Skip `index' lines of code
                        visitNextInstruction(stream, constant_pool, false);

                    for (int i = 0; stream.available() > 0; i++) {
                        if ((length < 0) || (i < length)) {
                            visitNextInstruction(stream, constant_pool, false);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new ClassFormatException("Byte code error: " + e);
                }
            }

            @Override
            public void visitConstantMethodref(ConstantMethodref obj) {
                String className = obj.getClass(javaClass.getConstantPool());
                ConstantNameAndType nameAndType = (ConstantNameAndType) javaClass.getConstantPool().getConstant(obj.getNameAndTypeIndex());
                String name = nameAndType.getName(javaClass.getConstantPool());
                String type = nameAndType.getSignature(javaClass.getConstantPool());
                referencedLibrary.referenceType(className).referenceMethod(name, type, false);
            }

            @Override
            public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj) {
                String className = obj.getClass(javaClass.getConstantPool());
                ConstantNameAndType nameAndType = (ConstantNameAndType) javaClass.getConstantPool().getConstant(obj.getNameAndTypeIndex());
                String name = nameAndType.getName(javaClass.getConstantPool());
                String type = nameAndType.getSignature(javaClass.getConstantPool());
                referencedLibrary.referenceType(className).referenceMethod(name, type, true);
            }

            @Override
            public void visitConstantFieldref(ConstantFieldref obj) {
                String className = obj.getClass(javaClass.getConstantPool());
                ConstantNameAndType nameAndType = (ConstantNameAndType) javaClass.getConstantPool().getConstant(obj.getNameAndTypeIndex());
                String name = nameAndType.getName(javaClass.getConstantPool());
                String type = nameAndType.getSignature(javaClass.getConstantPool());
                referencedLibrary.referenceType(className).referenceField(name, Utility.signatureToString(type, false));
            }
        }));
    }
}