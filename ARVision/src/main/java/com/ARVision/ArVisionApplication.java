package com.ARVision;

import jakarta.servlet.MultipartConfigElement;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.DispatcherServlet;

@SpringBootApplication
public class ArVisionApplication {

    private static final Logger log = LoggerFactory.getLogger(ArVisionApplication.class);

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(ArVisionApplication.class, args);
    }

    /**
     * Diagnostic: log the resolved multipart size properties at startup so we
     * can see exactly which values Spring actually applies.
     */
    @PostConstruct
    public void logMultipartConfig() {
        log.info("==== MULTIPART CONFIG DIAGNOSTIC ====");
        log.info("spring.servlet.multipart.max-file-size       = {}",
                env.getProperty("spring.servlet.multipart.max-file-size"));
        log.info("spring.servlet.multipart.max-request-size    = {}",
                env.getProperty("spring.servlet.multipart.max-request-size"));
        log.info("server.tomcat.max-http-form-post-size       = {}",
                env.getProperty("server.tomcat.max-http-form-post-size"));
        log.info("server.tomcat.max-swallow-size              = {}",
                env.getProperty("server.tomcat.max-swallow-size"));
        log.info("====================================");
    }

    /**
     * Force Tomcat connector to accept 100 MB request bodies, regardless of
     * any property- or bean-based config that might be ignored.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setMaxPostSize(100 * 1024 * 1024);           // 100 MB
            connector.setProperty("maxSwallowSize", "104857600"); // 100 MB
        });
    }

    /**
     * Mutate the auto-registered DispatcherServlet to attach a hardcoded
     * MultipartConfigElement of 100 MB. By depending on the existing
     * registration bean we avoid breaking DispatcherServletAutoConfiguration
     * (which provides the DispatcherServletPath bean) — we just override the
     * multipart element after Spring has created it.
     */
    @Bean
    public org.springframework.boot.web.servlet.ServletRegistrationBean<?> dispatcherMultipartOverride(
            org.springframework.beans.factory.ObjectProvider<org.springframework.boot.web.servlet.ServletRegistrationBean<?>> registrations) {
        org.springframework.boot.web.servlet.ServletRegistrationBean<?> dispatcherReg = registrations.stream()
                .filter(r -> "dispatcherServlet".equals(r.getServletName())
                        || "dispatcher".equals(r.getServletName()))
                .findFirst()
                .orElse(null);
        if (dispatcherReg != null) {
            dispatcherReg.setMultipartConfig(
                    new jakarta.servlet.MultipartConfigElement(
                            System.getProperty("java.io.tmpdir"), // location
                            100L * 1024 * 1024,                    // maxFileSize  100 MB
                            100L * 1024 * 1024,                    // maxRequestSize 100 MB
                            2 * 1024                                // fileSizeThreshold 2 KB
                    )
            );
            log.info("Overrode dispatcher servlet multipart-config to 100 MB");
        } else {
            log.warn("Could not locate dispatcher servlet registration to override multipart-config");
        }
        // Return a no-op pass-through bean so we don't replace the original registration
        return new org.springframework.boot.web.servlet.ServletRegistrationBean<>(
                new jakarta.servlet.http.HttpServlet() {}, "/__noop__");
    }
}
