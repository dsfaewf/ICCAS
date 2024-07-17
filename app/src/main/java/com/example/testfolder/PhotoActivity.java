package com.example.testfolder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;

public class PhotoActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGES = 9; // 최대 8개의 이미지 선택 가능

    private Button btnSelectImage;
    private GridLayout imagesContainer;
    private ArrayList<Uri> imageUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo);

        btnSelectImage = findViewById(R.id.btnSelectImage);
        imagesContainer = findViewById(R.id.imagesContainer);
        imageUris = new ArrayList<>();

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    // Multiple images selected
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        if (imageUris.size() < MAX_IMAGES) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            imageUris.add(imageUri);
                            addImageToContainer(imageUri);
                        } else {
                            Toast.makeText(this, "You can select up to 8 images only.", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                } else if (data.getData() != null) {
                    // Single image selected
                    if (imageUris.size() < MAX_IMAGES) {
                        Uri imageUri = data.getData();
                        imageUris.add(imageUri);
                        addImageToContainer(imageUri);
                    } else {
                        Toast.makeText(this, "You can select up to 8 images only.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void addImageToContainer(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);

            // Adjust image size and margins to fit the screen without cutting off
            int totalMargin = 16; // 총 여백 크기 (양쪽 8dp)
            int imageSize = (getResources().getDisplayMetrics().widthPixels - totalMargin * 4) / 3; // 여백을 뺀 이미지 크기, 한 줄에 3개의 칼럼

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = imageSize;
            params.height = imageSize;
            params.setMargins(8, 8, 8, 8); // 여백을 양쪽에 8dp 주어 이미지가 짤리지 않도록 조정

            imageView.setLayoutParams(params);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Change to desired scale type

            imagesContainer.addView(imageView);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load the image.", Toast.LENGTH_SHORT).show();
        }
    }



    private void navigateToDiary_write_UI() {
        Intent intent = new Intent(this, Diary_write_UI.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // 현재 액티비티 종료
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToDiary_write_UI();
    }

}
