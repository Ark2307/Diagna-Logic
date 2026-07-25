package com.diagna.logic.repository;

import com.diagna.logic.domain.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Plain Spring Data CRUD for persisted RAG chat threads. */
public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {

    List<ChatConversation> findByMeetingIdOrderByUpdatedAtDesc(String meetingId);
}
