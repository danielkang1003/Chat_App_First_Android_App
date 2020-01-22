package com.example.chatapp.adapters;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.models.ModelChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder>{

    private static  final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    Context context;
    List<ModelChat> chatList;
    String imageUrl;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    FirebaseUser fUser;

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate layouts: row_chat_left.xml for receiver, row_chat_right.xml for sender
        if(viewType == MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false);
            return new MyHolder(view);
        }
        else{
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false);
            return new MyHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        //get data
        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimeStamp();

        //convert time stamp to dd/mm/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.KOREA);
        calendar.setTimeInMillis(Long.parseLong(timeStamp));
        String dateTime = DateFormat.format("MM/dd/yyyy hh:mm aa", calendar).toString();

        //set data
        holder.tv_message.setText(message);
        holder.tv_time.setText(dateTime);
        try{
            Picasso.get().load(imageUrl).into(holder.iv_profile);
        }catch(Exception e){

        }

        //메세지 삭제 다이알로그 띄워주기
        holder.messageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show delete message confirm dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("메세지 삭제");
                builder.setMessage("정말 삭제하시겠습니까?");
                // 삭제 버튼
                builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMessage(position);    //위에 온 바인드 뷰홀더의 포지션 파라미터 넣어주기
                    }
                });
                //취소 버튼
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss dialog
                        dialog.dismiss();
                    }
                });
                //다이알로그 생성 후 보여주기
                builder.create().show();
            }
        });

        //set seen/delivered status of message (1이 사라지냐 안사라지냐 이거 확인)
        if(position == chatList.size()- 1){
            if(chatList.get(position).getChecked()){
                holder.tv_checked.setText("");  //확인하면 없어지는 걸로 해결? 아니면 '확인'으로
            }
            else{
                holder.tv_checked.setText("1"); //확인하지 않았다면 1
            }
        }
        else{
            holder.tv_checked.setVisibility(View.GONE);
        }
    }

    private void deleteMessage(int position) {
        final String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();    //자기것만 삭제 가능하도록 하기 위함


        //메시지 삭제
        //1. 선택된 position의 메세지 timestamp를 가지고 옴
        //2. 1번의 메세지와 채팅방에 있는 모든 메세지를 비교함
        //3. 채팅방에 있는 모든 대화를 돌면서 position의 timestamp(즉 사용자가 삭제하기 위해 선택한 메세지)와 값이 같으면 삭제
        String msgTimeStamp = chatList.get(position).getTimeStamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timeStamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    //이 부분은 myUID와 메세지에서 보낸 uid를 비교해서 삭제 가능하도록 구현
                    if(ds.child("sender").getValue().equals(myUID)){
                        //sender의 uid를 비교해야 삭제 가능하도록 여기서 id확인이 되면 삭제가 가능하도록 실행되어야함.
                        //두가지 기능 실행 가능함.
                        //1. 채팅방에서 대화 삭제

                        //ds.getRef().removeValue();  //여기만 넣으면 선택한 대화를 아예 채팅방에서 삭제해버림

                        //2. 삭제된 메세지의 자리에 메세지가 삭제됬음을 알려주는 문구 삽입 (업데이트 방식)
                        //여기서는 삭제된것을 알려주는 문구를 삽입함. 카카오톡 처럼
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "메세지가 삭제되었습니다.");
                        ds.getRef().updateChildren(hashMap);
                        Toast.makeText(context, "메세지 삭제 완료", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(context, "나의 메세지만 삭제 가능합니다", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        //현재 사용자 유저
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if(chatList.get(position).getSender().equals(fUser.getUid())){
            return MSG_TYPE_RIGHT;
        }
        else{
            return MSG_TYPE_LEFT;
        }
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //views
        ImageView iv_profile;
        TextView tv_message, tv_time, tv_checked;
        LinearLayout messageLayout; //메세지 삭제를 보여주기 위한 클릭 리스너에 사용

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            iv_profile = itemView.findViewById(R.id.Iv_profile);
            tv_message = itemView.findViewById(R.id.Tv_message);
            tv_time = itemView.findViewById(R.id.Tv_time);
            tv_checked = itemView.findViewById(R.id.Tv_checked);
            messageLayout = itemView.findViewById(R.id.messageLayout);
        }
    }
}
