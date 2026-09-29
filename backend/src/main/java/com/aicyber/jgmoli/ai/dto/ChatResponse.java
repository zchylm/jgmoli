package com.aicyber.jgmoli.ai.dto;

import java.util.List;

public record ChatResponse(String title, String body, List<String> bullets, String source) {
}
