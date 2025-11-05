package com.kimurashin.fastdev.domain.model

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform