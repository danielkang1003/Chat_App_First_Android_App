package com.example.chatapp.notifications;

public class Token {
    /*
    파이어베이스클라우드메세징(FCM) 토큰 또는 registrationToken으로 알려짐.
    GCM으로부터 받은 ID로 서버와 연결하고 클라이언트가 메세지 받는 것을 허용함
     */

    String token;

    public Token(String token) {
        this.token = token;
    }

    public Token() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
