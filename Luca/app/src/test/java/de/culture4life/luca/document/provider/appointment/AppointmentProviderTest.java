package de.culture4life.luca.document.provider.appointment;

import de.culture4life.luca.document.Document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import androidx.test.runner.AndroidJUnit4;

import static junit.framework.TestCase.assertEquals;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class AppointmentProviderTest {

    public static final String VALID_APPOINTMENT = "https://app.luca-app.de/webapp/appointment/?timestamp=1622559373000&type=Kostenloser%20Covid%2019%20B%C3%BCrgertest&lab=Test%20NOW%21%20Schnelltestzentrum%20PACHA&address=Maximiliansplatz%205%2C%2080333%20M%C3%BCnchen&qrCode=TerminID_1353d9b0-60f2-4f56-a816-1282b7faa04f";
    public static final String VALID_APPOINTMENT_2 = "https://app.luca-app.de/webapp/appointment/?timestamp=1624999890419&type=COVIDtest&lab=BestLab&address=Heaven&qrCode=HidfSF8fnsS54fSFkjdDsf3s3";
    public static final String UNSUPPORTED_APPOINTMENT = "https://testverify.io/v1#eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ2IjoyLCJuIjoiYTZhODVkODUyZTA1OTFkZjM2ODUwZTFmMDc1MTFkZWE0MmFjYjAxMjljNzFkZjk1ZTM2YWVkNzhkYWU5ZTNhOCIsInQiOjE2MjAyNzU3NDAsImMiOiJwIiwiciI6InAiLCJsIjoiQ292aW1lZGljYWwgR21iSCIsImQiOiJQcm9mLiBIYW5zIiwiZWQiOiJpTHhpSFZzck5oZFh6UDNoeEZkd1p3Z1ludDVGTUYzVXo3MmJHOHMxcytxV2hzZEgxeGJ4R1Z0SHZzbVwvZkRLXC9zWUczdVwvR0pKd3BXTldSZ2xHOGs0SnhyKytCeDBwWStudjlqb0diWm5lWEt4Ulp6T3RSWnlxeEY2cExrIn0.WocR6aa8EX1WEOKxES_gFnvfJnrg6xLzm1cwZ453StqubQPlMjG-JdZofVa4NgTRUCrxDvcQd8M-wQxksM79Dpy0_tOP2mHA59V5LTsVSVzk7teS6cTGhy1nGqZIfu3ORvOqTvxJmuBtT-Z8TGnJzkTTMNx_t8mPSBTHCJX9YQE0APXSnusiy5LF4iQTpYrgKEH0IZTT4gIx6-SbNpkuVmJE6RxVvjAdnlnTS6lqtr9jplaNw8L6gDw5s0zZ5z8xytuWvceRap_GOTeCxdmg-8f4EghjMJFea8T5WwfZY4BDJbEawsAcOY-ErS4Ey3M_W8PYaPTZWmClOiJGsCeU8w";

    private AppointmentProvider appointmentProvider;

    @Before
    public void setUp() {
        appointmentProvider = new AppointmentProvider();
    }

    @Test
    public void canParse_validAppointment_emitsTrue() {
        appointmentProvider.canParse(VALID_APPOINTMENT)
                .test()
                .assertValue(true);
    }

    @Test
    public void canParse_validAppointment2_emitsTrue() {
        appointmentProvider.canParse(VALID_APPOINTMENT_2)
                .test()
                .assertValue(true);
    }

    @Test
    public void canParse_invalidData_emitsFalse() {
        appointmentProvider.canParse(UNSUPPORTED_APPOINTMENT)
                .test()
                .assertValue(false);
    }

    @Test
    public void parse_validAppointment_parsesData() {
        appointmentProvider.parse(VALID_APPOINTMENT)
                .test()
                .assertValue(appointment -> {
                    assertEquals("Kostenloser Covid 19 Bürgertest", appointment.type);
                    assertEquals("Test NOW! Schnelltestzentrum PACHA", appointment.lab);
                    assertEquals("Maximiliansplatz 5, 80333 München", appointment.address);
                    assertEquals("1622559373000", appointment.timestamp);
                    assertEquals("TerminID_1353d9b0-60f2-4f56-a816-1282b7faa04f", appointment.qrCode);
                    assertEquals(Document.TYPE_APPOINTMENT, appointment.getDocument().getType());
                    assertEquals(1622559373000L, appointment.getDocument().getTestingTimestamp());
                    return true;
                });
    }

    @Test
    public void parse_validAppointment2_parsesData() {
        appointmentProvider.parse(VALID_APPOINTMENT_2)
                .test()
                .assertValue(appointment -> {
                    assertEquals("COVIDtest", appointment.type);
                    assertEquals("BestLab", appointment.lab);
                    assertEquals("Heaven", appointment.address);
                    assertEquals("1624999890419", appointment.timestamp);
                    assertEquals("HidfSF8fnsS54fSFkjdDsf3s3", appointment.qrCode);
                    assertEquals(Document.TYPE_APPOINTMENT, appointment.getDocument().getType());
                    return true;
                });
    }

}