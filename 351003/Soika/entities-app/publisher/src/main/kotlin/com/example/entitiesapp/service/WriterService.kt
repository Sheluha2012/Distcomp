package com.example.entitiesapp.service

import com.example.entitiesapp.dto.WriterRequestTo
import com.example.entitiesapp.dto.WriterResponseTo
import com.example.entitiesapp.exception.NotFoundException
import com.example.entitiesapp.model.Writer
import com.example.entitiesapp.repository.StoryRepository
import com.example.entitiesapp.repository.WriterRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class WriterService(
    private val repository: WriterRepository,
    private val storyRepository: StoryRepository,
    private val commentService: CommentService
) {

    private fun toEntity(dto: WriterRequestTo) = Writer(
        login = dto.login,
        password = dto.password,
        firstname = dto.firstname,
        lastname = dto.lastname
    )

    private fun toResponse(entity: Writer) = WriterResponseTo(
        id = entity.id!!,
        login = entity.login,
        password = entity.password,
        firstname = entity.firstname,
        lastname = entity.lastname
    )

    fun getAll(): List<WriterResponseTo> = repository.findAll().map { toResponse(it) }

    @Cacheable(value = ["writers"], key = "#id")
    fun getById(id: Long): WriterResponseTo {
        println("!!! Cache MISS for Writer $id")
        return toResponse(
            repository.findById(id).orElseThrow {
                NotFoundException("Writer with id=$id not found", 40401)
            }
        )
    }

    @CachePut(value = ["writers"], key = "#result.id")
    fun create(dto: WriterRequestTo): WriterResponseTo =
        toResponse(repository.save(toEntity(dto)))

    @CachePut(value = ["writers"], key = "#id")
    fun update(id: Long, dto: WriterRequestTo): WriterResponseTo {
        if (!repository.existsById(id))
            throw NotFoundException("Writer with id=$id not found", 40401)
        val entity = toEntity(dto).apply { this.id = id }
        return toResponse(repository.save(entity))
    }

    @CacheEvict(value = ["writers"], key = "#id")
    fun delete(id: Long) {
        if (!repository.existsById(id))
            throw NotFoundException("Writer with id=$id not found", 40401)

        val stories = storyRepository.findAllByWriterId(id)
        stories.forEach { story ->
            commentService.deleteByStory(story.id!!)
            storyRepository.deleteById(story.id!!)
        }
        repository.deleteById(id)
    }
}