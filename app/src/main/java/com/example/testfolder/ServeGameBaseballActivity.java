package com.example.testfolder;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ServeGameBaseballActivity extends AppCompatActivity {

    private EditText requestText;
    private TextView responseText;
    private TextView resultText;
    private TextView lifeCountText;
    private TextView coinText;
    FirebaseAuth auth;
    DatabaseReference database;
    FirebaseUser currentUser;
    private Integer[] comNumber = new Integer[3];
    private Integer[] userNumber = new Integer[3];

    private int lifeCount = 10;
    private int strike = 0;
    private int ball = 0;
    private int coinReward = 10; // 지급되는 보상은 10코인으로 설정
    private int maxClearsPerDay = 3; // 하루 최대 보상 횟수를 3으로 지정

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serve_game_baseball);
        coinText = findViewById(R.id.coin_text); // coinText 초기화 추가
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        currentUser = auth.getCurrentUser();

        try {
            requestText = findViewById(R.id.request_text);
            responseText = findViewById(R.id.response_text);
            lifeCountText = findViewById(R.id.life_count_text);
            resultText = findViewById(R.id.result_text);

            Button startBtn = findViewById(R.id.start_btn);
            Button answerBtn = findViewById(R.id.answer_btn);
            Button resetBtn = findViewById(R.id.reset_btn);

            viewMode("end");

            startBtn.setOnClickListener(view -> {
                randomNumber();
                toastMessage("게임 시작");
                viewMode("start");
            });

            answerBtn.setOnClickListener(view -> numberCheck());

            resetBtn.setOnClickListener(view -> {
                toastMessage("초기화");
                viewMode("end");
                reset();
            });

        } catch (Exception e) {
            Log.e("ServeGameBaseballActivity", "Error during onCreate", e);
        }
        // 사용자 코인 및 초기화 날짜 불러오기
        checkAndResetDailyClears();

        // 사용자 코인 불러오기
        loadUserCoins();
    }

    private void randomNumber() {
        try {
            Set<Integer> set = new HashSet<>();
            ArrayList<Integer> list = new ArrayList<>();
            Random random = new Random();

            while (set.size() < 3) {
                int randomValue = random.nextInt(9) + 1; // 1~9 범위로 변경
                set.add(randomValue);
            }

            list.addAll(set);
            java.util.Collections.shuffle(list);

            for (int i = 0; i < list.size(); i++) {
                comNumber[i] = list.get(i);
            }

            responseText.setText("컴퓨터 숫자: " + comNumber[0] + ", " + comNumber[1] + ", " + comNumber[2]);
        } catch (Exception e) {
            Log.e("ServeGameBaseballActivity", "Error during randomNumber", e);
        }
    }

    private void numberCheck() {
        try {
            lifeCount--;
            lifeCountText.setText("기회: " + lifeCount + " 번");
            String inputNumber = requestText.getText().toString();

            if (inputNumber.length() == 3) {
                userNumber[0] = Integer.parseInt(inputNumber.substring(0, 1));
                userNumber[1] = Integer.parseInt(inputNumber.substring(1, 2));
                userNumber[2] = Integer.parseInt(inputNumber.substring(2, 3));

                for (int num = 0; num < 3; num++) {
                    for (int num2 = 0; num2 < 3; num2++) {
                        if (comNumber[num].equals(userNumber[num2])) {
                            if (num == num2) {
                                strike++;
                            } else {
                                ball++;
                            }
                        }
                    }
                }

                if (strike == 3) {
                    toastMessage("성공");
                    responseText.setText("정답: " + comNumber[0] + ", " + comNumber[1] + ", " + comNumber[2]);
                    checkAndRewardCoins();
                } else if (lifeCount == 0) {
                    toastMessage("실패");
                    responseText.setText("정답: " + comNumber[0] + ", " + comNumber[1] + ", " + comNumber[2]);
                } else {
                    responseText.setText("Strike: " + strike + ", Ball: " + ball);
                    showResult(inputNumber);
                }

                requestText.setText("");
                strike = 0;
                ball = 0;
            } else {
                toastMessage("숫자 3개를 입력해주세요.");
            }
        } catch (Exception e) {
            Log.e("ServeGameBaseballActivity", "Error during numberCheck", e);
        }
    }

    private void showResult(String inputNumber) {
        try {
            String result = "Strike: " + strike + ", Ball: " + ball;
            resultText.append(inputNumber + " : " + result + "\n");
        } catch (Exception e) {
            Log.e("ServeGameBaseballActivity", "Error during showResult", e);
        }
    }

    private void reset() {
        try {
            lifeCount = 10;
            lifeCountText.setText("기회: " + lifeCount + " 번");
            responseText.setText("");
            resultText.setText("");
        } catch (Exception e) {
            Log.e("ServeGameBaseballActivity", "Error during reset", e);
        }
    }

    private void viewMode(String mode) {
        try {
            Button startBtn = findViewById(R.id.start_btn);
            Button answerBtn = findViewById(R.id.answer_btn);
            Button resetBtn = findViewById(R.id.reset_btn);

            if (mode.equals("start")) {
                startBtn.setEnabled(false);
                answerBtn.setEnabled(true);
                resetBtn.setEnabled(true);
                requestText.setEnabled(true);

                startBtn.setBackgroundColor(getColor(android.R.color.darker_gray));
                answerBtn.setBackgroundColor(getColor(android.R.color.holo_green_light));
                resetBtn.setBackgroundColor(getColor(android.R.color.holo_green_light));
            } else if (mode.equals("end")) {
                startBtn.setEnabled(true);
                answerBtn.setEnabled(false);
                resetBtn.setEnabled(true);
                requestText.setEnabled(false);

                startBtn.setBackgroundColor(getColor(android.R.color.holo_green_light));
                answerBtn.setBackgroundColor(getColor(android.R.color.holo_green_light));
                resetBtn.setBackgroundColor(getColor(android.R.color.holo_green_light));
            }
        } catch (Exception e) {
            Log.e("ServeGameBaseballActivity", "Error during viewMode", e);
        }
    }

    private void toastMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void checkAndResetDailyClears() {
        String userId = currentUser.getUid();
        DatabaseReference userRef = database.child("users").child(userId);

        userRef.child("lastResetDate").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastResetDate = snapshot.exists() ? snapshot.getValue(String.class) : "";
                String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

                if (!currentDate.equals(lastResetDate)) {
                    resetDailyClears(userRef, currentDate);
                } else {
                    loadUserCoins();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServeGameBaseballActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetDailyClears(DatabaseReference userRef, String currentDate) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("dailyClears", 0);
        updates.put("lastResetDate", currentDate);

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                loadUserCoins();
                Toast.makeText(ServeGameBaseballActivity.this, "dailyClears가 초기화되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ServeGameBaseballActivity.this, "초기화 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserCoins() {
        String userId = currentUser.getUid();
        DatabaseReference userRef = database.child("users").child(userId);

        userRef.child("coins").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long coins = snapshot.exists() ? snapshot.getValue(Long.class) : 0L;
                if (coins != null) {
                    coinText.setText(String.valueOf(coins));
                } else {
                    coinText.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServeGameBaseballActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRewardCoins() {
        String userId = currentUser.getUid();
        DatabaseReference userRef = database.child("users").child(userId);

        userRef.child("dailyClears").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long dailyClears = snapshot.exists() ? (long) snapshot.getValue() : 0;

                if (dailyClears < maxClearsPerDay) {
                    rewardCoins(userRef, dailyClears);
                } else {
                    Toast.makeText(ServeGameBaseballActivity.this, "오늘은 더 이상 보상을 받을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    viewMode("end");
                    reset();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServeGameBaseballActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rewardCoins(DatabaseReference userRef, long dailyClears) {
        userRef.child("coins").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long coins = snapshot.exists() ? snapshot.getValue(Long.class) : 0L;
                if (coins != null) {
                    coins += coinReward;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("coins", coins);
                    updates.put("dailyClears", dailyClears + 1);

                    Long finalCoins = coins;
                    userRef.updateChildren(updates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ServeGameBaseballActivity.this, "정답입니다. " + coinReward + " 코인이 지급되었습니다.", Toast.LENGTH_SHORT).show();
                            coinText.setText(String.valueOf(finalCoins)); // 코인 텍스트 업데이트
                        } else {
                            Toast.makeText(ServeGameBaseballActivity.this, "코인 지급 오류", Toast.LENGTH_SHORT).show();
                        }
                        viewMode("end");
                        reset();
                    });
                } else {
                    Toast.makeText(ServeGameBaseballActivity.this, "코인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServeGameBaseballActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
