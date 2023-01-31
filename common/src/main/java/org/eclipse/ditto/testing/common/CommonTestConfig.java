/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.testing.common;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.ditto.base.model.json.JsonSchemaVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

/**
 * Provides common test configuration for the System tests.
 */
public class CommonTestConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CommonTestConfig.class);

    private static final String MONGO_PREFIX = "mongoDB.";
    private static final String GATEWAY_PREFIX = "gateway.";
    private static final String PROPERTY_LEAF_HOSTNAME = "hostname";
    private static final String PROPERTY_LEAF_OVERWRITE_HOSTNAME = "overwriteHostname";
    private static final String PROPERTY_LEAF_PORT = "port";

    private static final String PROPERTY_SETUP_HTTP_PROXY_ENABLED = "setup.http.proxy.enabled";

    private static final String SYSTEM_PROPERTY_PROXY_HOST = "http.proxyHost";
    private static final String SYSTEM_PROPERTY_PROXY_PORT = "http.proxyPort";

    private static final String PROPERTY_MONGODB_URI = MONGO_PREFIX + "uri";

    private static final String PROPERTY_GATEWAY_URL = GATEWAY_PREFIX + "url";
    private static final String PROPERTY_GATEWAY_WS_URL = GATEWAY_PREFIX + "ws-url";
    private static final String PROPERTY_DEVELOPER_CONSOLE_URL = "developer-console.url";

    private static final String CONNECTIVITY_PREFIX = "connectivity.";
    private static final String RABBITMQ_PREFIX = CONNECTIVITY_PREFIX + "rabbitmq.";
    private static final String PROPERTY_RABBITMQ_HOSTNAME = RABBITMQ_PREFIX + PROPERTY_LEAF_HOSTNAME;
    private static final String PROPERTY_RABBITMQ_TUNNEL = RABBITMQ_PREFIX + "tunnel";
    private static final String PROPERTY_RABBITMQ_OVERWRITE_HOSTNAME = RABBITMQ_PREFIX + PROPERTY_LEAF_OVERWRITE_HOSTNAME;

    private static final String PROPERTY_RABBITMQ_PORT = RABBITMQ_PREFIX + PROPERTY_LEAF_PORT;
    private static final String PROPERTY_RABBITMQ_USER = RABBITMQ_PREFIX + "username";
    private static final String PROPERTY_RABBITMQ_PASSWORD = RABBITMQ_PREFIX + "password";

    private static final String DEVOPS_PREFIX = GATEWAY_PREFIX + "devops.";
    private static final String PROPERTY_DEVOPS_URL = DEVOPS_PREFIX + "url";
    private static final String PROPERTY_DEVOPS_COMMAND_TIMEOUT = DEVOPS_PREFIX + "command-timeout";
    private static final String DEVOPS_AUTH_PREFIX = DEVOPS_PREFIX + "auth.";
    private static final String PROPERTY_DEVOPS_AUTH_ENABLED = DEVOPS_AUTH_PREFIX + "enabled";
    private static final String DEVOPS_AUTH_USER = "devops";
    private static final String PROPERTY_DEVOPS_AUTH_PASSWORD = DEVOPS_AUTH_PREFIX + "password";

    private static final String OAUTH_MOCK_PREFIX = "oauth-mock.";
    private static final String OAUTH_MOCK_TOKEN_ENDPOINT = OAUTH_MOCK_PREFIX + "tokenEndpoint";

    private static final String PROPERTY_TEST_ENVIRONMENT = "test.environment";
    private static final String TEST_ENVIRONMENT_LOCAL = "local";
    private static final String TEST_ENVIRONMENT_DOCKER_COMPOSE = "docker-compose";

    private static final String PROPERTY_SEARCH_VERSIONS = "search.versions";

    private static final String API_URL_TEMPLATE = "/api/%d%s";

    private static final CommonTestConfig INSTANCE = new CommonTestConfig();

    private static final String MODULE_RESOURCE_NAME = "test.conf";
    private static final String FALLBACK_RESOURCE_NAME = "test-common.conf";
    private static final String DEFAULT_PROXY_HOST = "localhost";
    private static final int DEFAULT_PROXY_PORT = 3128;

    protected final Config conf;
    private final String testEnvironment;

    protected CommonTestConfig() {
        final Config baseConfig = ConfigFactory.parseResourcesAnySyntax(FALLBACK_RESOURCE_NAME);
        LOG.debug("Base config: {}", baseConfig);

        this.testEnvironment = extractTestEnvironment(baseConfig);
        LOG.info("Running against test environment: {}", testEnvironment);

        final String envSpecificBaseTestConfigFile = String.format("test-common-%s.conf", testEnvironment);
        final Config envSpecificBaseTestConfig = ConfigFactory.parseResourcesAnySyntax(envSpecificBaseTestConfigFile);
        LOG.debug("Environment specific base config: {}", envSpecificBaseTestConfig);

        final Config mergedBaseConfig = envSpecificBaseTestConfig.withFallback(baseConfig);
        LOG.debug("Merged base config: {}", mergedBaseConfig);

        // module specific config is optional: they can contain an optional test.conf overwriting test-common.conf
        final Config moduleConfig = ConfigFactory.parseResourcesAnySyntax(MODULE_RESOURCE_NAME,
                ConfigParseOptions.defaults().setAllowMissing(true));
        LOG.debug("Module config: {}", moduleConfig);

        // module/env-specific config is also optional
        final String moduleEnvSpecificTestConfigFile = String.format("test-%s.conf", testEnvironment);
        final Config moduleEnvSpecificTestConfig =
                ConfigFactory.parseResourcesAnySyntax(moduleEnvSpecificTestConfigFile,
                        ConfigParseOptions.defaults().setAllowMissing(true));
        LOG.debug("Module/Environment specific config: {}", moduleEnvSpecificTestConfig);

        final Config mergedModuleConfig = moduleEnvSpecificTestConfig.withFallback(moduleConfig);
        LOG.debug("Merged module config: {}", mergedModuleConfig);

        final Config finalConfig = mergedModuleConfig.withFallback(mergedBaseConfig);
        LOG.debug("Final config: {}", finalConfig);
        conf = ConfigFactory.load(finalConfig);

        LOG.debug("Loaded config: {}", this);
    }

    private static String extractTestEnvironment(final Config notYetLoadedBaseConfig) {
        // have to load the baseConfig, otherwise system property won't be resolved
        final Config baseConfigForLoadingTestEnvironment = ConfigFactory.load(notYetLoadedBaseConfig);
        if (!baseConfigForLoadingTestEnvironment.hasPath(PROPERTY_TEST_ENVIRONMENT)) {
            throw new IllegalStateException("Test environment must be defined with property: " +
                    PROPERTY_TEST_ENVIRONMENT);
        }
        return baseConfigForLoadingTestEnvironment.getString(PROPERTY_TEST_ENVIRONMENT);
    }

    public static CommonTestConfig getInstance() {
        return INSTANCE;
    }

    public String getTestEnvironment() {
        return testEnvironment;
    }

    public Config getConfig() {
        return conf;
    }

    public String getMongoDBUri() {
        return conf.getString(PROPERTY_MONGODB_URI);
    }

    public String getDeveloperConsoleUrl(final String subUrl) {
        return concatUrl(getDeveloperConsoleBaseUrl(), subUrl);
    }

    public String getDeveloperConsoleBaseUrl() {
        return conf.getString(PROPERTY_DEVELOPER_CONSOLE_URL);
    }

    public String getGatewayUrl(final String subUrl) {
        return concatUrl(getGatewayBaseUrl(), subUrl);
    }

    public String getGatewayApiUrl(final int version, final CharSequence subUrl) {
        return getGatewayUrl(String.format(API_URL_TEMPLATE, version, subUrl));
    }

    public String getWsUrl(final String subUrl) {
        return concatUrl(getWsBaseUrl(), subUrl);
    }

    public boolean isHttpProxyEnabled() {
        return conf.getBoolean(PROPERTY_SETUP_HTTP_PROXY_ENABLED);
    }

    public String getHttpProxyHost() {
        final String proxyHost = System.getProperty(SYSTEM_PROPERTY_PROXY_HOST);
        if (proxyHost == null) {
            LOG.info("No proxy host configured. Falling back to default <{}>.", DEFAULT_PROXY_HOST);
            return DEFAULT_PROXY_HOST;
        }
        return proxyHost;
    }

    public int getHttpProxyPort() {
        final String proxyPort = System.getProperty(SYSTEM_PROPERTY_PROXY_PORT);
        if (proxyPort == null) {
            LOG.info("No proxy port configured. Falling back to default <{}>.", DEFAULT_PROXY_PORT);
            return DEFAULT_PROXY_PORT;
        }
        return Integer.parseInt(proxyPort);
    }

    public String getRabbitMqHostname() {
        return conf.getString(PROPERTY_RABBITMQ_HOSTNAME);
    }

    public String getRabbitMqTunnel() {
        return conf.getString(PROPERTY_RABBITMQ_TUNNEL);
    }

    public String getRabbitMqOverwriteHostname() {
        return conf.getString(PROPERTY_RABBITMQ_OVERWRITE_HOSTNAME);
    }

    public int getRabbitMqPort() {
        return conf.getInt(PROPERTY_RABBITMQ_PORT);
    }

    public String getRabbitMqUser() {
        return conf.getString(PROPERTY_RABBITMQ_USER);
    }

    public String getRabbitMqPassword() {
        return conf.getString(PROPERTY_RABBITMQ_PASSWORD);
    }

    public boolean isLocalOrDockerTestEnvironment() {
        return TEST_ENVIRONMENT_LOCAL.equalsIgnoreCase(testEnvironment)
                || TEST_ENVIRONMENT_DOCKER_COMPOSE.equalsIgnoreCase(testEnvironment);
    }

    public Optional<List<JsonSchemaVersion>> getSearchVersions() {
        if (!conf.hasPath(PROPERTY_SEARCH_VERSIONS)) {
            return Optional.empty();
        }

        final List<JsonSchemaVersion> versions = conf.getStringList(PROPERTY_SEARCH_VERSIONS).stream()
                .map(Integer::parseInt)
                .map(JsonSchemaVersion::forInt)
                .map(Optional::orElseThrow)
                .collect(Collectors.toList());
        return Optional.of(versions);
    }

    public String getDevopsUrl(final String subUrl) {
        return concatUrl(getDevopsBaseUrl(), subUrl);
    }

    public boolean isDevopsAuthEnabled() {
        return conf.getBoolean(PROPERTY_DEVOPS_AUTH_ENABLED);
    }

    public String getDevopsAuthUser() {
        return DEVOPS_AUTH_USER;
    }

    public String getDevopsAuthPassword() {
        if (!isDevopsAuthEnabled()) {
            throw new IllegalStateException("Devops auth is disabled!");
        }

        return conf.getString(PROPERTY_DEVOPS_AUTH_PASSWORD);
    }

    public Duration getDevopsCommandTimeout() {
        return conf.getDuration(PROPERTY_DEVOPS_COMMAND_TIMEOUT);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" +
                "conf=" + conf +
                ']';
    }

    public String getGatewayBaseUrl() {
        return conf.getString(PROPERTY_GATEWAY_URL);
    }

    private String getWsBaseUrl() {
        return conf.getString(PROPERTY_GATEWAY_WS_URL);
    }

    private String getDevopsBaseUrl() {
        return conf.getString(PROPERTY_DEVOPS_URL);
    }

    protected static String concatUrl(final String baseUrl, final String subUrl) {
        requireNonNull(subUrl);

        if (subUrl.isEmpty()) {
            return baseUrl;
        } else if (!subUrl.startsWith("/")) {
            throw new IllegalArgumentException("Non-empty subUrl must start with '/': '" + subUrl + "'");
        }

        return baseUrl + subUrl;
    }

    public String getOAuthMockTokenEndpoint() { return conf.getString(OAUTH_MOCK_TOKEN_ENDPOINT); }

}
