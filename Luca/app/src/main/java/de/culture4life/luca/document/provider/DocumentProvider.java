package de.culture4life.luca.document.provider;

import androidx.annotation.NonNull;

import javax.annotation.Nonnull;

import de.culture4life.luca.document.DocumentVerificationException;
import de.culture4life.luca.registration.RegistrationData;
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
            compare(registrationData.getFirstName(), document.getDocument().getFirstName());
            compare(registrationData.getLastName(), document.getDocument().getLastName());
        });
    }

    private void compare(@NonNull String s1, @Nonnull String s2) throws DocumentVerificationException {
        s1 = removeAcademicTitles(s1);
        s2 = removeAcademicTitles(s2);
        if (!simplify(s1).equalsIgnoreCase(simplify(s2))) {
            throw new DocumentVerificationException(NAME_MISMATCH);
        }
    }

    protected String removeAcademicTitles(String name) {
        name = name.replaceAll("(?i)Prof\\. ", "");
        name = name.replaceAll("(?i)Dr\\. ", "");
        return name;
    }

    protected String simplify(String name) {
        name = name.toUpperCase();
        name = name.replaceAll("[^\\x41-\\x5A]", "");
        return name;
    }

}
