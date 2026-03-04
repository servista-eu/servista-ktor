package eu.servista.ktor.health

import eu.servista.commons.health.HealthCheckResult
import eu.servista.commons.health.HealthResponse
import eu.servista.commons.health.HealthStatus
import kotlinx.serialization.Serializable

/**
 * Serializable DTO for health check responses.
 *
 * The upstream [HealthResponse] from servista-commons is not annotated with `@Serializable` (it's
 * framework-agnostic). This DTO bridges the gap for Ktor JSON serialization.
 */
@Serializable
data class HealthResponseDto(
    val status: String,
    val checks: List<HealthCheckResultDto>,
    val buildInfo: BuildInfoDto,
) {
    companion object {
        fun from(response: HealthResponse) =
            HealthResponseDto(
                status = response.status.name,
                checks = response.checks.map { HealthCheckResultDto.from(it) },
                buildInfo = BuildInfoDto.from(response.buildInfo),
            )
    }
}

@Serializable
data class HealthCheckResultDto(
    val name: String,
    val status: String,
    val details: Map<String, String> = emptyMap(),
    val error: String? = null,
) {
    companion object {
        fun from(result: HealthCheckResult) =
            HealthCheckResultDto(
                name = result.name,
                status = result.status.name,
                details = result.details,
                error = result.error,
            )
    }
}

@Serializable
data class BuildInfoDto(
    val version: String,
    val gitSha: String,
    val buildTimestamp: String,
) {
    companion object {
        fun from(buildInfo: eu.servista.commons.health.BuildInfo) =
            BuildInfoDto(
                version = buildInfo.version,
                gitSha = buildInfo.gitSha,
                buildTimestamp = buildInfo.buildTimestamp,
            )
    }
}
