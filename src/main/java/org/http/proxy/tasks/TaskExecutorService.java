package org.http.proxy.tasks;

import java.util.concurrent.*;

public class TaskExecutorService {

    private static ExecutorService executorService;

    static {
        if (executorService == null || executorService.isShutdown()) {
            final ThreadFactory parent = Executors
                    .defaultThreadFactory();
            ThreadFactory factory = new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = parent.newThread(runnable);
                    thread.setDaemon(true);
                    return thread;
                }
            };

            executorService = Executors
                    .newCachedThreadPool(factory);
        }
    }

    public static void shutdown() {
        synchronized (executorService) {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
    }

    public static void submitCommon(Callable callable) {
        executorService.submit(callable);
    }
}
