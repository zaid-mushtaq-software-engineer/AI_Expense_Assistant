package com.expenseassistant.conversation.repository;

import com.expenseassistant.conversation.entity.Conversation;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}