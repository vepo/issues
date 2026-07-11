package dev.vepo.issues.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Red-phase tests for {@link PlainTextLength} — strip HTML tags then count
 * characters (FQ3).
 */
class PlainTextLengthTest {

    @ParameterizedTest
    @CsvSource({ "hello, 5", "<b>hello</b>, 5", "<p>hi</p>, 2", "<p><b>hello</b></p>, 5", "plain text, 10"
    })
    @DisplayName("Should count plain characters after stripping HTML tags")
    void shouldCountPlainCharactersAfterStrippingTags(String htmlOrText, int expectedLength) {
        assertThat(PlainTextLength.of(htmlOrText)).isEqualTo(expectedLength);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t", "\n" })
    @DisplayName("Should return zero for null or blank input")
    void shouldReturnZeroForNullOrBlank(String htmlOrText) {
        assertThat(PlainTextLength.of(htmlOrText)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should ignore nested tags when counting plain text")
    void shouldIgnoreNestedTagsWhenCountingPlainText() {
        assertThat(PlainTextLength.of("<div><span>ab</span><i>cd</i></div>")).isEqualTo(4);
    }
}
