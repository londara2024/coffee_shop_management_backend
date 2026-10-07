package com.aura.coffeeshop.common.config;

import com.aura.coffeeshop.CoffeeShopApplication;
import com.aura.coffeeshop.common.persistence.CodedEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.CacheControl;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    /**
     * Prefixes every controller in this application with {@code app.api.base-path} (e.g. {@code /api/v1}),
     * so controllers only declare their own path ({@code /carts}) and the version lives in one place.
     * Third-party controllers (springdoc's /v3/api-docs, actuator) are outside the package and stay unprefixed.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(properties.api().basePath(),
                HandlerTypePredicate.forBasePackageClass(CoffeeShopApplication.class));
    }

    /**
     * Serves uploaded images from {@code app.storage.upload-dir} at the path of {@code app.storage.public-url}
     * (e.g. {@code /uploads/products/<uuid>.jpg}). File names are random and never reused, so they can be cached forever.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String urlPath = URI.create(properties.storage().publicUrl()).getPath().replaceAll("/+$", "");
        String location = Path.of(properties.storage().uploadDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(urlPath + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(properties.api().basePath() + "/**")
                .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /** Lets query params like {@code ?status=brewing} bind to {@link CodedEnum} types by code. */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new CodedEnumConverterFactory());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class CodedEnumConverterFactory implements ConverterFactory<String, Enum> {
        @Override
        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            if (CodedEnum.class.isAssignableFrom(targetType)) {
                return source -> source.isBlank() ? null : (T) CodedEnum.fromCode((Class) targetType, source);
            }
            return source -> source.isBlank() ? null : (T) Enum.valueOf(targetType, source.trim());
        }
    }
}
