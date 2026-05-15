package com.resumepipeline.render;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class LatexRenderer {

    private final LatexEscaper escaper;

    public LatexRenderer(LatexEscaper escaper) {
        this.escaper = escaper;
    }

    /**
     * Fills the template by replacing {{PLACEHOLDER}} tokens with escaped values.
     * Values are passed through LatexEscaper.escape(). Pass already-rendered LaTeX
     * fragments via the {@link #renderRaw} variant instead.
     */
    public String render(String templateClasspath, Map<String, String> values) {
        String tex = readTemplate(templateClasspath);
        for (var entry : values.entrySet()) {
            String token = "{{" + entry.getKey() + "}}";
            tex = tex.replace(token, escaper.escape(entry.getValue()));
        }
        return tex;
    }

    /** Like render() but values are inserted verbatim (caller is responsible for escaping). */
    public String renderRaw(String templateClasspath, Map<String, String> values) {
        String tex = readTemplate(templateClasspath);
        for (var entry : values.entrySet()) {
            String token = "{{" + entry.getKey() + "}}";
            tex = tex.replace(token, entry.getValue() == null ? "" : entry.getValue());
        }
        return tex;
    }

    private String readTemplate(String classpath) {
        try (var in = getClass().getClassLoader().getResourceAsStream(classpath)) {
            if (in == null) throw new IllegalArgumentException("Template not found: " + classpath);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template " + classpath, e);
        }
    }
}
