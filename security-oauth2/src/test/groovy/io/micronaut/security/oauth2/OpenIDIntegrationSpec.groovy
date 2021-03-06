package io.micronaut.security.oauth2

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.Container
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.IgnoreIf
import spock.lang.Specification

trait OpenIDIntegrationSpec {

    static String CLIENT_SECRET
    static String ISSUER
    static GenericContainer keycloak

    static {
        keycloak = new GenericContainer("jboss/keycloak:6.0.1")
                .withExposedPorts(8080)
                .withEnv([
                        KEYCLOAK_USER: 'user',
                        KEYCLOAK_PASSWORD: 'password',
                        DB_VENDOR: 'H2',
                ])
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*Deployed \"keycloak-server.war\".*"))
        keycloak.start()
        Container.ExecResult result = keycloak.execInContainer("keycloak/bin/kcreg.sh config credentials --server http://localhost:8080/auth --realm master --user user --password password".split(" "))
        result = keycloak.execInContainer("keycloak/bin/kcreg.sh create -s clientId=myclient -s redirectUris=[\"http://localhost*\"]".split(" "))
        result = keycloak.execInContainer("keycloak/bin/kcreg.sh get myclient".split(" "))
        Map map = new ObjectMapper().readValue(result.getStdout(), Map.class)
        CLIENT_SECRET = map.get("secret")
        ISSUER = "http://localhost:" + keycloak.getMappedPort(8080) + "/auth/realms/master"
    }

    ApplicationContext startContext(Map<String, Object> configuration = getConfiguration()) {
        return ApplicationContext.run(configuration, "test")
    }

    Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>()
        config.put("spec.name", this.getClass().getSimpleName())
        return config
    }

}
