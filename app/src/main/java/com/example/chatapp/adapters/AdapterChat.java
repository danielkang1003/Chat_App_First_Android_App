package com.example.chatapp.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.models.ModelChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
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
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
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


        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            iv_profile = itemView.findViewById(R.id.Iv_profile);
            tv_message = itemView.findViewById(R.id.Tv_message);
            tv_time = itemView.findViewById(R.id.Tv_time);
            tv_checked = itemView.findViewById(R.id.Tv_checked);
        }
    }
}
