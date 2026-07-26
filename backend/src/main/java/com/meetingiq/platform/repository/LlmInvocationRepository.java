package com.meetingiq.platform.repository;

import com.meetingiq.platform.domain.LlmInvocation;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Plain Spring Data CRUD for the LLM response cache / observability trail.
 * {@code _id} is the cache key, so {@link #findById} is the cache lookup and
 * {@link #save} is the cache write — see {@code LlmResponseCache}.
 */
public interface LlmInvocationRepository extends MongoRepository<LlmInvocation, String> {
}
