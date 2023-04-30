package config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ConfigManager {
    private static final String ENV = System.getenv("ENVIRONMENT");

    public static Config getConfig() {
        if (ENV.equalsIgnoreCase("prod")) {
            System.out.println("Using prod config");
            return ConfigFactory.load("application-prod.conf");
        }

        System.out.println("Using dev config");
        return ConfigFactory.load("application-dev.conf");
    }
}
