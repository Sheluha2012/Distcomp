package com.example.entitiesapp.service

import com.example.entitiesapp.dto.CommentRequestTo
import com.example.entitiesapp.dto.CommentResponseTo
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class CommentService(private val discussionClient: RestClient) {

    fun getAll(): List<CommentResponseTo> = discussionClient.get()
        .uri("/comments")
        .retrieve()
        .body(object : ParameterizedTypeReference<List<CommentResponseTo>>() {}) ?: emptyList()

    fun getById(id: Long): CommentResponseTo = discussionClient.get()
        .uri("/comments/$id")
        .retrieve()
        .body(CommentResponseTo::class.java)!!

    fun create(dto: CommentRequestTo): CommentResponseTo = discussionClient.post()
        .uri("/comments")
        .contentType(MediaType.APPLICATION_JSON)
        .body(dto)
        .retrieve()
        .body(CommentResponseTo::class.java)!!

    fun update(id: Long, dto: CommentRequestTo): CommentResponseTo = discussionClient.put()
        .uri("/comments/$id")
        .body(dto)
        .retrieve()
        .body(CommentResponseTo::class.java)!!

    fun delete(id: Long) {
        discussionClient.delete().uri("/comments/$id").retrieve().toBodilessEntity()
    }

    fun deleteByStory(storyId: Long) {
        discussionClient.delete().uri("/comments/story/$storyId").retrieve().toBodilessEntity()
    }
}