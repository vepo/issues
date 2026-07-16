package dev.vepo.issues.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.BadRequestException;

class UiLocaleTest {

    @Test
    void shouldMapAcceptLanguageToEnOrPt() {
        assertThat(UiLocale.fromAcceptLanguage("en-US,en;q=0.9")).isEqualTo("en");
        assertThat(UiLocale.fromAcceptLanguage("pt-BR")).isEqualTo("pt");
        assertThat(UiLocale.fromAcceptLanguage("fr-FR")).isEqualTo("pt");
        assertThat(UiLocale.fromAcceptLanguage(null)).isEqualTo("pt");
    }

    @Test
    void shouldRejectUnknownLocale() {
        assertThatThrownBy(() -> UiLocale.requireAllowed("fr")).isInstanceOf(BadRequestException.class);
        assertThat(UiLocale.requireAllowed("EN")).isEqualTo("en");
    }
}
