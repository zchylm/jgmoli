package com.aicyber.jgmoli.ai.provider;

import com.aicyber.jgmoli.ai.dto.ChatResponse;
import com.aicyber.jgmoli.ai.dto.ChatTurn;

import java.util.List;

public interface LlmProvider {

    ChatResponse answer(String message, List<ChatTurn> history, String knowledgeContext);
}
