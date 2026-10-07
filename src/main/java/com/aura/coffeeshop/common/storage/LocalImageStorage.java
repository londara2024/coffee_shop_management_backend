package com.aura.coffeeshop.common.storage;

import com.aura.coffeeshop.common.config.AppProperties;
import com.aura.coffeeshop.common.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Writes images to {@code app.storage.upload-dir}; they are served back at {@code app.storage.public-url}
 * (see {@link com.aura.coffeeshop.common.config.WebConfig}).
 *
 * <p>The file type is detected from the file's leading bytes, never from the client-supplied name or
 * Content-Type, and files get random names, so uploads cannot overwrite each other or escape the folder.
 */
@Slf4j
@Component
public class LocalImageStorage implements ImageStorage {

    private static final Pattern FOLDER = Pattern.compile("^[a-z0-9-]+$");

    private final Path root;
    private final String publicUrl;
    private final long maxBytes;

    public LocalImageStorage(AppProperties properties) {
        AppProperties.Storage storage = properties.storage();
        this.root = Path.of(storage.uploadDir()).toAbsolutePath().normalize();
        this.publicUrl = storage.publicUrl().replaceAll("/+$", "");
        this.maxBytes = storage.maxImageSize().toBytes();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory " + root, e);
        }
        log.info("Image uploads stored in {} and served from {}", root, publicUrl);
    }

    public Path root() {
        return root;
    }

    @Override
    public StoredImage store(MultipartFile file, String folder) {
        if (!FOLDER.matcher(folder).matches()) {
            throw new IllegalArgumentException("Invalid storage folder: " + folder);
        }
        if (file == null || file.isEmpty()) {
            throw BusinessRuleException.badRequest("The uploaded file is empty");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image is larger than the " + (maxBytes / (1024 * 1024)) + " MB limit");
        }
        ImageType type = detect(file);

        String fileName = UUID.randomUUID() + "." + type.extension;
        Path target = root.resolve(folder).resolve(fileName).normalize();
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store uploaded image", e);
        }
        return new StoredImage(publicUrl + "/" + folder + "/" + fileName, type.contentType, file.getSize());
    }

    @Override
    public void deleteIfManaged(String url) {
        if (url == null || !url.startsWith(publicUrl + "/")) {
            return;
        }
        Path file = root.resolve(url.substring(publicUrl.length() + 1)).normalize();
        if (!file.startsWith(root)) {
            return; // never delete outside the upload directory
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete old image {}: {}", file, e.getMessage());
        }
    }

    private static ImageType detect(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded image", e);
        }
        byte[] h = Arrays.copyOf(head, read);
        for (ImageType type : ImageType.values()) {
            if (type.matches(h)) {
                return type;
            }
        }
        throw BusinessRuleException.badRequest("Only JPEG, PNG, WebP or GIF images are allowed");
    }

    private enum ImageType {
        JPEG("jpg", "image/jpeg") {
            boolean matches(byte[] h) {
                return h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            }
        },
        PNG("png", "image/png") {
            boolean matches(byte[] h) {
                return startsWith(h, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            }
        },
        GIF("gif", "image/gif") {
            boolean matches(byte[] h) {
                return startsWith(h, "GIF87a".getBytes()) || startsWith(h, "GIF89a".getBytes());
            }
        },
        WEBP("webp", "image/webp") {
            boolean matches(byte[] h) {
                return h.length >= 12 && startsWith(h, "RIFF".getBytes())
                        && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            }
        };

        final String extension;
        final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        abstract boolean matches(byte[] head);

        static boolean startsWith(byte[] data, byte[] prefix) {
            if (data.length < prefix.length) {
                return false;
            }
            for (int i = 0; i < prefix.length; i++) {
                if (data[i] != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
