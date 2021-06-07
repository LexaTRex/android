package de.culture4life.luca.testing.provider;

import de.culture4life.luca.registration.RegistrationData;
import de.culture4life.luca.testing.TestResultVerificationException;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import static de.culture4life.luca.testing.TestResultVerificationException.Reason.NAME_MISMATCH;

public abstract class TestResultProvider<TestResultType extends ProvidedTestResult> {

    public abstract Single<Boolean> canParse(@NonNull String encodedData);

    public abstract Single<TestResultType> parse(@NonNull String encodedData);

    public Completable verify(@NonNull String encodedData) {
        return Completable.complete();
    }

    public Single<TestResultType> verifyParseAndValidate(@NonNull String encodedData, @NonNull RegistrationData registrationData) {
        return verify(encodedData)
                .andThen(parse(encodedData))
                .flatMap(testResult -> validate(testResult, registrationData)
                        .andThen(Single.just(testResult)));
    }

    public Completable validate(@NonNull TestResultType testResult, @NonNull RegistrationData registrationData) {
        return Completable.fromAction(() -> {
            if (!registrationData.getFirstName().equals(testResult.getLucaTestResult().getFirstName())) {
                throw new TestResultVerificationException(NAME_MISMATCH);
            } else if (!registrationData.getLastName().equals(testResult.getLucaTestResult().getLastName())) {
                throw new TestResultVerificationException(NAME_MISMATCH);
            }
        });
    }

}
