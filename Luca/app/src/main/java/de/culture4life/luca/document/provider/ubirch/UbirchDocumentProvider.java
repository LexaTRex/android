package de.culture4life.luca.document.provider.ubirch;

import android.util.Base64;

import com.nexenio.rxkeystore.RxKeyStore;
import com.nexenio.rxkeystore.provider.hash.RxHashProvider;
import com.nexenio.rxkeystore.provider.hash.Sha256HashProvider;
import com.nexenio.rxkeystore.util.RxBase64;

import de.culture4life.luca.document.DocumentParsingException;
import de.culture4life.luca.document.DocumentVerificationException;
import de.culture4life.luca.document.provider.DocumentProvider;
import de.culture4life.luca.registration.Person;

import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static de.culture4life.luca.document.DocumentVerificationException.Reason.UNKNOWN;

public class UbirchDocumentProvider extends DocumentProvider<UbirchDocument> {

    static final String URL_PREFIX = "https://verify.govdigital.de/v/gd/#";
    private static final RxHashProvider HASH_PROVIDER = new Sha256HashProvider(new RxKeyStore());
    private static final String VERIFICATION_ENDPOINT = "https://verify.prod.ubirch.com/api/upp/verify/record";

    @Override
    public Single<Boolean> canParse(@NonNull String encodedData) {
        return Single.fromCallable(() -> encodedData.startsWith(URL_PREFIX));
    }

    @Override
    public Single<UbirchDocument> parse(@NonNull String encodedData) {
        return Single.fromCallable(() -> new UbirchDocument(encodedData))
                .onErrorResumeNext(throwable -> Single.error(new DocumentParsingException(throwable)));
    }

    @Override
    public Completable validate(@NonNull UbirchDocument document, @NonNull Person person) {
        return super.validate(document, person)
                .andThen(Single.fromCallable(document::toCompactJson)
                        .map(json -> json.getBytes(StandardCharsets.UTF_8))
                        .flatMap(HASH_PROVIDER::hash)
                        .flatMap(data -> RxBase64.encode(data, Base64.NO_WRAP))
                        .flatMapCompletable(encodedHash -> {
                            OkHttpClient client = new OkHttpClient();
                            RequestBody body = RequestBody.create(encodedHash, MediaType.parse("text/plain"));
                            Request request = new Request.Builder()
                                    .url(VERIFICATION_ENDPOINT)
                                    .post(body)
                                    .build();

                            Response response = client.newCall(request).execute();
                            if (response.isSuccessful()) {
                                return Completable.complete();
                            } else {
                                return Completable.error(new IllegalStateException("Verification request was not successful, status code " + response.code()));
                            }
                        })
                        .onErrorResumeNext(throwable -> Completable.error(new DocumentVerificationException(UNKNOWN, "Unable to verify document", throwable))));
    }

}
