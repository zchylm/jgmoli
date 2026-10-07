package com.aicyber.jgmoli.ai.provider;

import com.aicyber.jgmoli.ai.dto.ChatResponse;
import com.aicyber.jgmoli.ai.dto.ChatTurn;
import com.aicyber.jgmoli.ai.service.AiUnavailableException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeProviderTest {

    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsMessagesWithJgMoliInstructionsAndExtractsText() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> apiVersion = new AtomicReference<>();
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/messages", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            apiVersion.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
            requestBody.set(json.readTree(exchange.getRequestBody()));
            byte[] response = """
                    {"content":[{"type":"text","text":"A controller remains a core console match."}],"stop_reason":"end_turn"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ClaudeProvider provider = new ClaudeProvider("test-key", "claude-sonnet-5", baseUrl());
        ChatResponse answer = provider.answer("What works with Xbox for a $500 AUD budget?", List.of(
                new ChatTurn("user", "I want a more immersive setup"),
                new ChatTurn("assistant", "Which platform do you play on?")
        ), "<JG_MOLI_PUBLIC_CONTEXT>Current catalogue</JG_MOLI_PUBLIC_CONTEXT>");

        assertEquals("Bearer test-key", authorization.get());
        assertEquals("2023-06-01", apiVersion.get());
        assertEquals("claude-sonnet-5", requestBody.get().path("model").asText());
        assertEquals(4_096, requestBody.get().path("max_tokens").asInt());
        assertEquals("low", requestBody.get().path("output_config").path("effort").asText());
        assertEquals(3, requestBody.get().path("messages").size());
        assertEquals("What works with Xbox for a $500 AUD budget?", requestBody.get().path("messages").path(2).path("content").asText());
        String systemPrompt = requestBody.get().path("system").asText();
        assertTrue(systemPrompt.contains("official product and setup advisor for JG MOLI"));
        assertTrue(systemPrompt.contains("MOLI Cockpit, MOLI Racer and JG MOLI Arena"));
        assertTrue(systemPrompt.contains("Complete systems come first"));
        assertTrue(systemPrompt.contains("Do not force every visitor into the gear questionnaire"));
        assertTrue(systemPrompt.contains("personal immersive space for MOLI Cockpit"));
        assertTrue(systemPrompt.contains("dedicated motion racing for MOLI Racer"));
        assertTrue(systemPrompt.contains("shared family, group or venue play for JG MOLI Arena"));
        assertTrue(systemPrompt.contains("Platform compatibility is a hard constraint"));
        assertTrue(systemPrompt.contains("general knowledge are not proof of compatibility"));
        assertTrue(systemPrompt.contains("ask exactly one concise question about one decision at a time"));
        assertTrue(systemPrompt.contains("Never ask again for a known platform, goal, budget"));
        assertTrue(systemPrompt.contains("Console controls remain a platform-critical category"));
        assertTrue(systemPrompt.contains("If the visitor asks for one product, recommend at most one product"));
        assertTrue(systemPrompt.contains("Do not add generic offers"));
        assertTrue(systemPrompt.contains("Never ask for or repeat passwords"));
        assertTrue(systemPrompt.contains("<JG_MOLI_PUBLIC_CONTEXT>Current catalogue</JG_MOLI_PUBLIC_CONTEXT>"));
        assertTrue(systemPrompt.contains("visitor explicitly provided a budget"));
        assertEquals("A controller remains a core console match.", answer.body());
        assertEquals("claude", answer.source());
    }

    @Test
    void requiresApiKeyBeforeCallingProvider() {
        ClaudeProvider provider = new ClaudeProvider("", "claude-sonnet-5", "http://localhost:1");

        AiUnavailableException error = assertThrows(AiUnavailableException.class,
                () -> provider.answer("Hello", List.of(), "context"));

        assertEquals("MOLI AI is not configured yet", error.getMessage());
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
