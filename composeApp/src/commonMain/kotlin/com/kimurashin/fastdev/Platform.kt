package com.kimurashin.fastdev

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform