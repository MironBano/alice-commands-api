package ru.appforsale.alicecommands.api.domain.ports

interface IconStorage {
    fun store(slug: String, svg: String): String
    fun iconUrl(slug: String): String
    fun basePublicUrl(): String
    fun listSlugs(): List<String>
    fun exists(slug: String): Boolean
    fun read(slug: String): String?
}
