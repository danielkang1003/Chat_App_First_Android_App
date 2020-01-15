package com.example.chatapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.ChatActivity;
import com.example.chatapp.R;
import com.example.chatapp.models.ModelUsers;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyHolder>{

    Context context;
    List<ModelUsers> userList;

    //constructors
    public AdapterUsers(Context context, List<ModelUsers> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout(row_user.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        //get data
        final String hisUID = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        final String userEmail = userList.get(position).getEmail();

        //set Data
        holder.tv_mName.setText(userName);
        holder.tv_mEmail.setText(userEmail);
        try{
            Picasso.get().load(userImage).
                    placeholder(R.drawable.ic_default_img).into(holder.circle_Iv_avatar);
        }
        catch(Exception e){

        }

        //handle item click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(context, "" + userEmail, Toast.LENGTH_SHORT).show();
                //사용자가 상대방을 누르면 채팅 시작
                //상대방의 UID를 받아와서 채팅을 누구와할지 정함
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("hisUid", hisUID);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        ImageView circle_Iv_avatar;
        TextView tv_mName, tv_mEmail;

        //컨스트럭터
        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            circle_Iv_avatar = itemView.findViewById(R.id.circle_iv_avatar);
            tv_mName = itemView.findViewById(R.id.tv_user_name);
            tv_mEmail = itemView.findViewById(R.id.tv_user_email);
        }
    }
}
