package de.culture4life.luca.testing;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;

public class TestResults extends ArrayList<TestResult> {

    public TestResults() {
    }

    public TestResults(@NonNull Collection<? extends TestResult> testResults) {
        super(testResults);
    }

}
