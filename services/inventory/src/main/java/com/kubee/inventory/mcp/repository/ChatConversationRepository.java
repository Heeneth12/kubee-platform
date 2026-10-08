package com.kubee.inventory.mcp.repository;

import com.kubee.inventory.mcp.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    List<ChatConversation> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);

}