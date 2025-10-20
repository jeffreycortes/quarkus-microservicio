package com.dian;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EnvironmentService {

    @ConfigProperty(name = "app.name", defaultValue = "DefaultApp")
    String appName;

    @ConfigProperty(name = "app.version", defaultValue = "1.0.0")
    String appVersion;

    @ConfigProperty(name = "app.environment", defaultValue = "development")
    String appEnvironment;

    @ConfigProperty(name = "app.profile", defaultValue = "profile_nn")
    String appProfile;

    @ConfigProperty(name = "app.alias", defaultValue = "alias_sin_definir")
    String appAlias;

    @ConfigProperty(name = "app.database.host", defaultValue = "localhost")
    String dbHost;

    @ConfigProperty(name = "app.database.port", defaultValue = "3306")
    String dbPort;

    @ConfigProperty(name = "app.database.name", defaultValue = "test_db")
    String dbName;

    @ConfigProperty(name = "app.database.user", defaultValue = "default_user")
    String dbUser;

    @ConfigProperty(name = "app.database.password", defaultValue = "default_pass")
    String dbPassword;

    @ConfigProperty(name = "app.server.url", defaultValue = "http://localhost:8080")
    String serverUrl;

    @ConfigProperty(name = "app.server.timeout", defaultValue = "30")
    String serverTimeout;

    @ConfigProperty(name = "app.features.enabled", defaultValue = "true")
    String featuresEnabled;

    @ConfigProperty(name = "app.features.max-retries", defaultValue = "3")
    String maxRetries;

    public String getAppInfo() {
        return String.format("""
            Application Info:
            - Name: %s
            - Version: %s
            - Environment: %s
            - Profile: %s
            - Alias: %s
            """, appName, appVersion, appEnvironment, appProfile, appAlias);
    }

    public String getDatabaseConfig() {
        return String.format("""
            Database Config:
            - Host: %s
            - Port: %s
            - Name: %s
            - User: %s
            - Password: %s
            """, dbHost, dbPort, dbName, dbUser, "***" + dbPassword.substring(Math.max(0, dbPassword.length() - 3)));
    }

    public String getServerConfig() {
        return String.format("""
            Server Config:
            - URL: %s
            - Timeout: %s seconds
            """, serverUrl, serverTimeout);
    }

    public String getFeaturesConfig() {
        return String.format("""
            Features Config:
            - Enabled: %s
            - Max Retries: %s
            """, featuresEnabled, maxRetries);
    }

    public String getAllConfig() {
        return getAppInfo() + "\n" + getDatabaseConfig() + "\n" + getServerConfig() + "\n" + getFeaturesConfig();
    }
}
