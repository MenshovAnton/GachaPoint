package ru.menshovanton.gachapoint.data.local;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NetworkExecutor {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private NetworkExecutor() {}

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }

    public static void postToMain(Runnable task) {
        MAIN_HANDLER.post(task);
    }
}
