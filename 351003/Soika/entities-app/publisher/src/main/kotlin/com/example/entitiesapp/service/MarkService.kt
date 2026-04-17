package com.example.entitiesapp.service

import com.example.entitiesapp.dto.MarkRequestTo
import com.example.entitiesapp.dto.MarkResponseTo
import com.example.entitiesapp.exception.NotFoundException
import com.example.entitiesapp.model.Mark
import com.example.entitiesapp.repository.MarkRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class MarkService(private val repository: MarkRepository) {

    private fun toEntity(dto: MarkRequestTo) = Mark(name = dto.name)

    private fun toResponse(entity: Mark) = MarkResponseTo(id = entity.id!!, name = entity.name)

    fun getAll(): List<MarkResponseTo> = repository.findAll().map { toResponse(it) }

    @Cacheable(value = ["marks"], key = "#id")
    fun getById(id: Long): MarkResponseTo {
        println("!!! Cache MISS for Mark $id")
        return toResponse(
            repository.findById(id).orElseThrow {
                NotFoundException("Mark with id=$id not found", 40402)
            }
        )
    }

    @CachePut(value = ["marks"], key = "#result.id")
    fun create(dto: MarkRequestTo): MarkResponseTo = toResponse(repository.save(toEntity(dto)))

    @CachePut(value = ["marks"], key = "#id")
    fun update(id: Long, dto: MarkRequestTo): MarkResponseTo {
        if (!repository.existsById(id))
            throw NotFoundException("Mark with id=$id not found", 40402)
        val entity = toEntity(dto).apply { this.id = id }
        return toResponse(repository.save(entity))
    }

    @CacheEvict(value = ["marks"], key = "#id")
    fun delete(id: Long) {
        if (!repository.existsById(id))
            throw NotFoundException("Mark with id=$id not found", 40402)
        repository.deleteById(id)
    }
}