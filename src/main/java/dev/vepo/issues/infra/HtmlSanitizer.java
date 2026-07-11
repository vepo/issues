package dev.vepo.issues.infra;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HtmlSanitizer {

    private final PolicyFactory policy = new HtmlPolicyBuilder().allowElements("a",
                                                                               "b",
                                                                               "blockquote",
                                                                               "br",
                                                                               "code",
                                                                               "em",
                                                                               "h1",
                                                                               "h2",
                                                                               "h3",
                                                                               "i",
                                                                               "li",
                                                                               "ol",
                                                                               "p",
                                                                               "pre",
                                                                               "strong",
                                                                               "ul")
                                                                .allowUrlProtocols("http", "https", "mailto")
                                                                .allowAttributes("href")
                                                                .onElements("a")
                                                                .requireRelNofollowOnLinks()
                                                                .toFactory();

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return policy.sanitize(html);
    }
}
