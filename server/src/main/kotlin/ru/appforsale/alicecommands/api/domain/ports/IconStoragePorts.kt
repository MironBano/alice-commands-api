package ru.appforsale.alicecommands.api.domain.ports

interface IconStorage {
    fun store(slug: String, svg: String): String
    fun iconUrl(slug: String): String
    fun basePublicUrl(): String
    fun listSlugs(): List<String>
    fun exists(slug: String): Boolean
    fun read(slug: String): String?
}

interface DeviceImageStorage {
    fun store(slug: String, bytes: ByteArray, extension: String): String
    fun imageUrl(slug: String, extension: String): String
    fun basePublicUrl(): String
    fun exists(slug: String, extension: String): Boolean
}
