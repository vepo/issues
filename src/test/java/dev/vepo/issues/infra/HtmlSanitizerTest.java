package dev.vepo.issues.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class HtmlSanitizerTest {

    @Inject
    HtmlSanitizer htmlSanitizer;

    @Test
    void shouldStripScriptTags() {
        var cleaned = htmlSanitizer.sanitize("<p>Hello</p><script>alert(1)</script>");
        assertThat(cleaned).doesNotContain("script").doesNotContain("alert");
        assertThat(cleaned).contains("Hello");
    }

    @Test
    void shouldStripEventHandlers() {
        var cleaned = htmlSanitizer.sanitize("<img src=x onerror=alert(1)>");
        assertThat(cleaned).doesNotContain("onerror").doesNotContain("alert");
    }

    @Test
    void shouldStripJavascriptUrls() {
        var cleaned = htmlSanitizer.sanitize("<a href=\"javascript:alert(1)\">click</a>");
        assertThat(cleaned).doesNotContain("javascript:");
    }

    @Test
    void shouldKeepSafeFormatting() {
        var cleaned = htmlSanitizer.sanitize("<p><strong>Bold</strong> and <em>italic</em></p>");
        assertThat(cleaned).contains("strong").contains("em").contains("Bold");
    }
}
