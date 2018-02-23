package ru.urururu.jars2stubs;

import com.sun.org.apache.bcel.internal.classfile.Utility;

import java.util.*;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public class ReferencedMethod {
    private final ReferencedClass parentClass;
    private final String name;
    private final String signature;
    private final String returnType;
    private final List<String> parameterTypes = new ArrayList<>();

    static final Set<Character> primitives = new HashSet<>(Arrays.asList(
             'B','C','D','F','I','J','S','Z','V'
    ));

    public ReferencedMethod(ReferencedClass parentClass, String name, String signature) {
        this.parentClass = parentClass;
        this.name = name;
        this.signature = signature;

        int parametersEnd = signature.indexOf(')');

        int parameterPos = 1;
        int parameterStart = 1;
        while (parameterPos < parametersEnd) {
            char c = signature.charAt(parameterPos);
            if (primitives.contains(c)) {
                int parameterEnd = parameterPos + 1;
                parameterTypes.add(Utility.signatureToString(signature.substring(parameterStart, parameterEnd), false));
                parameterPos = parameterStart = parameterEnd;
            } else if (c == 'L') {
                int parameterEnd = signature.indexOf(';', parameterPos) + 1;
                parameterTypes.add(Utility.signatureToString(signature.substring(parameterStart, parameterEnd), false));
                parameterPos = parameterStart = parameterEnd;
            } else if (c == '[') {
                parameterPos++;
            } else {
                throw new IllegalStateException("Not supported: " + c);
            }
        }

        returnType = Utility.signatureToString(signature.substring(parametersEnd + 1), false);
    }

    public String getFlags() {
        return "public";
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }
}
