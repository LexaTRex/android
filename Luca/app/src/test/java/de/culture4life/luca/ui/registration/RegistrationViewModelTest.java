package de.culture4life.luca.ui.registration;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class RegistrationViewModelTest {

    private RegistrationViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new RegistrationViewModel(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void getFormattedPhoneNumber_withoutCountryCode_addsGermanCountryCode() {
        assertEquals("+4915112345678", viewModel.getFormattedPhoneNumber("0151 12345678", PhoneNumberUtil.PhoneNumberFormat.E164));
    }

    @Test
    public void getFormattedPhoneNumber_withCountryCode_keepsCountryCode() {
        assertEquals("+4915112345678", viewModel.getFormattedPhoneNumber("+49 151 12345678", PhoneNumberUtil.PhoneNumberFormat.E164));
    }

    @Test
    public void isMobilePhoneNumber_validPhoneStrings_areValid() {
        for (String phoneString : new String[]{
                "+4917112345678",
                "+49 171 12345678",
                "+49/171/12345678",
                "004917112345678",
                "017112345678",
                " 017112345678  ",
                "0 1 7 1 1 2 3 4 5 6 7 8",
        }) {
            Assert.assertTrue(phoneString, viewModel.isValidPhoneNumber(phoneString));
        }
    }

    @Test
    public void isMobilePhoneNumber_invalidPhoneStrings_areInvalid() {
        for (String phoneString : new String[]{
                "",
                "0",
                "12345678",
                "017112345678asdf",
                "                    ",
        }) {
            Assert.assertFalse(phoneString, viewModel.isValidPhoneNumber(phoneString));
        }
    }

    @Test
    public void isValidEMailAddress_validEmailStrings_areValid() {
        for (String emailString : new String[]{
                "a@b.de",
                "user.name@nexenio.com",
        }) {
            Assert.assertTrue(emailString, RegistrationViewModel.isValidEMailAddress(emailString));
        }
    }

    @Test
    public void isValidEMailAddress_invalidEmailStrings_areInvalid() {
        for (String emailString : new String[]{
                "",
                "@",
                "user name",
        }) {
            Assert.assertFalse(emailString, RegistrationViewModel.isValidEMailAddress(emailString));
        }
    }

    @Test
    public void isValidPostalCode_tenCharsWithLetters_isValid() {
        Assert.assertTrue(RegistrationViewModel.isValidPostalCode("12345678AB"));
    }

    @Test
    public void isValidPostalCode_fiveNumbers_isValid() {
        Assert.assertTrue(RegistrationViewModel.isValidPostalCode("12345"));
    }

    @Test
    public void isValidPostalCode_fourNumbers_isValid() {
        Assert.assertTrue(RegistrationViewModel.isValidPostalCode("1234"));
    }

    @Test
    public void isValidPostalCode_threeNumbers_isValid() {
        Assert.assertTrue(RegistrationViewModel.isValidPostalCode("123"));
    }

    @Test
    public void isValidPostalCode_hasSpecialsChars_isInvalid() {
        Assert.assertFalse(RegistrationViewModel.isValidPostalCode("12345!"));
    }

    @Test
    public void isValidPostalCode_isEmpty_isInvalid() {
        Assert.assertFalse(RegistrationViewModel.isValidPostalCode(""));
    }

    @Test
    public void isValidPostalCode_hasMoreThenFifteenChars_isInvalid() {
        Assert.assertFalse(RegistrationViewModel.isValidPostalCode("1234567890123456"));
    }

    @Test
    public void isValidPostalCode_twoChars_isInvalid() {
        Assert.assertFalse(RegistrationViewModel.isValidPostalCode("12"));
    }
}