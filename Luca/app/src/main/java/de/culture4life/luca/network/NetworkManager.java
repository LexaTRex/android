package de.culture4life.luca.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.LucaApplication;
import de.culture4life.luca.Manager;
import de.culture4life.luca.network.endpoints.LucaEndpointsV3;
import de.culture4life.luca.network.endpoints.LucaEndpointsV4;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import okhttp3.Cache;
import okhttp3.CertificatePinner;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class NetworkManager extends Manager {

    private static final int CACHE_SIZE = 1024 * 1024 * 10;
    private static final long DEFAULT_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final String USER_AGENT = createUserAgent();

    private final RxJava3CallAdapterFactory rxAdapter;

    private LucaEndpointsV3 lucaEndpointsV3;
    private LucaEndpointsV4 lucaEndpointsV4;
    private ConnectivityManager connectivityManager;

    private final BehaviorSubject<Boolean> connectivityStateSubject = BehaviorSubject.create();

    @Nullable
    private BroadcastReceiver connectivityReceiver;

    public NetworkManager() {
        rxAdapter = RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io());
    }

    @Override
    protected Completable doInitialize(@NonNull Context context) {
        return initializeConnectivityReceiver();
    }

    @Override
    public void dispose() {
        super.dispose();
        unregisterConnectivityReceiver();
    }

    private Retrofit createRetrofit(int version) {
        Gson gson = new GsonBuilder()
                .setLenient()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        OkHttpClient okHttpClient = createOkHttpClient();

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL + "/api/v" + version + "/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(rxAdapter)
                .client(okHttpClient)
                .build();
    }

    @NonNull
    private OkHttpClient createOkHttpClient() {
        Interceptor userAgentInterceptor = chain -> chain.proceed(chain.request()
                .newBuilder()
                .header("User-Agent", USER_AGENT)
                .build());

        Interceptor timeoutInterceptor = chain -> {
            int timeout = (int) getRequestTimeout(chain.request());
            return chain.withConnectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .withReadTimeout(timeout, TimeUnit.MILLISECONDS)
                    .withWriteTimeout(timeout, TimeUnit.MILLISECONDS)
                    .proceed(chain.request());
        };

        Interceptor cdnInterceptor = chain -> chain.proceed(replaceHostWithCdnIfRequired(chain.request()));

        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                .add("**.luca-app.de", "sha256/wjD2X9ht0iXPN2sSXiXd2aF6ar5cxHOmXZnnkAiwVpU=") // CN=*.luca-app.de,O=neXenio GmbH,L=Berlin,ST=Berlin,C=DE,2.5.4.5=#130c43534d303233353532353339
                .build();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor)
                .addInterceptor(timeoutInterceptor)
                .addInterceptor(cdnInterceptor)
                .cache(new Cache(context.getCacheDir(), CACHE_SIZE));

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        } else {
            builder.certificatePinner(certificatePinner);
        }

        if (LucaApplication.IS_USING_STAGING_ENVIRONMENT) {
            builder.authenticator((route, response) -> {
                String credential = Credentials.basic(BuildConfig.STAGING_API_USERNAME, BuildConfig.STAGING_API_PASSWORD);
                return response.request().newBuilder()
                        .header("Authorization", credential)
                        .build();
            });
        }

        return builder.build();
    }

    public Single<LucaEndpointsV3> getLucaEndpointsV3() {
        return Single.fromCallable(() -> {
            if (lucaEndpointsV3 == null) {
                lucaEndpointsV3 = createRetrofit(3).create(LucaEndpointsV3.class);
            }
            return lucaEndpointsV3;
        });
    }

    public Single<LucaEndpointsV4> getLucaEndpointsV4() {
        return Single.fromCallable(() -> {
            if (lucaEndpointsV4 == null) {
                lucaEndpointsV4 = createRetrofit(4).create(LucaEndpointsV4.class);
            }
            return lucaEndpointsV4;
        });
    }

    public Completable assertNetworkConnected() {
        return isNetworkConnected()
                .flatMapCompletable(isNetworkConnected -> {
                    if (isNetworkConnected) {
                        return Completable.complete();
                    } else {
                        return Completable.error(new NetworkUnavailableException("Network is not connected"));
                    }
                });
    }

    public Single<Boolean> isNetworkConnected() {
        return Single.defer(() -> getInitializedField(connectivityManager))
                .flatMapMaybe(manager -> Maybe.fromCallable(manager::getActiveNetworkInfo))
                .map(NetworkInfo::isConnectedOrConnecting)
                .defaultIfEmpty(false);
    }

    private static String createUserAgent() {
        String appVersionName = BuildConfig.VERSION_NAME;
        return "luca/Android " + appVersionName;
    }

    private static long getRequestTimeout(@NonNull Request request) {
        long timeout;
        String path = request.url().encodedPath();
        if (path.contains("/sms/request")) {
            timeout = TimeUnit.SECONDS.toMillis(45);
        } else {
            timeout = DEFAULT_REQUEST_TIMEOUT;
        }
        return timeout;
    }

    protected static boolean useCdn(@NonNull Request request) {
        String path = request.url().encodedPath();
        return path.contains("notifications") || path.contains("healthDepartments");
    }

    protected static Request replaceHostWithCdn(@NonNull Request request) {
        String cdnHost = request.url().host().replaceFirst("app", "data");
        HttpUrl cdnUrl = request.url().newBuilder()
                .host(cdnHost)
                .build();
        return request.newBuilder()
                .url(cdnUrl)
                .build();
    }

    protected static Request replaceHostWithCdnIfRequired(@NonNull Request request) {
        if (useCdn(request)) {
            return replaceHostWithCdn(request);
        } else {
            return request;
        }
    }

    public static boolean isHttpException(@NonNull Throwable throwable, int expectedStatusCode) {
        if (!(throwable instanceof HttpException)) {
            return false;
        }
        int actualStatusCode = ((HttpException) throwable).code();
        return expectedStatusCode == actualStatusCode;
    }

    public Observable<Boolean> getConnectivityStateAndChanges() {
        return connectivityStateSubject;
    }

    private Completable initializeConnectivityReceiver() {
        return Completable.fromAction(() -> {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            connectivityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    managerDisposable.add(
                            isNetworkConnected()
                                    .doOnSuccess(isConnected -> Timber.d("Connectivity state changed: isConnected=%b", isConnected))
                                    .doOnSuccess(connectivityStateSubject::onNext)
                                    .onErrorComplete()
                                    .subscribe()
                    );
                }
            };

            context.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        });
    }

    private void unregisterConnectivityReceiver() {
        if (connectivityReceiver != null) {
            context.unregisterReceiver(connectivityReceiver);
            connectivityReceiver = null;
            connectivityStateSubject.onComplete();
        }
    }

}
