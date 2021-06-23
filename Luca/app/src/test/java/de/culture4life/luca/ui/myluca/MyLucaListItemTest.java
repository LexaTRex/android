package de.culture4life.luca.ui.myluca;

import de.culture4life.luca.LucaUnitTest;
import de.culture4life.luca.R;
import de.culture4life.luca.document.Document;
import de.culture4life.luca.document.provider.opentestcheck.OpenTestCheckDocument;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import androidx.test.runner.AndroidJUnit4;

import static org.junit.Assert.assertEquals;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class MyLucaListItemTest extends LucaUnitTest {

    @Test
    public void parse_validData_emitsTestResult() {
        String encodedData = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ2IjoyLCJuIjoiZDg5NzMwNDFlMzVlZGE5ZTcxNmQzZTJmZWQ1NDA0NWU5MTNlYmJhNDM4YmE2YmQ4MzA5ZTg0Zjg1N2ZhNzM5YyIsInQiOjE2MTY3NTg0MjYsImMiOiJmIiwiciI6Im4iLCJsIjoiQ292aW1lZGljYWwgR21iSCIsImQiOiJQcm9mLiBEci4gQ29yb25hZmlnaHQiLCJlZCI6ImRmckdHcTBiVmVIMXRtSGlLRUFkcW01bXNQSTUzY3NqNnJIQnBVdGk3dDhIUHBEdWFzbFlNR3JMZ29lSCtkOGZNdTYzWGNaQUtGYkxQcGlNZTdtT3EzeEZlNkc2YTJPbmJkUzJERkg3N1lwTlBpOVlrNWhMRXM2V01iQm4ifQ.E6KIMvF83THYqbNLaZqiUiDmtVhRmlUMpvZnkkrDXCOerVn6ML70vzLQPHsCDhx4jWF7H9o-3xGCDmZfm20chfsJ9l9TPJNh2fioUfne2ZitOeT93F8uvoZHkH3iEET9PZ_8HJ3mzVkD3P_DQnmQDtPhoyTisv74rzF00YRRaqNEsUe912oHRKhku2uibaRR0mjhuHns2V8fgD3r9eI5qf4A0LlMBRW8ZQb4xYHLV3RsdeslOARfnJnYw3-xFqO_ERQkKp8-7DH8xRgL0mO2PmhvwOGzCCFcn7cW1c0_yL5OhRyTJX9zX3u7zIkBpA8ot8HVNeWWnQHD8OYu8sB1Hw";
        Document document = new OpenTestCheckDocument(encodedData).getDocument();
        TestResultItem item = new TestResultItem(application, document);

        assertEquals(application.getString(R.string.document_outcome_negative), item.getTitle());
        assertEquals(application.getString(R.string.document_type_fast), item.getTopContent().get(0).second);
    }

}