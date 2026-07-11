package dev.vepo.issues.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkiverse.mcp.server.Tool;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class IssuesMcpToolsTest {

    private static final Set<String> REQUIRED_TOOLS = Set.of(
                                                             "search_tickets",
                                                             "get_ticket_context",
                                                             "update_ticket",
                                                             "move_ticket",
                                                             "add_comment",
                                                             "list_projects");

    @Inject
    IssuesMcpTools issuesMcpTools;

    @Test
    void shouldExposeRequiredMcpTools() {
        assertNotNull(issuesMcpTools);

        var toolNames = Arrays.stream(IssuesMcpTools.class.getDeclaredMethods())
                              .filter(method -> method.isAnnotationPresent(Tool.class))
                              .map(this::toolName)
                              .collect(Collectors.toSet());

        assertEquals(REQUIRED_TOOLS, toolNames);
        assertTrue(toolNames.contains("get_ticket_context"));
        assertTrue(toolNames.contains("add_comment"));
    }

    private String toolName(Method method) {
        var tool = method.getAnnotation(Tool.class);
        if (tool.name() == null || tool.name().isBlank() || Tool.ELEMENT_NAME.equals(tool.name())) {
            return method.getName();
        }
        return tool.name();
    }
}
