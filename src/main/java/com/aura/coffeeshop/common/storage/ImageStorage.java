package com.aura.coffeeshop.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded images. {@link LocalImageStorage} writes to disk; a cloud implementation
 * (S3, GCS, ...) can replace it without touching the callers.
 */
public interface ImageStorage {

    /**
     * Validates and stores an image under {@code folder} with a generated file name.
     *
     * @throws com.aura.coffeeshop.common.exception.BusinessRuleException 400 when empty or not an allowed image type,
     *                                                                     413 when larger than the configured limit
     */
    StoredImage store(MultipartFile file, String folder);

    /** Deletes a previously stored image. URLs this storage did not create (e.g. /images/... seed files) are ignored. */
    void deleteIfManaged(String url);

    /** @param url public URL to save in {@code image_url} */
    record StoredImage(String url, String contentType, long size) {
    }
}
