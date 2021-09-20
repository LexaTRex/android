package de.culture4life.luca;

import androidx.test.platform.app.InstrumentationRegistry;

public class LucaInstrumentationTest {

    protected LucaApplication application;

    public LucaInstrumentationTest() {
        this.application = getInstrumentedApplication();
    }

    protected LucaApplication getInstrumentedApplication() {
        return (LucaApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    }

    protected <ManagerType extends Manager> ManagerType getInitializedManager(ManagerType manager) {
        initializeManager(manager);
        return manager;
    }

    protected <ManagerType extends Manager> void initializeManager(ManagerType manager) {
        manager.initialize(application).blockingAwait();
    }

    protected <ManagerType extends Manager> void initializeManagers(ManagerType... managers) {
        for (ManagerType manager : managers) {
            initializeManager(manager);
        }
    }

}
