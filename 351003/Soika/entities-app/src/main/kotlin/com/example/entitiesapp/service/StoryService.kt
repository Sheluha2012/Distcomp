package com.example.entitiesapp.service

import com.example.entitiesapp.dto.StoryRequestTo
import com.example.entitiesapp.dto.StoryResponseTo
import com.example.entitiesapp.exception.NotFoundException
import com.example.entitiesapp.exception.ValidationException
import com.example.entitiesapp.model.Mark
import com.example.entitiesapp.model.Story
import com.example.entitiesapp.repository.MarkRepository
import com.example.entitiesapp.repository.StoryRepository
import com.example.entitiesapp.repository.WriterRepository
import com.example.entitiesapp.repository.CommentRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoryService(
    private val storyRepository: StoryRepository,
    private val writerRepository: WriterRepository,
    private val markRepository: MarkRepository,
    private val commentRepository: CommentRepository
) {

    private fun toEntity(dto: StoryRequestTo): Story {
        val marksByIds = dto.markIds.map {
            markRepository.findById(it).orElseThrow {
                ValidationException("Mark id=$it does not exist", 40002)
            }
        }

        val marksByName = dto.marks.map { name ->
            markRepository.findByName(name)
                ?: markRepository.save(Mark(name = name))
        }

        val allMarks = (marksByIds + marksByName).toMutableSet()

        return Story(
            writerId = dto.writerId,
            title = dto.title,
            content = dto.content,
            marks = allMarks
        )
    }

    private fun toResponse(entity: Story) =
        StoryResponseTo(
            id = entity.id!!,
            title = entity.title,
            content = entity.content,
            writerId = entity.writerId,
            markIds = entity.marks.map { it.id!! }.toSet()
        )

    fun getAll(): List<StoryResponseTo> =
        storyRepository.findAll().map { toResponse(it) }

    fun getById(id: Long): StoryResponseTo =
        toResponse(
            storyRepository.findById(id).orElseThrow {
                NotFoundException("Story with id=$id not found", 40403)
            }
        )

    fun create(dto: StoryRequestTo): StoryResponseTo {
        if (!writerRepository.existsById(dto.writerId))
            throw ValidationException("Writer does not exist", 40001)
        println(">>> markIds from request: ${dto.markIds}")
        val entity = toEntity(dto).apply {
            created = LocalDateTime.now()
            modified = LocalDateTime.now()
        }
        println(">>> marks in entity before save: ${entity.marks.map { it.id }}")
        val saved = storyRepository.save(entity)
        println(">>> marks in saved entity: ${saved.marks.map { it.id }}")
        return toResponse(saved)
    }

    fun update(id: Long, dto: StoryRequestTo): StoryResponseTo {
        if (!storyRepository.existsById(id))
            throw NotFoundException("Story with id=$id not found", 40403)
        if (!writerRepository.existsById(dto.writerId))
            throw ValidationException("Writer does not exist", 40001)
        val entity = toEntity(dto).apply {
            this.id = id
            modified = LocalDateTime.now()
        }
        return toResponse(storyRepository.save(entity))
    }

    fun delete(id: Long) {
        if (!storyRepository.existsById(id))
            throw NotFoundException("Story with id=$id not found", 40403)

        val story = storyRepository.findById(id).get()
        val markIds = story.marks.map { it.id!! }.toSet()

        commentRepository.deleteAllByStoryId(id)
        story.marks.clear()
        storyRepository.save(story)
        storyRepository.deleteById(id)

        markIds.forEach { markId ->
            val isUsed = storyRepository.existsByMarksId(markId)
            if (!isUsed) {
                markRepository.deleteById(markId)
            }
        }
    }
}