plugins {
    id("servista.api-service")
    id("servista.testing")
    id("servista.observability")
    id("servista.jooq")
}

repositories { mavenCentral() }

// Override detekt config to fix detekt 2.x category renames (pre-existing gradle-platform issue).
// The shared detekt.yml uses old category names (emptyblocks, potentialbugs, build).
detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    parallel = true
}

// Fix testcontainers 2.x module renames (pre-existing issue in gradle-platform convention plugin).
// TC 2.x renamed org.testcontainers:postgresql -> org.testcontainers:testcontainers-postgresql
// and org.testcontainers:kafka -> org.testcontainers:testcontainers-kafka.
configurations.all {
    exclude(group = "org.testcontainers", module = "postgresql")
    exclude(group = "org.testcontainers", module = "kafka")
}

dependencies {
    // Upstream: framework-agnostic commons (ServistaException, HealthRegistry, ServistaContext)
    implementation("eu.servista:servista-commons:0.1.0")

    // JSON structured logging for production (logstash-logback-encoder 8.1, NOT 9.0 which requires Jackson 3.x)
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    // Janino for Logback conditional processing (<if> tags in logback.xml)
    implementation("org.codehaus.janino:janino:3.1.12")

    // Ktor Micrometer metrics plugin (in version catalog but not in convention plugin)
    implementation(libs.ktor.server.metrics.micrometer)

    // Prometheus metrics registry (already in observability plugin, explicit for clarity)
    implementation(libs.micrometer.prometheus)

    // JUnit Platform launcher (required by Gradle 9.x test execution)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
