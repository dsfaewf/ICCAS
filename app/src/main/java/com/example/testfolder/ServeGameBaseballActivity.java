package com.example.testfolder;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class ServeGameBaseballActivity extends AppCompatActivity {

    private EditText requestText;
    private TextView responseText;
    private TextView resultText;
    private TextView lifeCountText;
    private TextView coinText;
    private FirebaseUser currentUser;
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

        currentUser = SingletonJava.getInstance().getCurrentUser();

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
        SingletonJava.getInstance().checkAndResetDailyClears(currentUser, coinText, this);
        // 사용자 코인 불러오기
        SingletonJava.getInstance().loadUserCoins(coinText);
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
                    SingletonJava.getInstance().checkAndRewardCoins(currentUser, maxClearsPerDay, coinReward, coinText, ServeGameBaseballActivity.this);
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
}
