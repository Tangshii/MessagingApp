package com.hike.messagingapp.Notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAvqK_J5o:APA91bG2vxaE09JOgzg8cFJ_VFFYfR3APPxsAIcaafpG-CMwMGg0FV_k2_gn6nxGc1QhSd5oGM6YoOvmasQvHiQ1HWuSPkvTkg5Eu9D7pzSAWnYB0VhhQFQuPWhd-9Fcm-D9kUeyv8Ed"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
