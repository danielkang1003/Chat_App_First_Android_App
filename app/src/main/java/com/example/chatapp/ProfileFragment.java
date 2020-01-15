package com.example.chatapp;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    //firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    //storage
    StorageReference storageReference;

    //유저 프로필 사진 및 커버 사진이 저장될 경로
    String storagePath = "Users_Profile_Cover_Imgs/";

    //views
    ImageView iv_avartar, iv_cover;
    TextView tv_name, tv_email, tv_phone;
    FloatingActionButton fab;

    //progress dialog
    ProgressDialog pd;

    //permissions constants for camera and gallery
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //요청받아야할 퍼미션들을 배열로
    String cameraPermissions[];
    String storagePermissions[];

    //uri of picked image
    Uri image_uri;

    //프로필 사진이랑 커버 사진 체크하기 위한 변수
    String profileOrCoverPhoto;


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
        storageReference = getInstance().getReference();    //파이어베이스 스토리지 레퍼런스

        pd = new ProgressDialog(getActivity());

        //arrays of permissions
        cameraPermissions = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init views
        iv_avartar = view.findViewById(R.id.iv_avatar);
        iv_cover = view.findViewById(R.id.iv_coverPhoto);
        tv_name = view.findViewById(R.id.tv_name);
        tv_email = view.findViewById(R.id.tv_email);
        tv_phone = view.findViewById(R.id.tv_phone);
        fab = view.findViewById(R.id.fab);


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
                    String cover = "" + ds.child("cover").getValue();


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
                        Picasso.get().load(R.drawable.ic_default_img_white).into(iv_avartar);
                    }

                    try{
                        //if image is received, then set the image into avartar section
                        Picasso.get().load(cover).into(iv_cover);
                    }
                    catch (Exception e){
                        //if there is any exception while getting image, then set default
                        //Picasso.get().load(R.drawable.ic_default_img_white).into(iv_cover);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //when fab button click
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        return view;
    }

    private boolean checkStoragePermission(){
        //check if storage permission is enabled or not
        //return true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        //request runtime storage permission
        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        //check if storage permission is enabled or not
        //return true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        //request runtime storage permission
        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }


    private void showEditProfileDialog() {
        //옵션을 포함한 다이알로그 보여주기
        //1. 프로필 사진 수정
        //2. 커버 사진 수정
        //3. 이름 수정
        //4. 폰 번호 수정

        //options to show in dialog
        String options[] = {"프로필 사진 수정", "커버 사진 수정", "이름 수정", "번호 수정"};

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("수정 사항을 골라주세요");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item clicks
                if(which == 0){
                    //0이면 options 배열의 첫 인덱스인 프로필 사진 수정임
                    pd.setMessage("프로필 사진을 변경 중입니다");
                    profileOrCoverPhoto = "image";  //프로필 사진 변경하고 변수에 저장
                    showImagePicDialog();
                }
                else if(which == 1){
                    //1은 커버 사진
                    pd.setMessage("커버 사진을 변경 중입니다");
                    profileOrCoverPhoto = "cover";  //커버 사진 변경하고 변수에 저장
                    showImagePicDialog();
                }
                else if(which == 2){
                    //이름 수정
                    pd.setMessage("이름을 변경 하고 있습니다");
                    //메소드를 불러서 사용자 디비의 이름 수정. 파라미터(인자)로 키 name 넘겨주기
                    showNamePhoneUpdateDialog("name");
                }
                else if (which == 3){
                    //번호 수정
                    pd.setMessage("번호를 수정하고 있습니다");
                    showNamePhoneUpdateDialog("phone");
                }
            }
        });
        //create and show dialog
        builder.create().show();
    }

    private void showNamePhoneUpdateDialog(final String key) {
        //받은 인자 key는 name 또는 phone이라는 값을 가지고 있는데 이 키 값을 통해서 사용자의 이름 또는 폰 번호 변경
        //custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(key + "업데이트 하기");  //key값에 따라 이름 업데이트 또는 폰 번호 업데이트로 나오게 커스텀
        //set layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        //add edit text
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter " + key);   //키 값에 따라 다르게 보이게 설정
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //add button
        builder.setPositiveButton("업데이트", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //input text from edit text
                String value = editText.getText().toString().trim();
                //validate if user has entered sth or not
                if(!TextUtils.isEmpty(value)){
                    //빈값이 아닐 때
                    pd.show();
                    HashMap<String, Object> result = new HashMap<>();
                    result.put(key, value);

                    databaseReference.child(user.getUid()).updateChildren(result).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //업데이트 성공. progress dismiss
                            pd.dismiss();
                            Toast.makeText(getActivity(), "업데이트가 완료되었습니다", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //업데이트 실패 에러 메세지 보여주기
                            pd.dismiss();
                            Toast.makeText(getActivity(), "업데이트에 실패했습니다. " +e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else { //빈 값을 입력하려 한다면
                    Toast.makeText(getActivity(),key + "를 입력해주세요", Toast.LENGTH_SHORT).show();

                }
            }
        });

        //cancel button
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //create and show dialog
        builder.create().show();
    }

    private void showImagePicDialog() {
        //카메라와 갤러리 권한으로 사용자가 이미지를 고를 수 있도록 다이알로그 띄워주기

        //options to show in dialog
        String options[] = {"카메라", "갤러리"};

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("이미지를 선택해주세요");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog item clicks
                if(which == 0){
                    //카메라 선택 시
                    if(!checkCameraPermission()){   //권한이 허용되지 않았다면 사용자에게 접근권한 띄워주기
                        requestCameraPermission();
                    }
                    else{   //권한 허용되있으면 pickFromCamera로 넘어가기
                        pickFromCamera();
                    }
                }
                else if(which == 1){
                    //갤러리 선택 시
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

        //이미지를 카메라와 갤러리에서 불러오려면 권한 필요
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //사용자가 다이알로그에서 앱 권한 허용 또는 거절을 눌렀을 때
        //여기서 핸들링 함.
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                //카메라를 골랐을 때, 권한 허용되었는지 확인
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && writeStorageAccepted){
                        //권한 모두 허용 되있음
                        pickFromCamera();
                    }
                    else{
                        //퍼미션 거부
                        Toast.makeText(getActivity(), "카메라와 storage 접근 권한을 허용하셔야 사용 가능합니다", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //갤러리를 골랐을 때, 권한 허용되었는지 확인
                if(grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(writeStorageAccepted){
                        //권한 모두 허용 되있음
                        pickFromGallery();
                    }
                    else{
                        //퍼미션 거부
                        Toast.makeText(getActivity(), "storage 접근 권한을 허용하셔야 사용 가능합니다", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //이 곳은 pickFromGallery 또는 pickFromCamera 메소드가 불리고 난 이후에 실행 됨
        if(resultCode == RESULT_OK){

            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                //이미지가 갤러리로부터 불려왔을때 이미지 uri 가져오기
                image_uri = data.getData();

                uploadProfileCoverPhoto(image_uri);
            }
            if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //카메라에서 불려오면 이미지 uri 가져오기

                uploadProfileCoverPhoto(image_uri);
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(Uri uri) {
        //프로그레스 바
        pd.show();

        //프로필 사진이랑 커버 사진 만드는 함수 통합
        //체크를 하기 위한 스트링 변수 선언하여서 image 에는 사용자가 프로필 수정을 누르면 적용
        //cover는 사용자가 커버 사진 수정을 눌렀을 때 저장됨
        // 'image'는 각 사용자의 프로필 사진을 포함한 url
        // 'cover'은 각 사용자의 커버 사진을 포함한 url

        //image_uri 는 카메라 또는 갤러리로부터 불러온 이미지의 uri를 포함
        //UID를 활용해서 현재 로그인된 사용자에게 각각 하나의 프로필과 커버 사진 이미지를 부여

        //경로와 이미지 이름은 파이어베이스 스토리지에 저장됨
        String filePathAndName = storagePath + "" +profileOrCoverPhoto +"_" + user.getUid();
        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //이미지가 스토리지에 성공적으로 업로드 되면 uri 와 유저 db에서 가져옴
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while(!uriTask.isSuccessful()){};
                Uri downloadUri = uriTask.getResult();

                //이미지가 올라갔는지 확인.
                if(uriTask.isSuccessful()){
                    //이미지 업로드 성공
                    //사용자 db의 uri 추가 및 업데이트
                    HashMap<String, Object> results = new HashMap<>();
                    //첫번째 파라미터는 위에서 profileOrCoverPhoto 둘중 image 또는 cover의 값이 들어가 있는데 이를 통해서
                    //프로필 사진 또는 커버사진에 이미지가 저장 된다
                    //두번째 파라미터는 파이어베이스 저장소에 저장된 이미지 uri인데 profileOrCoverPhoto의 값에 따라 저장됨
                    results.put(profileOrCoverPhoto, downloadUri.toString());

                    databaseReference.child(user.getUid()).updateChildren(results).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //유저 db의 uri가 성공적으로 추가 됫을 떄
                            pd.dismiss();
                            Toast.makeText(getActivity(), "이미지가 성공적으로 업데이트 되었습니다", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //추가 하는 과정에서 에러 발생
                            pd.dismiss();
                            Toast.makeText(getActivity(), "이미지 업로드 과정에서 에러가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else{
                    //업로드 이미지 실패. 에러 메시지 보여주기
                    pd.dismiss();
                    Toast.makeText(getActivity(), "이미지 업로드에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
//                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //에러 발생. 에러 메세지 보여주고 progress dialog 종료
                pd.dismiss();
                Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void pickFromGallery() {
        //pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        //Intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        //Intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }


    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //user is signed in, stay here
            //set email of logged in user
            //tv_profile.setText(user.getEmail());
        }
        else{
            //user not signed in, go to main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);    //메뉴 옵션을 프레그먼트에 보여주기 위함
        super.onCreate(savedInstanceState);
    }
    //inflate options menu

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    //handle menu item clicks
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
