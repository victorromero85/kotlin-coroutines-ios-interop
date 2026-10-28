package org.example.kotlin_coroutines_ios_interop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform