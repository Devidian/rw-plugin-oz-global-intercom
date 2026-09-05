package de.omegazirkel.risingworld.globalintercom;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TranslationKeysTest {
    @Test
    public void allMessageAndRelayKeysExistInEveryLanguage() throws Exception {
        Set<String> keys = new HashSet<>();
        Pattern keyPattern = Pattern.compile("\"((?:msg|state|cmd|bc)\\.[a-z.]+|RELAY_[A-Z_]+)\"");
        Pattern legacyPattern = Pattern.compile("\"(?:MSG|STATE|CMD|BC)_[A-Z_]+\"");
        try (var sources = Files.walk(Path.of("src/de"))) {
            for (Path source : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source).replaceAll("(?m)//.*$", "");
                assertFalse(source + " uses legacy translation keys", legacyPattern.matcher(text).find());
                var matcher = keyPattern.matcher(text);
                while (matcher.find()) keys.add(RelayTranslationKey.fromCode(matcher.group(1)));
            }
        }
        // Info responses are delivered dynamically by the relay.
        keys.add("relay.info.registered");
        keys.add("relay.info.unregistered");
        assertTrue(keys.contains("msg.join"));
        assertTrue(keys.contains("msg.leave"));
        for (String lang : new String[] { "en", "de", "fr", "ru" }) {
            JsonObject translations = JsonParser.parseString(Files.readString(Path.of("src/i18n", lang + ".json"))).getAsJsonObject();
            for (String key : keys) {
                var value = (com.google.gson.JsonElement) translations;
                for (String segment : key.split("\\.")) {
                    assertTrue(lang + ": missing " + key, value.isJsonObject() && value.getAsJsonObject().has(segment));
                    value = value.getAsJsonObject().get(segment);
                }
                if (value.isJsonObject()) value = value.getAsJsonObject().get("$value");
                assertTrue(lang + ": empty " + key, value != null && value.isJsonPrimitive() && !value.getAsString().isBlank());
            }
        }
    }

    @Test
    public void relayCodesAreMappedIndependentlyOfServerLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals("relay.join.success", RelayTranslationKey.fromCode("RELAY_JOIN_SUCCESS"));
            assertEquals("relay.info.registered", RelayTranslationKey.fromCode("RELAY_INFO_REGISTERED"));
            assertEquals("relay.leave.success", RelayTranslationKey.fromCode("relay.leave.success"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
