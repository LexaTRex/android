package de.culture4life.luca.network.endpoints;

import com.google.gson.JsonObject;

import io.reactivex.rxjava3.core.Single;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface LucaEndpointsV4 {

    /*
        Notifications
     */

    @GET("notifications/config")
    Single<JsonObject> getNotificationConfig();

    @GET("notifications/traces")
    Single<ResponseBody> getNotifications();

    @GET("notifications/traces/{chunkId}")
    Single<ResponseBody> getNotifications(@Path("chunkId") String chunkId);

}
