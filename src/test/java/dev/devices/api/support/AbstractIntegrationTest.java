package dev.devices.api.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for integration tests.
 *
 * <p>Uses the Testcontainers "singleton container" pattern: one PostgreSQL
 * container is started once and shared by every subclass. Because all
 * subclasses have the same configuration, Spring also reuses a single cached
 * application context instead of starting a new one per test class.
 * The container is stopped automatically by Testcontainers when the JVM exits.
 */
@SpringBootTest(properties = {
        "devices.security.admin.username=" + AbstractIntegrationTest.ADMIN_USERNAME,
        "devices.security.admin.password=" + AbstractIntegrationTest.ADMIN_PASSWORD,
        "devices.security.viewer.username=" + AbstractIntegrationTest.VIEWER_USERNAME,
        "devices.security.viewer.password=" + AbstractIntegrationTest.VIEWER_PASSWORD
})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_USERNAME = "test-admin";
    protected static final String ADMIN_PASSWORD = "test-admin-password";
    protected static final String VIEWER_USERNAME = "test-viewer";
    protected static final String VIEWER_PASSWORD = "test-viewer-password";

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }
}
