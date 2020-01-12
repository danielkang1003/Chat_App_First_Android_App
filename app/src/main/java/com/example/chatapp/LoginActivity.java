package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    GoogleSignInClient mGoogleSignInClient;
    //views
    EditText et_email, et_password;
    TextView tv_not_have_account, tv_forgot_pw;
    //Button btn_login;
    RelativeLayout btn_login;
    //google btn login
    SignInButton googleLoginBtn;


    //Declare an instance of FirebaseAuth
    private FirebaseAuth mAuth;

    //Progress dialog
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Actionbar and its title
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Login");

        //enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        //before mAuth
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuth = FirebaseAuth.getInstance();

        //init
        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        tv_not_have_account = findViewById(R.id.tv_not_have_account);
        tv_forgot_pw = findViewById(R.id.tv_forgotPass);
        btn_login = findViewById(R.id.loginBtn);
        googleLoginBtn = findViewById(R.id.googleLoginBtn);

        //login button click
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //input data
                String email = et_email.getText().toString();
                String pass = et_password.getText().toString().trim();  //trim: remove whitespace
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    //if invalid email address pattern
                    et_email.setError("이메일 형식으로 입력해주세요");
                    et_email.setFocusable(true);
                }
                else{
                    //if valid email pattern
                    loginUser(email, pass);
                }
            }
        });
        //not have account text view click
        tv_not_have_account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        });

        //Forgot password textview is clicked
        tv_forgot_pw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });

        //handle google login btn click
        googleLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //begin google login process
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        //init progress dialog
        progressDialog = new ProgressDialog(this);

    }

    private void showRecoverPasswordDialog() {
        //AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("비밀번호 초기화").setMessage("이메일을 입력해주세요");
        //set layout linear layout
        LinearLayout linearLayout = new LinearLayout(this);

        //views to set in dialog
        final EditText emailEt = new EditText(this);
        emailEt.setHint(R.string.email);
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        //set the min width of a edit text to fit a text of an 'M' letters regardless of the actual text extension and text size
        emailEt.setMinEms(16);

        linearLayout.addView(emailEt);
        linearLayout.setPadding(10,10,10,10);

        builder.setView(linearLayout);

        //buttons recover
        builder.setPositiveButton("보내기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //input email
                String email = emailEt.getText().toString().trim();
                beginRecovery(email);
            }
        });

        //buttons cancel
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dismiss dialog
                dialog.dismiss();
            }
        });

        //show dialog
        builder.create().show();
    }

    private void beginRecovery(String email) {
        //show progress dialog
        progressDialog.setMessage("Sending email...");
        progressDialog.show();
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    Toast.makeText(LoginActivity.this, "이메일을 확인해주세요", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(LoginActivity.this, "이메일 보내기에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                //get and show proper error message
                Toast.makeText(LoginActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loginUser(String email, String pass) {
        //show progress dialog
        progressDialog.setMessage("Logging In...");
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //dismiss progress Dialog
                            progressDialog.dismiss();
                            // Sign in success, update UI with the signed-in user's information
                            //Log.d("Daniel", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            //user is logged in, start loginActivity
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            //dismiss progress Dialog
                            progressDialog.dismiss();
                            // If sign in fails, display a message to the user.
                            //Log.w("Daniel", "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //dismiss progress Dialog
                progressDialog.dismiss();
                //error, get and show error message
//                Toast.makeText(LoginActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                Toast.makeText(LoginActivity.this, "아이디 또는 비밀번호가 틀립니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();    //go previous activity
        return super.onSupportNavigateUp();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("Daniel", "Google sign in failed", e);
                Toast.makeText(this, ""+ e.getMessage(), Toast.LENGTH_SHORT).show();
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        //Log.d("Daniel", "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            //Log.d("Daniel", "signInWithCredential:success");

                            FirebaseUser user = mAuth.getCurrentUser();

                            //if user is signing in first time then get and show user information from google account
                            if(task.getResult().getAdditionalUserInfo().isNewUser()){
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
                            }

                            //show user email int toast
                            Toast.makeText(LoginActivity.this, user.getDisplayName() + " 님, 구글 로그인 성공", Toast.LENGTH_SHORT).show();
                            //go to profile activity after logged in
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            //Log.w("Daniel", "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "구글 로그인 실패", Toast.LENGTH_SHORT).show();
                            //Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            //updateUI(null);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //get and show error message
                Toast.makeText(LoginActivity.this, ""+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
