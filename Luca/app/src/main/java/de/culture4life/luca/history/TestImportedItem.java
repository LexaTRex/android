package de.culture4life.luca.history;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import de.culture4life.luca.testing.TestResult;

import androidx.annotation.NonNull;

public class TestImportedItem extends HistoryItem {

    @SerializedName("testResult")
    @Expose
    private TestResult testResult;

    public TestImportedItem() {
        super(HistoryItem.TYPE_TEST_RESULT_IMPORTED);
    }

    public TestImportedItem(@NonNull TestResult testResult) {
        this.testResult = testResult;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public void setTestResult(TestResult testResult) {
        this.testResult = testResult;
    }

    @Override
    public String toString() {
        return "TestImportedItem{" +
                "testResult=" + testResult +
                "} " + super.toString();
    }

}
