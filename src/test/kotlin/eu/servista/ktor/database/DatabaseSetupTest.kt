package eu.servista.ktor.database

import eu.servista.ktor.DatabaseConfig
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer

class DatabaseSetupTest {

    @Test
    fun `migrateDatabase creates working HikariDataSource and runs Flyway migration`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()

            val config =
                DatabaseConfig(
                    url = postgres.jdbcUrl,
                    user = postgres.username,
                    password = postgres.password,
                )

            val dataSource = migrateDatabase(config)

            // Verify the data source is usable
            dataSource shouldNotBe null
            dataSource.isClosed shouldBe false

            // Verify Flyway ran the V1__test_schema.sql migration by querying the created table
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs =
                        stmt.executeQuery(
                            "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'test_table')"
                        )
                    rs.next()
                    rs.getBoolean(1) shouldBe true
                }
            }

            // Verify the HikariDataSource pool is active
            dataSource.isRunning shouldBe true
            dataSource.maximumPoolSize shouldBe 10

            dataSource.close()
        }
    }

    @Test
    fun `migrateDatabase pool is usable for SQL queries`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()

            val config =
                DatabaseConfig(
                    url = postgres.jdbcUrl,
                    user = postgres.username,
                    password = postgres.password,
                )

            val dataSource = migrateDatabase(config)

            // Insert and query data to verify full database connectivity
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("INSERT INTO test_table (id, name) VALUES (1, 'test')")
                }
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT name FROM test_table WHERE id = 1")
                    rs.next()
                    rs.getString("name") shouldBe "test"
                }
            }

            dataSource.close()
        }
    }
}
