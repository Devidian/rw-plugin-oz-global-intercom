package de.omegazirkel.risingworld.globalintercom;

import java.util.Locale;

/** Maps legacy relay protocol codes to the JSON translation namespace. */
final class RelayTranslationKey {
    private RelayTranslationKey() {
    }

    static String fromCode(String code) {
        return code.toLowerCase(Locale.ROOT).replace('_', '.');
    }
}
