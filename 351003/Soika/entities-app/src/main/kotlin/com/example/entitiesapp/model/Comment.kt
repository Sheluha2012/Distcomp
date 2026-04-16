package com.example.entitiesapp.model

import jakarta.persistence.*

@Entity
@Table(name = "tbl_comment", schema = "distcomp")
data class Comment(
    override var id: Long? = null,

    @Column(name = "story_id", nullable = false)
    var storyId: Long,

    @Column(nullable = false, length = 2048)
    var content: String
) : BaseEntity(id)