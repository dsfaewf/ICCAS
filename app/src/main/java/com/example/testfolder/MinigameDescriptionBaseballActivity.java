package com.example.testfolder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MinigameDescriptionBaseballActivity extends AppCompatActivity {

    private VideoView descriptionVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_minigame_description_baseball);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 뒤로가기 버튼 클릭 리스너 설정
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MinigameDescriptionBaseballActivity.this, GamelistActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 게임 시작하기 버튼 클릭 리스너 설정
        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MinigameDescriptionBaseballActivity.this, ServeGameBaseballActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // VideoView 초기화 및 동영상 로드
        descriptionVideo = findViewById(R.id.description);
        loadDescriptionVideo();
    }

    private void loadDescriptionVideo() {
        try {
            // 동영상 파일 경로 설정
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.baseball_game;
            Uri uri = Uri.parse(videoPath);
            descriptionVideo.setVideoURI(uri);

            // 동영상 재생 준비 및 시작
            descriptionVideo.setOnPreparedListener(mp -> {
                // 동영상 재생 준비가 완료되면 시작
                mp.setLooping(true); // 반복 재생 설정
                descriptionVideo.start();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티 종료 시 동영상 재생 중지
        if (descriptionVideo != null) {
            descriptionVideo.stopPlayback();
        }
    }
}
