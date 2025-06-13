package com.ppgenarator.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParallelProcessor {

    private static final int CORE_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.min(CORE_COUNT, 16),
            new CustomThreadFactory()
    );

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            EXECUTOR.shutdown();
            try {
                if (!EXECUTOR.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                EXECUTOR.shutdownNow();
            }
        }));
    }

    public static <T, R> List<R> processInParallel(List<T> items, Function<T, R> processor) {
        if (items.size() < 4) {
            // For small lists, process sequentially to avoid overhead
            return items.stream().map(processor).collect(Collectors.toList());
        }

        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> processor.apply(item), EXECUTOR))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public static <T> void processInParallelVoid(List<T> items, java.util.function.Consumer<T> processor) {
        if (items.size() < 4) {
            items.forEach(processor);
            return;
        }

        List<CompletableFuture<Void>> futures = items.stream()
                .map(item -> CompletableFuture.runAsync(() -> processor.accept(item), EXECUTOR))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    private static class CustomThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "PastPaper-Worker-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
