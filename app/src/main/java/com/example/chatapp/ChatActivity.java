package com.example.chatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.abdularis.civ.AvatarImageView;

public class ChatActivity extends AppCompatActivity {

    //Views from activity_chat.xml
    Toolbar toolbar;
    RecyclerView rv_chat;
    AvatarImageView iv_profile; //이부분 확실하지 않음. 에러나면 이부분 ImageView로 바꿔서 해보기
    TextView tv_chat_name , tv_user_status;
    EditText et_message;
    ImageButton btn_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //init views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        rv_chat = findViewById(R.id.Rv_chat);
        iv_profile = findViewById(R.id.Iv_profile) ;
        tv_chat_name = findViewById(R.id.Tv_chat_name);
        tv_user_status = findViewById(R.id.Tv_user_status);
        et_message = findViewById(R.id.Et_message);
        btn_send = findViewById(R.id.Btn_send);
    }
}
