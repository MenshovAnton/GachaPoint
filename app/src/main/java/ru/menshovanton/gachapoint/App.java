package ru.menshovanton.gachapoint;

import android.app.Application;

import ru.menshovanton.gachapoint.data.local.AppLogger;
import ru.menshovanton.gachapoint.di.AppContainer;

public class App extends Application {

    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLogger.init(this);

        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            AppLogger.e("FatalCrash", "Uncaught exception on thread: " + thread.getName(), throwable);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        appContainer = new AppContainer(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
