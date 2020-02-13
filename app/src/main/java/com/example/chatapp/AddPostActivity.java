package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class AddPostActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;
    ActionBar actionBar;

    //views
    EditText Et_title, Et_description;
    ImageView Iv_image;
    Button Btn_upload;

    //고른 이미지가 저장되는 uri
    Uri image_uri = null;

    //프로그레스 바 다이알로그
    ProgressDialog pd;

    //카메라 퍼미션
    private static final int CAMERA_REQUEST_CODE = 100;
    //갤러리 퍼미션
    private static final int STORAGE_REQUEST_CODE = 200;

    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    //퍼미션 배열
    String[] cameraPermissions;
    String[] storagePermissions;

    //사용자 정보
    String name, email, uid, dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        actionBar = getSupportActionBar();
        actionBar.setTitle("업로드");
        //액션바의 뒤로가기 버튼 활성화
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //퍼미션 배열 초기화
        cameraPermissions = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        pd = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        actionBar.setSubtitle(email);

        //현재 사용자의 정보 가져오기(포스팅한 곳에 사용하기 위함)
        userDbRef = FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    name = ds.child("name").getValue().toString();
                    email = ds.child("email").getValue().toString();
                    dp = ds.child("image").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //init views
        Et_title = findViewById(R.id.EtpTitle);
        Et_description = findViewById(R.id.EtpDescription);
        Iv_image = findViewById(R.id.IvpImage);
        Btn_upload = findViewById(R.id.BtnpUpload);

        //이미지뷰 누르면 카메라 또는 갤러리에서 사진 가져오기
        Iv_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //어디서 가지고 올지 다이알로그 띄워주기
                showImagePickDialog();
            }
        });

        //사진 업로드 버튼 리스너
        Btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = Et_title.getText().toString().trim();
                String description = Et_description.getText().toString().trim();
                if(TextUtils.isEmpty(title)){
                    Toast.makeText(AddPostActivity.this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(description)){
                    Toast.makeText(AddPostActivity.this, "설명을 추가해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(image_uri == null){
                    //사진 없이 포스트 등록
                    uploadData(title, description, "이미지가 없습니다");
                }
                else{
                    //사진과 함께 포스팅
                    uploadData(title, description, String.valueOf(image_uri));
                }
            }
        });
    }

    private void uploadData(String title, String description, String uri) {
        pd.setMessage("포스트 올리는 중...");
        pd.show();

        //포스트 이미지 이름, id, 업로드 시간
        String timeStamp = String.valueOf(System.currentTimeMillis());

        String filePathAndName = "Posts/" + "post_" + timeStamp;

        if(!uri.equals("이미지가 없습니다")){
            //이미지가 포함되있는 포스트를 올리려고할 때 (not 임)
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putFile(Uri.parse(uri)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //이미지가 파이어베이스 스토리지에 업로드될 때

                    //이제 이미지 uri를 가져와야함
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uriTask.isSuccessful());

                    String downloadUri = uriTask.getResult().toString();

                    if(uriTask.isSuccessful()){
                        //uri가 파이어베이스 db로 성공적으로 올라감
                        HashMap<Object, String> hashMap = new HashMap<>();
                        //이미지가 올라갔으니 포스팅 정보도 넣기
                        hashMap.put("uid", uid);
                        hashMap.put("uName", name);
                        hashMap.put("uEmail", email);
                        hashMap.put("uDp", dp);
                        hashMap.put("pId", timeStamp);
                        hashMap.put("pTitle", title);
                        hashMap.put("pDescription", description);
                        hashMap.put("pImage", downloadUri);
                        hashMap.put("pTime", timeStamp);

                        //포스트 정보가 들어갈 곳 설정 (Post로 설정)
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                        //ref 에 데이터 넣기
                        ref.child(timeStamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //정보가 디비에 잘 담김
                                pd.dismiss();
                                Toast.makeText(AddPostActivity.this, "포스팅 완료!", Toast.LENGTH_SHORT).show();
                                //views 리셋. 새로 넣으려면 빈 값이여야함.
                                Et_title.setText("");
                                Et_description.setText("");
                                Iv_image.setImageURI(null);
                                image_uri = null;
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //디비에 정보 넣기 실패
                                pd.dismiss();
                                Toast.makeText(AddPostActivity.this, "포스팅 실패..에러: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //이미지 업로드 실패
                    pd.dismiss();
                    Toast.makeText(AddPostActivity.this, "이미지 업로드 실패: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        else {
            //이미지 없는 포스트 올릴 시

            HashMap<Object, String> hashMap = new HashMap<>();
            //이미지가 올라갔으니 포스팅 정보도 넣기
            hashMap.put("uid", uid);
            hashMap.put("uName", name);
            hashMap.put("uEmail", email);
            hashMap.put("uDp", dp);
            hashMap.put("pId", timeStamp);
            hashMap.put("pTitle", title);
            hashMap.put("pDescription", description);
            hashMap.put("pImage", "이미지 없음");
            hashMap.put("pTime", timeStamp);

            //포스트 정보가 들어갈 곳 설정 (Post로 설정)
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
            //ref 에 데이터 넣기
            ref.child(timeStamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    //정보가 디비에 잘 담김
                    pd.dismiss();
                    Toast.makeText(AddPostActivity.this, "포스팅 완료!", Toast.LENGTH_SHORT).show();
                    //views 리셋. 새로 넣으려면 빈 값이여야함.
                    Et_title.setText("");
                    Et_description.setText("");
                    Iv_image.setImageURI(null);
                    image_uri = null;
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //디비에 정보 넣기 실패
                    pd.dismiss();
                    Toast.makeText(AddPostActivity.this, "포스팅 실패..에러: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showImagePickDialog() {
        //카메라 아니면 갤러리에서 가지고 오는 옵션
        String[] options = {"Camera", "Gallery"};

        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("이미지를 가져올 곳을 선택해주세요");
        //set options to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //item click handle
                if(which == 0){
                    //카메라가 선택됬을 때
                    //퍼미션 체크 해야함
                    if(!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else{
                        pickFromCamera();
                    }
                }
                if(which == 1){
                    //갤러리 선택
                    if(!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else{
                        pickFromGallery();
                    }
                }
            }
        });
        //create and show dialog
        builder.create().show();
    }


    private void pickFromGallery(){
        //갤러리로부터 이미지 고르는 인텐트
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        //intent 사용 카메라에서 이미지 고르기 위함
        ContentValues cv = new ContentValues(); //ContentResolver가 처리할 수 있는 값 집합을 저장. 컨텐트 리졸버가 사용하는 데이터 운송 수단
        cv.put(MediaStore.Images.Media.TITLE, "임시 제목");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "임시 설명");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);


        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    //갤러리 퍼미션 확인
    private boolean checkStoragePermission(){
        //check if storage permission is enabled or not
        //return true when enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    //카메라 퍼미션 확인
    private boolean checkCameraPermission(){
        //check if storage permission is enabled or not
        //return true when enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result & result1;
    }

    private void requestCameraPermission(){
        //request runtime camera permission
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //user is signed in, stay here
            email = user.getEmail();
            uid = user.getUid();

        }
        else{
            //user not signed in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();    //이전 액티비티로 이동
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_add_post).setVisible(false);
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


    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)  {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //이 메서드는 유저가 수락 또는 거절 버튼을 permission request 다이알로그에서 눌렀을 때 발생
        //여기서 핸들링 해야함
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length > 0){
                    //permission accepted
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && storageAccepted){
                        //두개 모두 접근 허용되었을 때
                        pickFromCamera();
                    }
                    else{
                        Toast.makeText(this, "카메라와 갤러리 접근 권한을 풀어주어야 합니다.", Toast.LENGTH_SHORT).show();
                    }
                }
                else{

                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if(grantResults.length> 0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        //storage permission granted
                        pickFromGallery();;
                    }
                    else{
                        Toast.makeText(this, "갤러리 접근권한을 풀어주세요.", Toast.LENGTH_SHORT).show();
                    }

                }
                else{

                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //이 메소드는 카메라 또는 갤러리로부터 이미지가 골라지고나서 실행
        if(resultCode == RESULT_OK){
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                //갤러리로부터 골라졌으면 이미지의 uri 가져오기
                image_uri = data.getData();
                //이미지뷰에 set 해주기
                Iv_image.setImageURI(image_uri);
            }
            else if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //카메라로 부터 골라지면 이미지 uri 가져오기
                Iv_image.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
