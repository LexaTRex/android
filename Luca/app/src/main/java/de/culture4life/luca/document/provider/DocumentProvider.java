package de.culture4life.luca.document.provider;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.List;
import java.util.NoSuchElementException;

import de.culture4life.luca.children.Child;
import de.culture4life.luca.document.DocumentVerificationException;
import de.culture4life.luca.registration.Person;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.exceptions.CompositeException;

import static de.culture4life.luca.document.DocumentVerificationException.Reason.DATE_OF_BIRTH_TOO_OLD_FOR_CHILD;
import static de.culture4life.luca.document.DocumentVerificationException.Reason.NAME_MISMATCH;
import static de.culture4life.luca.document.DocumentVerificationException.Reason.TIMESTAMP_IN_FUTURE;
import static de.culture4life.luca.document.DocumentVerificationException.Reason.UNKNOWN;

public abstract class DocumentProvider<DocumentType extends ProvidedDocument> {

    public abstract Single<Boolean> canParse(@NonNull String encodedData);

    public abstract Single<DocumentType> parse(@NonNull String encodedData);

    public Completable verify(@NonNull String encodedData) {
        return Completable.complete();
    }

    public Single<DocumentType> verifyParseAndValidate(@NonNull String encodedData, @NonNull Person person, @NonNull List<Child> children) {
        return verify(encodedData)
                .andThen(parse(encodedData))
                .flatMap(document -> validate(document, person, children)
                        .andThen(Single.just(document)));
    }

    public Completable validate(@NonNull DocumentType document, @NonNull Person person, @NonNull List<Child> children) {
        Observable<Person> personsToValidate = Observable.just(person)
                .mergeWith(Observable.fromIterable(children))
                .mergeWith(Observable.fromIterable(children)
                        .filter(child -> !child.getLastName().equals(person.getLastName()))
                        .map(child -> new Child(child.getFirstName(), person.getLastName())))
                .distinct();

        return Observable.mergeDelayError(
                personsToValidate.map(personToValidate -> validateName(document, personToValidate)
                        .andThen(setName(document, personToValidate))
                        .andThen(Observable.just(personToValidate))
                        .onErrorResumeWith(Observable.empty())
                )
        )
                .firstOrError()
                .onErrorResumeNext(throwable -> {
                    if (throwable instanceof CompositeException) {
                        throwable = ((CompositeException) throwable).getExceptions().get(0);
                    }
                    if (throwable instanceof NoSuchElementException) {
                        return Single.error(new DocumentVerificationException(NAME_MISMATCH, "Could not find a matching name"));
                    } else {
                        return Single.error(new DocumentVerificationException(UNKNOWN, throwable));
                    }
                })
                .flatMapCompletable(firstPerson -> validate(document, firstPerson));
    }

    public Completable validate(@NonNull DocumentType document, @NonNull Person person) {
        return validateName(document, person)
                .andThen(Completable.defer(() -> {
                    if (person instanceof Child) {
                        return validateChildAge(document);
                    } else {
                        return Completable.complete();
                    }
                }));
    }

    private Completable setName(@NonNull DocumentType document, @NonNull Person person) {
        return Completable.fromAction(() -> {
            document.document.setFirstName(person.getFirstName());
            document.document.setLastName(person.getLastName());
        });
    }

    protected Completable validateName(@NonNull DocumentType document, @NonNull Person person) {
        return Completable.fromAction(() -> {
            if (!Person.Companion.compare(person.getFirstName(), document.getDocument().getFirstName())
                    || !Person.Companion.compare(person.getLastName(), document.getDocument().getLastName())) {
                throw new DocumentVerificationException(NAME_MISMATCH);
            }

        }).andThen(validateTime(document.document.getTestingTimestamp()));
    }

    protected Completable validateChildAge(@NonNull DocumentType document) {
        return Completable.fromAction(() -> validateChildAge(document.getDocument().getDateOfBirth()));
    }

    protected static void validateChildAge(long dateOfBirth) throws DocumentVerificationException {
        DateTime maximumDate = new DateTime().minusYears(Child.MAXIMUM_AGE_IN_YEARS);
        DateTime dateTime = new DateTime(dateOfBirth);
        if (dateOfBirth != 0 && (dateTime.isBefore(maximumDate) || dateTime.isAfterNow())) {
            throw new DocumentVerificationException(DATE_OF_BIRTH_TOO_OLD_FOR_CHILD);
        }
    }

    public static Completable validateTime(long testingTimestamp) {
        return Completable.fromAction(() -> {
            if (testingTimestamp > System.currentTimeMillis()) {
                throw new DocumentVerificationException(TIMESTAMP_IN_FUTURE);
            }
        });
    }

}
