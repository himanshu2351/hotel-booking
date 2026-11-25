package com.project.hotel_booking.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryLockManagerTest {
    private final InventoryLockManager lockManager = new InventoryLockManager();

    @Test
    void returnsOperationResultForInventoryKey() {
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();

        String result = lockManager.withLock(propertyId, roomTypeId, () -> "completed");

        assertThat(result).isEqualTo("completed");
    }

    @Test
    void releasesLockWhenOperationThrows() {
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        assertThatThrownBy(() -> lockManager.withLock(propertyId, roomTypeId, () -> {
            throw new IllegalStateException("failure");
        })).isInstanceOf(IllegalStateException.class);

        AtomicInteger completedCalls = new AtomicInteger();
        lockManager.withLock(propertyId, roomTypeId, () -> completedCalls.incrementAndGet());

        assertThat(completedCalls).hasValue(1);
    }
    @Test
    void allowsDifferentInventoryKeysToProceedIndependently() throws Exception {
        UUID propertyId = UUID.randomUUID();
        UUID firstRoomTypeId = UUID.randomUUID();
        UUID secondRoomTypeId = UUID.randomUUID();
        CountDownLatch firstOperationStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstOperation = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> firstOperation = executor.submit(() -> lockManager.withLock(
                    propertyId, firstRoomTypeId, () -> {
                        firstOperationStarted.countDown();
                        await(releaseFirstOperation);
                        return "first";
                    }));
            assertThat(firstOperationStarted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<String> secondOperation = executor.submit(() -> lockManager.withLock(
                    propertyId, secondRoomTypeId, () -> "second"));

            assertThat(secondOperation.get(1, TimeUnit.SECONDS)).isEqualTo("second");
            releaseFirstOperation.countDown();
            assertThat(firstOperation.get(5, TimeUnit.SECONDS)).isEqualTo("first");
        } finally {
            releaseFirstOperation.countDown();
            executor.shutdownNow();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting in test", exception);
        }
    }

}
