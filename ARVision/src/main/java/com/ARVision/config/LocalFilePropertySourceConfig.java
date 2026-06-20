package com.ARVision.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "file:./src/main/resources/application.properties", ignoreResourceNotFound = true)
public class LocalFilePropertySourceConfig {
}
