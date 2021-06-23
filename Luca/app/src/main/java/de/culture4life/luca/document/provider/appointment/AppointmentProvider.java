package de.culture4life.luca.document.provider.appointment;

import de.culture4life.luca.registration.RegistrationData;
import de.culture4life.luca.document.DocumentParsingException;
import de.culture4life.luca.document.DocumentManager;
import de.culture4life.luca.document.provider.DocumentProvider;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class AppointmentProvider extends DocumentProvider<Appointment> {

    @Override
    public Single<Boolean> canParse(@NonNull @NotNull String encodedData) {
        return Single.fromCallable(() -> DocumentManager.isAppointment(encodedData))
                .onErrorReturnItem(false);
    }

    @Override
    public Single<Appointment> parse(@NonNull @NotNull String encodedData) {
        return Single.fromCallable(() -> new Appointment(encodedData))
                .onErrorResumeNext(throwable -> Single.error(new DocumentParsingException(throwable)));
    }

    @Override
    public Completable validate(@NonNull @NotNull Appointment document, @NonNull @NotNull RegistrationData registrationData) {
        return Completable.complete();
    }

}
