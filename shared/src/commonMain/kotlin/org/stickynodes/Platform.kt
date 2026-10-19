package org.stickynodes

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform