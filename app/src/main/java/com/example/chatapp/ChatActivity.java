package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.service.autofill.UserData;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.adapters.AdapterChat;
import com.example.chatapp.models.ModelChat;
import com.example.chatapp.models.ModelUsers;
import com.example.chatapp.notifications.APIService;
import com.example.chatapp.notifications.Client;
import com.example.chatapp.notifications.Data;
import com.example.chatapp.notifications.Response;
import com.example.chatapp.notifications.Sender;
import com.example.chatapp.notifications.Token;
import com.github.abdularis.civ.AvatarImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity {

    //Views from activity_chat.xml
    Toolbar toolbar;
    RecyclerView rv_chat;
    AvatarImageView iv_profile; //이부분 확실하지 않음. 에러나면 이부분 ImageView로 바꿔서 해보기
    TextView tv_chat_name , tv_user_status;
    EditText et_message;
    ImageButton btn_send;

    //firebase auth 로그아웃 옵션을 위해
    FirebaseAuth firebaseAuth;

    //db 접근해서 사용자 정보가져올때 사용
    FirebaseDatabase firebaseDatabase;
    DatabaseReference userDbRef;

    //채팅할 때 사용
    String hisUid;
    String myUid;
    String hisImage;

    APIService apiService;
    boolean notify = false;


    //메세지를 확인했는지 안했는지 확인하기위해
    ValueEventListener checkedListener;
    DatabaseReference userRefForChecked;

    List<ModelChat> chatList;
    AdapterChat adapterChat;

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


        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        //리사이클러 뷰 레이아웃(리니어 레이아웃)
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //recyclerView properties
        rv_chat.setHasFixedSize(true);
        rv_chat.setLayoutManager(linearLayoutManager);

        //api service 생성
        apiService = Client.getRetrofit("https://fcm.googleapis.com").create(APIService.class);


        //adapterUsers에서 사용자가 상대방을 누르면 인텐트로 hisUid를 넘겨주게 되어있는데
        //이것을 사용해서 프로필 사진과 이름 그리고 채팅이 가능하게 만듬
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        firebaseDatabase = FirebaseDatabase.getInstance();
        userDbRef = firebaseDatabase.getReference("Users");

        //search user to get that user's info
        Query userQuery = userDbRef.orderByChild("uid").equalTo(hisUid);

        //get user picture & name
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //check until required info is received
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    //get data
                    String name = ds.child("name").getValue().toString();
                    hisImage = ""+ ds.child("image").getValue();

                    //get value of onlineStatus
                    String onlineStatus = ds.child("onlineStatus").getValue().toString();

                    //get value of typing status
                    String typingStatus = ds.child("typing").getValue().toString();
                    //check typing status
                    if(typingStatus.equals(myUid)){
                        tv_user_status.setText("작성중...");
                    }
                    else {
                        if(onlineStatus.equals("online")){
                            tv_user_status.setText(onlineStatus);
                        }
                        else{
                            //convert proper time and date
                            Calendar calendar = Calendar.getInstance(Locale.KOREA);
                            calendar.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("MM/dd/yyyy hh:mm aa", calendar).toString();
                            tv_user_status.setText("마지막 로그인: " + dateTime);
                        }
                    }

                    tv_chat_name.setText(name);


                    //image received check
                    try{
                        //if image received, set it to imageview in toolbar
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img_white).into(iv_profile);
                    }
                    catch (Exception e){
                        //failed to get picture from users info. set default picture
                        Picasso.get().load(R.drawable.ic_default_img_white).into(iv_profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //click btn to send msg
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                //get text from edit text
                String message = et_message.getText().toString().trim();

                //checking empty text
                if(TextUtils.isEmpty(message)){
                    //text is currently empty
                    Toast.makeText(ChatActivity.this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
                else{
                    //text is not empty send it
                    sendMessage(message);
                }

                //reset edit text after sending message
                et_message.setText(""); //문자를 보내고 비워줘야 하므로. 계속있으면 이상하잖아?
            }
        });

        //check edit text change listener
        //상대 유저가 메세지 타이핑 하는게 감지되면 진입
        et_message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length() == 0){
                    //아무것도 감지가 안된다면 0이므로
                    checkTypingStatus("noOne");
                }
                else{
                    checkTypingStatus(hisUid);  //받는 사람 uid
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        readMessage();

         checkMessage();
    }

    private void checkMessage() {
        userRefForChecked = FirebaseDatabase.getInstance().getReference("Chats");
        checkedListener = userRefForChecked.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if(chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)){
                        HashMap<String, Object> checkedHashMap = new HashMap<>();
                        checkedHashMap.put("checked", true);
                        ds.getRef().updateChildren(checkedHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessage() {
        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    //채팅방 상대를 양방향으로 확인하여야함 나 <-> 상대방 둘다 맞으면 시작
                    if(chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)||
                    chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)){
                        chatList.add(chat);
                    }
                    //어댑터   채팅 내용과 프로필 이미지 넘기기
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();

                    //어답터 리사이클러뷰로 세팅
                    rv_chat.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(final String message) {
        /*
            채팅방 이름은 Chats로 해놨음. 사용자와 상대방간의 대화를 가지고 있음
            만약 다른 유저가 다른유저한테 메세지를 보내면 또다른 Chats을 만들겠지?
            유저가 메세지를 보내면 Chat 노드 아래로 보내는 사용자 받는 사용자 메세지가 들어갈 것임
            sender: myUid
            receiver: hisUid
            message: sender가 친 메세지
            timestamp: 메세지를 보내거나 받은 시간을 저장
            checked: 읽었는지 안읽었는지 확인
        */

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timeStamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timeStamp", timeStamp);
        hashMap.put("checked", false);
        databaseReference.child("Chats").push().setValue(hashMap);


        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelUsers user = dataSnapshot.getValue(ModelUsers.class);

                if(notify){
                    sendNotification(hisUid, user.getName(), message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotification(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid, name+":"+message, "New Message", hisUid, R.drawable.applogo);
                    Sender sender = new Sender(data, token.getToken());
                    apiService.sendNotification(sender).enqueue(new Callback<Response>() {
                        @Override
                        public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                            Toast.makeText(ChatActivity.this, ""+ response.message(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<Response> call, Throwable t) {
                            Toast.makeText(ChatActivity.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //user is signed in, stay here
            //set email of logged in user
            //tv_profile.setText(user.getEmail());
            myUid = user.getUid();  //현자 사용자의 uid



        }
        else{
            //user not signed in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);

        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);

    }

    private void checkTypingStatus(String typing){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typing", typing);

        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //get timestamp and set offline with last seen time stamp
        //오프라인상태이면 마지막 접속 시간을 표시해주기 위해 timestamp 필요
        String timeStamp = String.valueOf(System.currentTimeMillis()); //string으로 시간 변환해주기
        checkOnlineStatus(timeStamp);  //status에 timeStamp 전달

        //메세지를 상대방이 치고 있으면 보여줌
        checkTypingStatus("noOne");
        userRefForChecked.removeEventListener(checkedListener);
    }

    @Override
    protected void onResume() {
        //set online
        checkOnlineStatus("online");
        super.onResume();
    }

    //메뉴바
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide searchView. 여기서는 필요없기에 없애주는 것
        menu.findItem(R.id.action_search).setVisible(false);


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if(id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }

        return super.onOptionsItemSelected(item);
    }
}
