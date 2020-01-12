package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    EditText et_email, et_password;
    //Button registerBtn;
    RelativeLayout registerBtn;
    TextView tv_haveAccount;

    //progressbar to display while registering users
    ProgressDialog progressDialog;

    //Declare an instance of FirebaseAuth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Actionbar and its title
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create Account");

        //enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        registerBtn = findViewById(R.id.registerBtn);
        tv_haveAccount = findViewById(R.id.tv_have_account);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering User...");
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //input email, password
                String email = et_email.getText().toString();
                String password = et_password.getText().toString().trim();

                //validate
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    //when email is wrong. set error and focus to email editText
                    et_email.setError("이메일 형식이 올바르지 않습니다");
                    et_email.setFocusable(true);
                }
                else if(password.length() < 6){
                    //set error and focus to password edittext
                    et_password.setError("최소 6자리 이상의 비밀번호로 설정해주세요");
                    et_password.setFocusable(true);
                }
                else {
                    registerUser(email, password);  //register the user if all corrects
                }
            }
        });

        //handle login textview click listener
        tv_haveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent( RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

    }

    private void registerUser(String email, String password) {
        //email and password pattern is valid, show progress dialog and start registering user.
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, dismiss dialog and start register activity
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();

                            //get user email and uid from auth
                            String email = user.getEmail();
                            String uid = user.getUid();
                            //when user is registered, store user info in firebase realtime db too
                            //using Hash Map
                            HashMap<Object, String> hashMap = new HashMap<>();
                            //put info in hashmap
                            hashMap.put("email", email);
                            hashMap.put("uid", uid);
                            hashMap.put("name", "");    //will add later(e.g. edit profile)
                            hashMap.put("phone", "");   //same
                            hashMap.put("image", "");   //same
                            hashMap.put("cover", "");   //same
                            //Firebase db instance
                            FirebaseDatabase db = FirebaseDatabase.getInstance();
                            //path to store user data named "Users"
                            DatabaseReference reference = db.getReference("Users");
                            //put data within hashmap in database
                            reference.child(uid).setValue(hashMap);

                            Toast.makeText(RegisterActivity.this, "Registered...\n" + user.getEmail(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //error, dismiss progress dialog and get and show the error msg
                Toast.makeText(RegisterActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();    //go previous activity
        return super.onSupportNavigateUp();
    }
}
