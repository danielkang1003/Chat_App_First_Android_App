package com.example.chatapp;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    //firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    //views
    ImageView iv_avartar;
    TextView tv_name, tv_email, tv_phone;
    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users"); //firebase database Name of tree

        //init views
        iv_avartar = view.findViewById(R.id.iv_avatar);
        tv_name = view.findViewById(R.id.tv_name);
        tv_email = view.findViewById(R.id.tv_email);
        tv_phone = view.findViewById(R.id.tv_phone);

        //여기부터는 현재 로그인 된 사용자의 정보를 가져와야함. 유저의 이메일 또는 uid를 통해서 가져오면 됨
        //이메일을 통해 유저의 상세 정보를 찾음
        //orderByChild query 사용해서 상세 정보를 노드로 부터 가져옴(이메일의 키가 현재 로그인된 이메일이랑 같다면)
        //모든 노드를 탐색해서 맞는 저옵를 가져옴
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //checking until required data is found
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    //get data
                    String name = "" + ds.child("name").getValue();
                    String email = "" +ds.child("email").getValue();
                    String phone = "" + ds.child("phone").getValue();
                    String image = "" + ds.child("image").getValue();

                    //set Data
                    tv_name.setText(name);
                    tv_email.setText(email);
                    tv_phone.setText(phone);
                    try{
                        //if image is received, then set the image into avartar section
                        Picasso.get().load(image).into(iv_avartar);
                    }
                    catch (Exception e){
                        //if there is any exception while getting image, then set default
                        Picasso.get().load(R.drawable.ic_add_image).into(iv_avartar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return view;
    }

}
