package io.github.chenyilei2016.maintain.manager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAssistantServiceTest {
    @Test
    void extractsCompatibleChatCompletionAndResponsesContent() {
        assertEquals("risk review", AiAssistantService.extractContent(
                "{\"choices\":[{\"message\":{\"content\":\"risk review\"}}]}"));
        assertEquals("generated script", AiAssistantService.extractContent(
                "{\"output_text\":\"generated script\"}"));
    }
}
