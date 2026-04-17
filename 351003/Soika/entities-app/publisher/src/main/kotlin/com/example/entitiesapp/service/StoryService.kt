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
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoryService(
    private val storyRepository: StoryRepository,
    private val writerRepository: WriterRepository,
    private val markRepository: MarkRepository,
    private val commentService: CommentService
) {

    private fun toEntity(dto: StoryRequestTo): Story {
        val marksByIds = dto.markIds.map {
            markRepository.findById(it).orElseThrow { ValidationException("Mark id=$it missing", 40002) }
        }
        val marksByName = dto.marks.map { name ->
            markRepository.findByName(name) ?: markRepository.save(Mark(name = name))
        }
        return Story(
            writerId = dto.writerId,
            title = dto.title,
            content = dto.content,
            marks = (marksByIds + marksByName).toMutableSet()
        )
    }

    private fun toResponse(entity: Story) = StoryResponseTo(
        id = entity.id!!,
        title = entity.title,
        content = entity.content,
        writerId = entity.writerId,
        markIds = entity.marks.map { it.id!! }.toSet()
    )

    fun getAll(): List<StoryResponseTo> = storyRepository.findAll().map { toResponse(it) }

    @Cacheable(value = ["stories"], key = "#id")
    fun getById(id: Long): StoryResponseTo {
        println("!!! Cache MISS for Story $id")
        return toResponse(
            storyRepository.findById(id).orElseThrow {
                NotFoundException("Story with id=$id not found", 40403)
            }
        )
    }

    @CachePut(value = ["stories"], key = "#result.id")
    fun create(dto: StoryRequestTo): StoryResponseTo {
        if (!writerRepository.existsById(dto.writerId)) throw ValidationException("Writer missing", 40001)
        val entity = toEntity(dto).apply {
            created = LocalDateTime.now()
            modified = LocalDateTime.now()
        }
        return toResponse(storyRepository.save(entity))
    }

    @CachePut(value = ["stories"], key = "#id")
    fun update(id: Long, dto: StoryRequestTo): StoryResponseTo {
        if (!storyRepository.existsById(id)) throw NotFoundException("Story not found", 40403)
        val entity = toEntity(dto).apply {
            this.id = id
            modified = LocalDateTime.now()
        }
        return toResponse(storyRepository.save(entity))
    }

    @CacheEvict(value = ["stories"], key = "#id")
    fun delete(id: Long) {
        if (!storyRepository.existsById(id)) throw NotFoundException("Story not found", 40403)
        commentService.deleteByStory(id)
        storyRepository.deleteById(id)
    }
}