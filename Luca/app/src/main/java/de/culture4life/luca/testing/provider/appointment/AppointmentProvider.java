package de.culture4life.luca.testing.provider.appointment;

import de.culture4life.luca.registration.RegistrationData;
import de.culture4life.luca.testing.TestResultParsingException;
import de.culture4life.luca.testing.TestingManager;
import de.culture4life.luca.testing.provider.TestResultProvider;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class AppointmentProvider extends TestResultProvider<Appointment> {

    @Override
    public Single<Boolean> canParse(@NonNull @NotNull String encodedData) {
        return Single.fromCallable(() -> TestingManager.isAppointment(encodedData))
                .onErrorReturnItem(false);
    }

    @Override
    public Single<Appointment> parse(@NonNull @NotNull String encodedData) {
        return Single.fromCallable(() -> new Appointment(encodedData))
                .onErrorResumeNext(throwable -> Single.error(new TestResultParsingException(throwable)));
    }

    @Override
    public Completable validate(@NonNull @NotNull Appointment testResult, @NonNull @NotNull RegistrationData registrationData) {
        return Completable.complete();
    }

}
