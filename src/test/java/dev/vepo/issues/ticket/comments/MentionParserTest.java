package dev.vepo.issues.ticket.comments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MentionParserTest {

    @Test
    @DisplayName("It should extract a single @username mention")
    void shouldExtractSingleMentionTest() {
        assertThat(MentionParser.extractUsernames("Thanks @project-manager for reviewing!")).containsExactly("project-manager");
    }

    @Test
    @DisplayName("It should extract multiple distinct mentions without duplicates")
    void shouldExtractMultipleDistinctMentionsTest() {
        assertThat(MentionParser.extractUsernames("cc @user and @project-manager, also @user again")).containsExactlyInAnyOrder("user",
                                                                                                                                "project-manager");
    }

    @Test
    @DisplayName("It should not treat an email address as a mention")
    void shouldIgnoreEmailAddressTest() {
        assertThat(MentionParser.extractUsernames("Contact me at pm@issues.vepo.dev for details")).isEmpty();
    }

    @Test
    @DisplayName("It should stop a mention token at sentence punctuation")
    void shouldStopMentionAtPunctuationTest() {
        assertThat(MentionParser.extractUsernames("cc @user, please check @project-manager.")).containsExactlyInAnyOrder("user",
                                                                                                                         "project-manager");
    }

    @Test
    @DisplayName("It should return an empty set when there is no mention")
    void shouldReturnEmptyWhenNoMentionTest() {
        assertThat(MentionParser.extractUsernames("No mentions in this comment.")).isEmpty();
    }

    @Test
    @DisplayName("It should return an empty set for blank or null content")
    void shouldReturnEmptyForBlankOrNullContentTest() {
        assertThat(MentionParser.extractUsernames("")).isEmpty();
        assertThat(MentionParser.extractUsernames("   ")).isEmpty();
        assertThat(MentionParser.extractUsernames(null)).isEmpty();
    }

    @Test
    @DisplayName("It should extract a mention at the very start of the content")
    void shouldExtractMentionAtStartTest() {
        assertThat(MentionParser.extractUsernames("@user please take a look")).containsExactly("user");
    }
}
