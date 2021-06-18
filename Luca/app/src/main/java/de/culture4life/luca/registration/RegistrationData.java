package de.culture4life.luca.registration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import de.culture4life.luca.network.pojo.ContactData;
import de.culture4life.luca.util.StringSanitizeUtil;

import java.util.UUID;

/**
 * Model of user-provided contact data, entered during registration. Will be transferred into {@link
 * ContactData} and encrypted before leaving the device. Special characters set are
 * automatically removed to keep the dataset cleaner.
 *
 * @see <a href="https://www.luca-app.de/securityoverview/processes/guest_registration.html">Security
 *         Overview: Guest Registration</a>
 */
public class RegistrationData {

    @SerializedName("id")
    @Expose
    private UUID id;

    @SerializedName("firstName")
    @Expose
    private String firstName;

    @SerializedName("lastName")
    @Expose
    private String lastName;

    @SerializedName("phoneNumber")
    @Expose
    private String phoneNumber;

    @SerializedName("email")
    @Expose
    private String email;

    @SerializedName("street")
    @Expose
    private String street;

    @SerializedName("houseNumber")
    @Expose
    private String houseNumber;

    @SerializedName("city")
    @Expose
    private String city;

    @SerializedName("postalCode")
    @Expose
    private String postalCode;

    public RegistrationData() {

    }

    @Override
    public String toString() {
        return "RegistrationData{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", street='" + street + '\'' +
                ", houseNumber='" + houseNumber + '\'' +
                ", city='" + city + '\'' +
                ", postalCode='" + postalCode + '\'' +
                '}';
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = StringSanitizeUtil.sanitize(firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = StringSanitizeUtil.sanitize(lastName);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = StringSanitizeUtil.sanitize(phoneNumber);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = StringSanitizeUtil.sanitize(email);
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = StringSanitizeUtil.sanitize(street);
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = StringSanitizeUtil.sanitize(houseNumber);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = StringSanitizeUtil.sanitize(city);
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = StringSanitizeUtil.sanitize(postalCode);
    }

    /**
     * @return the full name with firstName and lastName, separated by a space
     */
    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

}
