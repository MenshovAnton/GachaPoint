package ru.menshovanton.gachapoint;

import android.app.Application;

import ru.menshovanton.gachapoint.di.AppContainer;

public class App extends Application {

    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
