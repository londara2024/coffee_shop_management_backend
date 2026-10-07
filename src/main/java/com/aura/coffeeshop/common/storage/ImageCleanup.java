package com.aura.coffeeshop.common.storage;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Keeps files on disk consistent with the database when an image URL is replaced or removed:
 * nothing is deleted until the transaction outcome is known.
 */
public final class ImageCleanup {

    private ImageCleanup() {
    }

    /**
     * After commit, delete {@code previousUrl}; after rollback, delete the just-stored {@code newUrl} instead.
     * Must be called inside a transaction.
     */
    public static void onReplace(ImageStorage storage, String previousUrl, String newUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                storage.deleteIfManaged(status == STATUS_COMMITTED ? previousUrl : newUrl);
            }
        });
    }

    /** Delete {@code url} once the transaction commits (e.g. the row that referenced it was removed). */
    public static void afterCommit(ImageStorage storage, String url) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.deleteIfManaged(url);
            }
        });
    }
}
