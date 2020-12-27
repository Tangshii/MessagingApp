package com.tange.messagingapp.Notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAA-wYOCYA:APA91bGdoxuQp0Ncf14ttMIIEm_GSQGLzt7A6z55cnbOvSOfoN5WUSgPMSVuv2Pgftw3VJk5azQCzErFElst2rRrhRU_RUgc4GzBBsjtcHEDKqNS8cs7Eumtg99bJNIhNwy2EgWh0rNw"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
