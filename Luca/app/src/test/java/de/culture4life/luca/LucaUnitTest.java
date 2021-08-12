package de.culture4life.luca;

import androidx.test.core.app.ApplicationProvider;

import net.lachlanmckee.timberjunit.TimberTestRule;

import org.junit.Rule;

public class LucaUnitTest {

    @Rule
    public TimberTestRule timberTestRule = TimberTestRule.logAllAlways();

    protected LucaApplication application;

    public LucaUnitTest() {
        this.application = ApplicationProvider.getApplicationContext();
    }

}
