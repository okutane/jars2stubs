package ru.urururu.jars2stubs;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:dmatveev@roox.ru">Dmitry Matveev</a>
 */
public class ClassDumper {
    public static void dump(File out, ReferencedClass clazz) {
        String[] parts = clazz.getName().split("\\.");
        for (String part : parts) {
            out.mkdirs();
            out = new File(out, part);
        }

        if (out.exists()) {
            throw new IllegalStateException(out + " exists");
        }

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty(RuntimeConstants.ENCODING_DEFAULT, "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();

        Template template = velocityEngine.getTemplate("template.java");

        StringWriter stringWriter = new StringWriter();

        Map ctx = new HashMap();
        int classSeparatorPos = clazz.getName().lastIndexOf('.');
        ctx.put("package", clazz.getName().substring(0, classSeparatorPos));
        ctx.put("classKind", "class");
        ctx.put("className", clazz.getName().substring(classSeparatorPos + 1));
        ctx.put("fields", clazz.getReferencedFields().values());
        ctx.put("methods", clazz.getReferencedMethods().values());

        VelocityContext context = new VelocityContext(ctx);
        template.merge(context, stringWriter);

        stringWriter.toString();
    }
}
