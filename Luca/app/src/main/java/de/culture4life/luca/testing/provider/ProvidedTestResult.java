package de.culture4life.luca.testing.provider;

import de.culture4life.luca.testing.TestResult;

public abstract class ProvidedTestResult {

    protected TestResult lucaTestResult;

    public ProvidedTestResult() {
        this.lucaTestResult = new TestResult();
    }

    public TestResult getLucaTestResult() {
        return lucaTestResult;
    }

    public void setLucaTestResult(TestResult lucaTestResult) {
        this.lucaTestResult = lucaTestResult;
    }

}
