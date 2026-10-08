package com.kubee.inventory.mcp.service;

import com.kubee.inventory.mcp.dto.ChatConversationDto;
import com.kubee.inventory.mcp.dto.ChatMessageDto;

import java.util.List;

public interface MCPService {

    ChatMessageDto processUserMessage(String userMessage, Long conversationId, String authToken);

    List<ChatConversationDto> getUserConversations();

    List<ChatMessageDto> getConversationMessages(Long conversationId);

}