package de.culture4life.luca.document.provider.appointment;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import de.culture4life.luca.document.DocumentManager;
import de.culture4life.luca.document.DocumentParsingException;
import de.culture4life.luca.document.provider.DocumentProvider;
import de.culture4life.luca.registration.Person;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class AppointmentProvider extends DocumentProvider<Appointment> {

    @Override
    public Single<Boolean> canParse(@NonNull String encodedData) {
        return Single.fromCallable(() -> DocumentManager.isAppointment(encodedData))
                .onErrorReturnItem(false);
    }

    @Override
    public Single<Appointment> parse(@NonNull String encodedData) {
        return Single.fromCallable(() -> new Appointment(encodedData))
                .onErrorResumeNext(throwable -> Single.error(new DocumentParsingException(throwable)));
    }

    @Override
    protected Completable validateName(@NonNull Appointment document, @NonNull Person person) {
        return Completable.complete();
    }

    @Override
    protected Completable validateChildAge(@NonNull Appointment document) {
        return Completable.complete();
    }

}
