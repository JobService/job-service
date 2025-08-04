/*
 * Copyright 2016-2025 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jobservice.scheduledexecutor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Manages a cache of active QueueServices instances.
 * <p>
 * This class acts as a static facade, hiding the underlying Guava Cache implementation.
 * It ensures that QueueServices are kept in scope until they are confirmed (acked/nacked)
 * or time out.
 */
final class QueueServicesCache {

    private static final Logger LOG = LoggerFactory.getLogger(QueueServicesCache.class);

    private static final long TIMEOUT_MINUTES = ScheduledExecutorConfig.getQueueServicesCacheTimeoutMinutes();

    record Key(String partitionId, String jobId) { }

    private static final Cache<Key, QueueServices> CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                    .removalListener(new QueueServicesRemovalListener())
                    .build();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private QueueServicesCache() {}

    /**
     * Adds a QueueServices instance to the cache, associated with a unique key.
     *
     * @param key      The unique key.
     * @param services The QueueServices instance to cache.
     */
    public static void put(final Key key, final QueueServices services) {
        CACHE.put(key, services);
    }

    /**
     * Retrieves a QueueServices instance from the cache, if it exists.
     *
     * @param key The unique key.
     * @return The cached QueueServices instance, or null if not found.
     */
    public static QueueServices getIfPresent(final Key key) {
        return CACHE.getIfPresent(key);
    }

    /**
     * Explicitly invalidates and removes a QueueServices instance from the cache.
     * This will trigger the removal listener to close the connection.
     *
     * @param key The unique key to invalidate.
     */
    public static void invalidate(final Key key) {
        CACHE.invalidate(key);
    }

    /**
     * Returns the current size of the cache.
     *
     * @return The number of entries in the cache.
     */
    public static long size() {
        return CACHE.size();
    }

    /**
     * The removal listener handles the cleanup logic when an entry is removed from the cache,
     * either by explicit invalidation or timeout.
     */
    private static class QueueServicesRemovalListener implements RemovalListener<Key, QueueServices> {
        @Override
        public void onRemoval(final RemovalNotification<Key, QueueServices> notification) {
            final QueueServices queueServices = notification.getValue();
            if (queueServices == null) {
                return;
            }

            switch (notification.getCause()) {
                case EXPLICIT:
                    LOG.info("Removing QueueServices {} from cache. " +
                            "Reason: Publisher ack, nack, return or shutdown received.", notification.getKey());
                    break;
                case EXPIRED:
                    LOG.error("Removing QueueServices {} from cache. Reason: No publisher ack, nack, return " +
                                    "or shutdown received within timeout period of {} minutes.",
                            notification.getKey(), TIMEOUT_MINUTES);
                    break;
                default:
                    LOG.warn("Removing QueueServices {} from cache. Reason: {} (unexpected)",
                            notification.getKey(), notification.getCause());
                    break;
            }

            // Create a new thread to call QueueServices.close() (prevents a TimeoutException on the main thread)
            LOG.info("Calling close() on QueueServices {}", notification.getKey());
            final Thread cleanupThread = new Thread(queueServices::close);
            cleanupThread.start();
        }
    }
}
