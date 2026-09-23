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
    val publicSubmissionRateLimit: Int,
    val pushInstallRateLimit: Int,
    val contentSeedPath: Path?,
    val iconStoragePath: Path,
    val iconPublicBaseUrl: String,
    val iconUrlAllowedHosts: Set<String>,
    val iconCatalogPath: Path,
    val deviceImageStoragePath: Path,
    val announcementImageStoragePath: Path,
    val analyticsRateLimitPerIp: Int,
    val analyticsEventsPerIpPerDay: Int,
    val analyticsMaxBodyBytes: Int,
    val analyticsRawRetentionDays: Int,
    val rustorePushProjectId: String,
    val rustorePushServiceToken: String,
    val pushCampaignEnabled: Boolean,
    /** When true, device picks require erid/advertiser and affiliate query on Market URLs. Staging first; prod off until go. */
    val requirePickAffiliateQuery: Boolean,
    /** Staging-first: enable Market affiliate pick refresh ticker. */
    val marketAffiliateRefreshEnabled: Boolean,
    val marketAffiliateOauthToken: String,
    val marketAffiliateClid: String,
    val pickSkuMapPath: Path,
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

            val publicBaseUrl = env("PUBLIC_BASE_URL", "http://localhost:8080").trimEnd('/')
            val iconPublicBaseUrl = env("ICON_PUBLIC_BASE_URL", publicBaseUrl).trimEnd('/')
            val iconUrlAllowedHosts = parseHostAllowlist(
                env("ICON_URL_ALLOWED_HOSTS", "cdn.alicecommands.ru,staging-api.alicecommands.ru,api.alicecommands.ru,localhost,127.0.0.1"),
                iconPublicBaseUrl,
            )

            return AppConfig(
                env = appEnv,
                port = env("APP_PORT", "8080").toInt(),
                publicBaseUrl = publicBaseUrl,
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
                publicSubmissionRateLimit = env("PUBLIC_SUBMISSION_RATE_LIMIT", "20").toInt(),
                pushInstallRateLimit = env("PUSH_INSTALL_RATE_LIMIT", "30").toInt(),
                contentSeedPath = env("CONTENT_SEED_PATH").takeIf { it.isNotBlank() }?.let { resolvePath(it) }
                    ?: resolvePath("./seed/catalog-audit-fixed.json").takeIf { appEnv == "local" && it.toFile().exists() },
                iconStoragePath = resolvePath(env("ICON_STORAGE_PATH", "./storage/icons")),
                iconPublicBaseUrl = iconPublicBaseUrl,
                iconUrlAllowedHosts = iconUrlAllowedHosts,
                iconCatalogPath = resolveCatalogPath(),
                deviceImageStoragePath = resolvePath(env("DEVICE_IMAGE_STORAGE_PATH", "./storage/devices")),
                announcementImageStoragePath = resolvePath(env("ANNOUNCEMENT_IMAGE_STORAGE_PATH", "./storage/announcements")),
                analyticsRateLimitPerIp = env("ANALYTICS_RATE_LIMIT_PER_IP", "120").toInt(),
                analyticsEventsPerIpPerDay = env("ANALYTICS_EVENTS_PER_IP_PER_DAY", "10000").toInt(),
                analyticsMaxBodyBytes = env("ANALYTICS_MAX_BODY_BYTES", "262144").toInt(),
                analyticsRawRetentionDays = env("ANALYTICS_RAW_RETENTION_DAYS", "90").toInt(),
                rustorePushProjectId = env("RUSTORE_PUSH_PROJECT_ID"),
                rustorePushServiceToken = env("RUSTORE_PUSH_SERVICE_TOKEN"),
                pushCampaignEnabled = env("PUSH_CAMPAIGN_ENABLED", "true").equals("true", ignoreCase = true),
                requirePickAffiliateQuery = env("REQUIRE_PICK_AFFILIATE_QUERY", "false")
                    .equals("true", ignoreCase = true),
                marketAffiliateRefreshEnabled = env("MARKET_AFFILIATE_REFRESH_ENABLED", "false")
                    .equals("true", ignoreCase = true),
                marketAffiliateOauthToken = env("MARKET_AFFILIATE_OAUTH_TOKEN"),
                marketAffiliateClid = env("MARKET_AFFILIATE_CLID"),
                pickSkuMapPath = resolvePath(env("PICK_SKU_MAP_PATH", "./storage/affiliate/pick_sku_map.json")),
            )
        }

        private fun parseHostAllowlist(raw: String, iconPublicBaseUrl: String): Set<String> {
            val hosts = raw.split(',').map { it.trim().lowercase() }.filter { it.isNotBlank() }.toMutableSet()
            try {
                val uri = java.net.URI(iconPublicBaseUrl)
                uri.host?.lowercase()?.let { hosts += it }
            } catch (_: Exception) {
                // ignore
            }
            return hosts
        }

        private fun resolveCatalogPath(): Path {
            val candidates = listOf(
                Path("content/icon_catalog.json"),
                Path("../content/icon_catalog.json"),
            )
            return candidates.firstOrNull { it.toFile().exists() } ?: Path("content/icon_catalog.json")
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
