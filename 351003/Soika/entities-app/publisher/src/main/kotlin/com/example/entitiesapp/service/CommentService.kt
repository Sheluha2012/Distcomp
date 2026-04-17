package com.example.entitiesapp.service

import com.example.entitiesapp.dto.CommentRequestTo
import com.example.entitiesapp.dto.CommentResponseTo
import com.example.entitiesapp.dto.CommentState
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class CommentService(
    private val replyingKafkaTemplate: ReplyingKafkaTemplate<String, String, String>,
    private val objectMapper: ObjectMapper
) {
    private val inTopic = "InTopic"

    private fun sendRequest(op: String, value: Any?, key: String? = null): String {
        val jsonValue = if (value is String) value else objectMapper.writeValueAsString(value)
        val record = ProducerRecord<String, String>(inTopic, key, jsonValue)
        record.headers().add("op", op.toByteArray())

        val reply = replyingKafkaTemplate.sendAndReceive(record)
        return reply.get().value()
    }

    fun getAll(): List<CommentResponseTo> {
        val responseJson = sendRequest("GET_ALL", "EMPTY")
        return objectMapper.readValue(responseJson)
    }

    fun getById(id: Long): CommentResponseTo {
        val responseJson = sendRequest("GET_BY_ID", id.toString())
        return objectMapper.readValue(responseJson)
    }

    fun create(dto: CommentRequestTo): CommentResponseTo {
        val generatedId = Math.abs(Random.nextLong(1000000))
        val pendingDto = dto.copy(id = generatedId, state = CommentState.PENDING)

        val responseJson = sendRequest("CREATE", pendingDto, dto.storyId.toString())
        return objectMapper.readValue(responseJson)
    }

    fun update(id: Long, dto: CommentRequestTo): CommentResponseTo {
        val responseJson = sendRequest("UPDATE", dto.copy(id = id), dto.storyId.toString())
        return objectMapper.readValue(responseJson)
    }

    fun delete(id: Long) {
        sendRequest("DELETE", id.toString())
    }

    fun deleteByStory(storyId: Long) {
        sendRequest("DELETE_BY_STORY", storyId.toString(), storyId.toString())
    }
}