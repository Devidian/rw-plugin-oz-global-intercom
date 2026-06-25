package de.omegazirkel.risingworld.globalintercom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatShortcutParserTest {
    @Test
    public void parsesScreenshotShortcutAtEndOfMessage() {
        ChatShortcutParser.Result result = ChatShortcutParser.parse("look here +s");

        assertEquals("look here \uD83D\uDDBC\uFE0F", result.message());
        assertTrue(result.screenshotWithGui());
        assertFalse(result.screenshotWithoutGui());
    }

    @Test
    public void parsesScreenshotNoGuiShortcutAtEndOfMessage() {
        ChatShortcutParser.Result result = ChatShortcutParser.parse("look here +sng");

        assertEquals("look here \uD83D\uDDBC\uFE0F", result.message());
        assertFalse(result.screenshotWithGui());
        assertTrue(result.screenshotWithoutGui());
    }

    @Test
    public void parsesLongScreenshotNoGuiShortcut() {
        ChatShortcutParser.Result result = ChatShortcutParser.parse("look here +screennogui");

        assertEquals("look here \uD83D\uDDBC\uFE0F", result.message());
        assertFalse(result.screenshotWithGui());
        assertTrue(result.screenshotWithoutGui());
    }

    @Test
    public void ignoresShortcutPrefixInsideNormalText() {
        ChatShortcutParser.Result result = ChatShortcutParser.parse("keep +screenshot text");

        assertEquals("keep +screenshot text", result.message());
        assertFalse(result.hasScreenshot());
    }
}
