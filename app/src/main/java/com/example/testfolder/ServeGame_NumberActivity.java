package com.example.testfolder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

public class ServeGame_NumberActivity extends AppCompatActivity implements View.OnClickListener {
    GridLayout gridLayout;
    TextView requestText, responseText, coinText; //coinText 추가
    Button startBtn, answerBtn;
    Button[] buttons = new Button[10];
    String answer = "";
    int randomNumber;

    int min = 1;
    int max = 100;

    int count = 0; //시작은 0번이어야 하므로 0으로 수정

    int coinReward = 10; // 지급되는 보상은 10코인으로 설정해놓음
    int maxClearsPerDay = 3; // 하루 최대 보상 횟수를 3으로 일단 지정해놓음

    FirebaseAuth auth;
    DatabaseReference database;
    FirebaseUser currentUser;


    HashSet<Character> enteredNumbers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serve_game_number);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        currentUser = auth.getCurrentUser();
        SingletonJava.getInstance().initialize(auth, database);
        coinText = findViewById(R.id.coin_text); // coinText 초기화 추가

        //버튼 레이아웃
        gridLayout = findViewById(R.id.grid_layout);

        //입력된 숫자가 보여지는 텍스트뷰
        requestText = findViewById(R.id.request_text);

        //정답이 맞는지 보여주는 텍스트뷰
        responseText = findViewById(R.id.response_text);

        //정답버튼
        answerBtn = findViewById(R.id.answer_btn);

        //시작버튼
        startBtn = findViewById(R.id.start_btn);

        //버튼 초기화
        for (int i = 0; i < buttons.length; i++) {
            String buttonID = "btn" + i;
            int resourceID = getResources().getIdentifier(buttonID, "id", getPackageName());
            buttons[i] = findViewById(resourceID);
            buttons[i].setOnClickListener(this);
        }

        //버튼 숨김
        viewMode("end");

        //시작버튼
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewMode("start");

                //랜덤숫자 생성
                Random random = new Random();
                randomNumber = random.nextInt(100) + 1; //1~100

                // 시작 버튼을 누를 때 정답 텍스트 초기화
                responseText.setText("");

                Toast.makeText(ServeGame_NumberActivity.this, "숫자가 생성되었습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        //정답 버튼 클릭 이벤트
        answerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count++; //도전횟수 카운트

                //도전횟수가 7번일 때
                if (count == 7) {
                    Toast.makeText(ServeGame_NumberActivity.this, "도전 횟수를 초과했습니다.", Toast.LENGTH_SHORT).show();

                    //버튼 숨김
                    viewMode("end");

                    //초기화
                    reset();
                    resetButtons(); //버튼 초기화
                } else {
                    //입력숫자
                    int inputNumber = Integer.parseInt(requestText.getText().toString());

                    //입력값이 랜덤수보다 크면
                    if (inputNumber > randomNumber) {
                        //입력값을 최대값에 넣는다.
                        max = inputNumber;

                        responseText.setText(count + " 번째 " + min + " ~ " + max);
                    }
                    //입력값이 랜덤수보다 작으면
                    else if (inputNumber < randomNumber) {
                        //입력값을 최초값에 넣는다.
                        min = inputNumber;

                        responseText.setText(count + " 번째 " + min + " ~ " + max);
                    }
                    //입력값이랑 랜덤수가 같다면 ( 정답 )
                    else if (inputNumber == randomNumber) {
                       // 싱글톤으로 수정
                        SingletonJava.getInstance().checkAndRewardCoins(currentUser, database, maxClearsPerDay,
                                coinReward, coinText, ServeGame_NumberActivity.this);
                    }

                    //입력창 초기화
                    requestText.setText("");

                    //정답 변수
                    answer = "";
                }
            }
        });
        // 사용자 코인 및 초기화 날짜 불러오기 싱글톤으로 수정
        // checkAndResetDailyClears();
        SingletonJava.getInstance().checkAndResetDailyClears(currentUser, database, coinText, this);
        // 사용자 코인 불러오기 싱글톤으로 수정!
        SingletonJava.getInstance().loadUserCoins(coinText);
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
                    SingletonJava.getInstance().loadUserCoins(coinText);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServeGame_NumberActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetDailyClears(DatabaseReference userRef, String currentDate) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("dailyClears", 0);
        updates.put("lastResetDate", currentDate);

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                SingletonJava.getInstance().loadUserCoins(coinText);
                Toast.makeText(ServeGame_NumberActivity.this, "반갑습니다! 오늘도 총 3번 서브게임 보상을 받을 수 있습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ServeGame_NumberActivity.this, "보상 시스템 기회 업데이트 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 버튼 클릭 이벤트
    @Override
    public void onClick(View view) {
        int id = view.getId();

        // 숫자 길이 체크
        if (checkLength(answer)) {
            Button clickedButton = (Button) view;
            String buttonText = clickedButton.getText().toString();

            // HashSet에 이미 해당 숫자가 있는지 확인하여 중복 방지
            if (!enteredNumbers.contains(buttonText.charAt(0))) {
                // HashSet에 해당 숫자 추가
                enteredNumbers.add(buttonText.charAt(0));

                // 정답에 숫자 추가 및 화면에 표시
                answer = answer + buttonText;
                display();
            } else {
                // 이미 입력된 숫자라면 알림 표시
                Toast.makeText(this, "같은 숫자는 중복해서 입력할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //버튼 초기화
    private void resetButtons() {
        answer = ""; //정답 변수 초기화
        requestText.setText(""); //입력창 초기화
        enteredNumbers.clear(); // HashSet 초기화
    }

    //게임타입
    public void viewMode(String type) {
        //게임 시작
        if (type.equals("start")) {
            gridLayout.setVisibility(View.VISIBLE); //번호 활성화
            startBtn.setEnabled(false); //시작버튼 비활성화
            answerBtn.setEnabled(true); //정답버튼 활성화
        }
        //게임 완료
        else {
            gridLayout.setVisibility(View.INVISIBLE); //번호 비활성화
            startBtn.setEnabled(true); //시작버튼 활성화
            answerBtn.setEnabled(false); //정답버튼 비활성화
        }
    }

    //초기화
    public void reset() {
        min = 1;
        max = 100;
        count = 0;
        responseText.setText("");
    }

    //입력숫자 보여주기
    public void display() {
        requestText.setText(answer);
    }

    //숫자 길이 체크
    public boolean checkLength(String number) {
        boolean result = true;

        //숫자는 10의 자리까지만
        if (number.length() == 2) {
            Toast.makeText(this, "입력불가", Toast.LENGTH_SHORT).show();
            result = false;
        } else {
            result = true;
        }
        return result;
    }

//    private void checkAndRewardCoins() { //하루 3번의 보상만 받을 수 있도록 하였음.
//        String userId = currentUser.getUid(); //유저 아이디 변수 추가
//        DatabaseReference userRef = database.child("users").child(userId); //사용자 데이터 참조
//
//        userRef.child("dailyClears").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                long dailyClears = snapshot.exists() ? (long) snapshot.getValue() : 0;
//                /* 데이터가 존재할 경우 dailyClears에 저장, 그렇지 않으면 0으로 초기화.*/
//
//                if (dailyClears < maxClearsPerDay) { //3번 이상 플레이하지 않은 경우메만 코인을 지급
//                    rewardCoins(userRef, dailyClears);
//                } else {
//                    Toast.makeText(ServeGame_NumberActivity.this, "3회 보상을 받아, 오늘은 더 이상 보상을 받을 수 없습니다.", Toast.LENGTH_SHORT).show();
//                    viewMode("end");
//                    resetButtons();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { //쿼리 실패
//                Toast.makeText(ServeGame_NumberActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    // 보상을 지급하는 메서드
//    private void rewardCoins(DatabaseReference userRef, long dailyClears) {
//        userRef.child("coins").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Long coins = snapshot.exists() ? snapshot.getValue(Long.class) : 0L;
//                if (coins != null) {
//                    final Long updatedCoins = coins + coinReward;  // 코인 업데이트를 최종 변수로 설정
//
//                    Map<String, Object> updates = new HashMap<>();
//                    updates.put("coins", updatedCoins);
//                    updates.put("dailyClears", dailyClears + 1);
//
//                    userRef.updateChildren(updates).addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            Toast.makeText(ServeGame_NumberActivity.this, "정답입니다. " + coinReward + " 코인이 지급되었습니다.", Toast.LENGTH_SHORT).show();
//                            coinText.setText(String.valueOf(updatedCoins)); // 코인 텍스트 업데이트
//                        } else {
//                            Toast.makeText(ServeGame_NumberActivity.this, "코인 지급 오류", Toast.LENGTH_SHORT).show();
//                        }
//                        viewMode("end");
//                        reset();
//                        resetButtons();
//                    });
//                } else {
//                    Toast.makeText(ServeGame_NumberActivity.this, "코인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(ServeGame_NumberActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }


}
