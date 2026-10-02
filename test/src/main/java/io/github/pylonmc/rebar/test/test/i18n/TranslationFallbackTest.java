package io.github.pylonmc.rebar.test.test.i18n;

import io.github.pylonmc.rebar.i18n.RebarTranslator;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.SyncTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests per-key fallback from a regional locale to its language and then the
 * addon's default language (see pylonmc/rebar#591).
 */
public class TranslationFallbackTest extends SyncTest {

    private static final String PRESENT_BOTH = "rebartest.test.translation.present_both";
    private static final String DEFAULT_ONLY = "rebartest.test.translation.default_only";
    private static final String MISSING_EVERYWHERE = "rebartest.test.translation.missing_everywhere";
    private static final String PREFER_LANG = "rebartest.test.translation.prefer_lang";

    @Override
    protected void test() {
        RebarTranslator translator = RebarTranslator.getTranslatorForAddon(RebarTest.instance());

        // Key present in the matched locale file uses the locale value
        assertThat(render(translator, PRESENT_BOTH, Locale.SIMPLIFIED_CHINESE)).isEqualTo("中文值");

        // Key missing from the matched locale file falls back to the default language
        assertThat(render(translator, DEFAULT_ONLY, Locale.SIMPLIFIED_CHINESE)).isEqualTo("English fallback value");
        assertThat(translator.canTranslate(DEFAULT_ONLY, Locale.SIMPLIFIED_CHINESE)).isTrue();

        // Locale with no file at all keeps the file-level fallback behavior
        assertThat(render(translator, DEFAULT_ONLY, Locale.FRENCH)).isEqualTo("English fallback value");

        // Keys missing from the matched file resolve from the language-level file (zh)
        // before the default language
        assertThat(render(translator, PREFER_LANG, Locale.SIMPLIFIED_CHINESE)).isEqualTo("语言层优先值");

        // A variant locale resolves a missing key through the middle of the chain
        assertThat(render(translator, PRESENT_BOTH, Locale.of("zh", "CN", "POSIX"))).isEqualTo("中文值");

        // Default language itself is unaffected
        assertThat(render(translator, PRESENT_BOTH, Locale.ENGLISH)).isEqualTo("English value");

        // Key missing everywhere still reports as untranslatable
        assertThat(translator.canTranslate(MISSING_EVERYWHERE, Locale.SIMPLIFIED_CHINESE)).isFalse();
        assertThat(translator.translate(Component.translatable(MISSING_EVERYWHERE), Locale.SIMPLIFIED_CHINESE)).isNull();

        // Exact lookups bypass the fallback (used by missing-translation warnings)
        assertThat(translator.canTranslateExact(PRESENT_BOTH, Locale.SIMPLIFIED_CHINESE)).isTrue();
        assertThat(translator.canTranslateExact(DEFAULT_ONLY, Locale.SIMPLIFIED_CHINESE)).isFalse();
        assertThat(translator.canTranslateExact(DEFAULT_ONLY, Locale.ENGLISH)).isTrue();

        // Repeated lookups stay consistent
        assertThat(render(translator, DEFAULT_ONLY, Locale.SIMPLIFIED_CHINESE)).isEqualTo("English fallback value");

        // Exact lookups still report keys that only the language-level file has as missing
        assertThat(translator.canTranslateExact(PREFER_LANG, Locale.SIMPLIFIED_CHINESE)).isFalse();
        // The variant file itself is loaded and lacks the key
        assertThat(translator.canTranslateExact(PRESENT_BOTH, Locale.of("zh", "CN", "POSIX"))).isFalse();

        // Results stay consistent across reloads
        translator.reload();
        assertThat(render(translator, DEFAULT_ONLY, Locale.SIMPLIFIED_CHINESE)).isEqualTo("English fallback value");
        assertThat(render(translator, PRESENT_BOTH, Locale.SIMPLIFIED_CHINESE)).isEqualTo("中文值");
        assertThat(render(translator, PREFER_LANG, Locale.SIMPLIFIED_CHINESE)).isEqualTo("语言层优先值");
        assertThat(render(translator, PRESENT_BOTH, Locale.of("zh", "CN", "POSIX"))).isEqualTo("中文值");
    }

    private static String render(RebarTranslator translator, String key, Locale locale) {
        @Nullable Component component = translator.translate(Component.translatable(key), locale);
        assertThat(component).isNotNull();
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
