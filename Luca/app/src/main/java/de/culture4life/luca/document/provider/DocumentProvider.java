package de.culture4life.luca.document.provider;

import de.culture4life.luca.document.DocumentVerificationException;
import de.culture4life.luca.registration.RegistrationData;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import static de.culture4life.luca.document.DocumentVerificationException.Reason.NAME_MISMATCH;

public abstract class DocumentProvider<DocumentType extends ProvidedDocument> {

    public abstract Single<Boolean> canParse(@NonNull String encodedData);

    public abstract Single<DocumentType> parse(@NonNull String encodedData);

    public Completable verify(@NonNull String encodedData) {
        return Completable.complete();
    }

    public Single<DocumentType> verifyParseAndValidate(@NonNull String encodedData, @NonNull RegistrationData registrationData) {
        return verify(encodedData)
                .andThen(parse(encodedData))
                .flatMap(document -> validate(document, registrationData)
                        .andThen(Single.just(document)));
    }

    public Completable validate(@NonNull DocumentType document, @NonNull RegistrationData registrationData) {
        return Completable.fromAction(() -> {
            if (!registrationData.getFirstName().equalsIgnoreCase(document.getDocument().getFirstName())) {
                throw new DocumentVerificationException(NAME_MISMATCH);
            } else if (!registrationData.getLastName().equalsIgnoreCase(document.getDocument().getLastName())) {
                throw new DocumentVerificationException(NAME_MISMATCH);
            }
        });
    }

}
