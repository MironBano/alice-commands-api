package ru.appforsale.alicecommands.api.config

import java.nio.file.Path
import kotlin.io.path.Path

data class AppConfig(
    val env: String,
    val port: Int,
    val publicBaseUrl: String,
    val adminUsername: String,
    val adminPassword: String,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val bundleStoragePath: Path,
    val manifestStoragePath: Path,
    val bundleRetentionCount: Int,
    val sessionSecret: String,
    val adminLoginRateLimit: Int,
    val contentSeedPath: Path?,
) {
    val isProduction: Boolean get() = env == "prod" || env == "staging"

    companion object {
        fun load(): AppConfig {
            val vars = loadEnvVars()
            fun env(key: String, default: String = ""): String =
                vars[key]?.takeIf { it.isNotBlank() } ?: default

            val appEnv = env("APP_ENV", "local")
            val adminPassword = env("ADMIN_PASSWORD").ifBlank {
                error("ADMIN_PASSWORD is required")
            }
            if (appEnv == "prod" && adminPassword == "change-me-in-production") {
                error("ADMIN_PASSWORD must be changed in production")
            }

            return AppConfig(
                env = appEnv,
                port = env("APP_PORT", "8080").toInt(),
                publicBaseUrl = env("PUBLIC_BASE_URL", "http://localhost:8080").trimEnd('/'),
                adminUsername = env("ADMIN_USERNAME").ifBlank { error("ADMIN_USERNAME is required") },
                adminPassword = adminPassword,
                databaseUrl = env("DATABASE_URL").ifBlank { error("DATABASE_URL is required") },
                databaseUser = env("DATABASE_USER").ifBlank { error("DATABASE_USER is required") },
                databasePassword = env("DATABASE_PASSWORD").ifBlank { error("DATABASE_PASSWORD is required") },
                bundleStoragePath = resolvePath(env("BUNDLE_STORAGE_PATH", "./storage/bundles")),
                manifestStoragePath = resolvePath(env("MANIFEST_STORAGE_PATH", "./storage/manifest")),
                bundleRetentionCount = env("BUNDLE_RETENTION_COUNT", "5").toInt(),
                sessionSecret = env("SESSION_SECRET").ifBlank { error("SESSION_SECRET is required") }.also {
                    require(it.length >= 32) { "SESSION_SECRET must be at least 32 characters" }
                },
                adminLoginRateLimit = env("ADMIN_LOGIN_RATE_LIMIT", "5").toInt(),
                contentSeedPath = env("CONTENT_SEED_PATH").takeIf { it.isNotBlank() }?.let { resolvePath(it) }
                    ?: resolvePath("./seed/full-catalog.json").takeIf { appEnv == "local" && it.toFile().exists() },
            )
        }

        private fun loadEnvVars(): Map<String, String> {
            val result = mutableMapOf<String, String>()
            System.getenv().forEach { (k, v) -> result[k] = v }
            System.getProperties().forEach { (k, v) ->
                val key = k.toString()
                if (key !in result && !key.startsWith("java.") && !key.startsWith("user.") &&
                    !key.startsWith("os.") && !key.startsWith("file.") && !key.startsWith("line.") &&
                    !key.startsWith("path.") && v.toString().isNotBlank()
                ) {
                    result[key] = v.toString()
                }
            }
            val dotEnv = listOf(Path(".env"), Path("../.env")).firstOrNull { it.toFile().exists() }
            if (dotEnv != null) {
                dotEnv.toFile().readLines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                    val idx = trimmed.indexOf('=')
                    if (idx <= 0) return@forEach
                    val key = trimmed.substring(0, idx).trim()
                    if (key !in result) {
                        val value = trimmed.substring(idx + 1).trim()
                            .removeSurrounding("\"")
                            .removeSurrounding("'")
                        result[key] = value
                    }
                }
            }
            return result
        }

        private fun resolvePath(raw: String): Path {
            val path = Path(raw)
            if (path.isAbsolute || path.toFile().exists()) return path
            val fromParent = Path("../$raw")
            return if (fromParent.toFile().exists() || raw.startsWith("./storage")) fromParent else path
        }
    }
}
