package com.example.entitiesapp.repository

import com.example.entitiesapp.model.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {

    @Transactional
    fun deleteAllByStoryId(storyId: Long)
}