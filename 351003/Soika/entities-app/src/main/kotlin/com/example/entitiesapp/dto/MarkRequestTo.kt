package com.example.entitiesapp.dto

import jakarta.validation.constraints.NotBlank

data class MarkRequestTo(
    val id: Long? = null,

    @field:NotBlank
    val name: String
)