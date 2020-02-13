package com.example.chatapp.notifications;



import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
            //git 올릴 때 이거 빼고 올리기
            "Content-Type:application/json",
            "Authorization:key=AAAAdckyknk:APA91bHDuKLdpVnL-jxhRh1NcY7TrWj5bCp3pKpUSE7ywLE7T1QcEn2cqhAFmFmgab3o3KI6vdBuDWx7F_jZaRhS9ejJxeFhckCitaMaQiVATG3K2fXE_CQECPC5RwjTDuBSm3VEEeZq"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
