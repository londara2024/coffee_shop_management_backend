package com.aura.coffeeshop.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Api api, BigDecimal taxRate, String orderCodePrefix, Cors cors, Storage storage) {

    /** @param basePath prefix for every REST controller, e.g. {@code /api/v1} */
    public record Api(String basePath) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /**
     * @param uploadDir    directory on disk where uploaded files are written
     * @param publicUrl    absolute URL the {@code /uploads/**} files are served from (stored in {@code image_url})
     * @param maxImageSize largest accepted image
     */
    public record Storage(String uploadDir, String publicUrl, DataSize maxImageSize) {
    }
}
