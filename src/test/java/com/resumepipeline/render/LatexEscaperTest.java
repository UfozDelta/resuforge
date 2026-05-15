package com.resumepipeline.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatexEscaperTest {

    private final LatexEscaper esc = new LatexEscaper();
    private static final String NBSP = " ";

    @Test void plainText()  { assertEquals("hello world", esc.escape("hello world")); }
    @Test void nullInput()  { assertEquals("", esc.escape(null)); }

    @Test void percent()    { assertEquals("50\\%",  esc.escape("50%")); }
    @Test void ampersand()  { assertEquals("R\\&D",  esc.escape("R&D")); }
    @Test void underscore() { assertEquals("user\\_id", esc.escape("user_id")); }
    @Test void hash()       { assertEquals("\\#1",   esc.escape("#1")); }
    @Test void dollar()     { assertEquals("\\$5",   esc.escape("$5")); }
    @Test void braces()     { assertEquals("\\{x\\}", esc.escape("{x}")); }
    @Test void tilde()      { assertEquals("a\\textasciitilde{}b", esc.escape("a~b")); }
    @Test void caret()      { assertEquals("x\\textasciicircum{}2", esc.escape("x^2")); }

    @Test void backslash() {
        assertEquals("path\\textbackslash{}to", esc.escape("path\\to"));
    }

    @Test void backslashDoesNotCascade() {
        assertEquals("a\\textbackslash{}\\&b", esc.escape("a\\&b"));
    }

    @Test void allSpecialsTogether() {
        assertEquals(
            "\\% \\& \\_ \\# \\$ \\textasciitilde{} \\textasciicircum{} \\textbackslash{} \\{ \\}",
            esc.escape("% & _ # $ ~ ^ \\ { }")
        );
    }

    @Test void url() {
        assertEquals(
            "https://x.com/path?a=1\\&b=2\\#frag",
            esc.escape("https://x.com/path?a=1&b=2#frag")
        );
    }

    @Test void unicodeQuotes() {
        assertEquals("``hello''", esc.escape("“hello”"));
    }

    @Test void unicodeSingleQuotes() {
        assertEquals("it`s `quoted'", esc.escape("it‘s ‘quoted’"));
    }

    @Test void emDash()     { assertEquals("a---b", esc.escape("a—b")); }
    @Test void enDash()     { assertEquals("2024--2026", esc.escape("2024–2026")); }
    @Test void ellipsis()   { assertEquals("wait\\ldots{}", esc.escape("wait…")); }

    @Test void nbspBecomesTilde() {
        assertEquals("Mr.~Smith", esc.escape("Mr." + NBSP + "Smith"));
    }

    @Test void regularSpaceUnchanged() {
        assertEquals("a b c", esc.escape("a b c"));
    }
}
