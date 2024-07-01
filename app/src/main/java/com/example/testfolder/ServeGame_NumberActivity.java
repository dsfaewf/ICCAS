package com.example.testfolder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Random;

public class ServeGame_NumberActivity extends AppCompatActivity implements View.OnClickListener {
    RelativeLayout rootView;
    GridLayout gridLayout;
    TextView requestText, responseText;
    Button startBtn, answerBtn, backBtn;
    String answer = "";
    int randomNumber;

    int min = 1;
    int max = 100;

    //도전할 횟수 카운트
    int count = 7;

    HashSet<Character> enteredNumbers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serve_game_number);

        // View 초기화
        rootView = findViewById(R.id.main);
        gridLayout = findViewById(R.id.grid_layout);

        requestText = findViewById(R.id.request_text);
        responseText = findViewById(R.id.response_text);

        startBtn = findViewById(R.id.start_btn);
        answerBtn = findViewById(R.id.answer_btn);
        backBtn = findViewById(R.id.back_btn);

        // 버튼 숨김
        viewMode("end");

        // 시작버튼
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewMode("start");

                // 랜덤숫자 생성
                Random random = new Random();
                randomNumber = random.nextInt(100) + 1; // 1~100

                // 시작 버튼을 누를 때 정답 텍스트 초기화
                responseText.setText("");
                requestText.setText("기회 7번");

                Toast.makeText(ServeGame_NumberActivity.this, "숫자가 생성되었습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // 정답 버튼 클릭 이벤트
        answerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count--; // 도전횟수 감소

                // 도전횟수가 0번이 될 때
                if (count == 0) {
                    Toast.makeText(ServeGame_NumberActivity.this, "도전 횟수를 초과했습니다.", Toast.LENGTH_SHORT).show();
                    viewMode("end");
                    reset();
                } else {
                    // 입력숫자
                    int inputNumber;
                    try {
                        inputNumber = Integer.parseInt(requestText.getText().toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(ServeGame_NumberActivity.this, "유효한 숫자를 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (inputNumber > randomNumber) {
                        max = inputNumber;
                        responseText.setText(count + " 번째 " + min + " ~ " + max);
                    } else if (inputNumber < randomNumber) {
                        min = inputNumber;
                        responseText.setText(count + " 번째 " + min + " ~ " + max);
                    } else {
                        Toast.makeText(ServeGame_NumberActivity.this, "정답입니다.", Toast.LENGTH_SHORT).show();
                        viewMode("end");
                        reset();
                    }

                    requestText.setText("기회 " + count + "번");

                    answer = "";
                }
            }
        });

        // 뒤로가기 버튼 클릭 이벤트
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 현재 액티비티 종료
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
                enteredNumbers.add(buttonText.charAt(0));
                answer = answer + buttonText;
                display();
            } else {
                Toast.makeText(this, "같은 숫자는 중복해서 입력할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 버튼 초기화
    private void resetButtons() {
        answer = "";
        requestText.setText("");
        enteredNumbers.clear();
    }

    // 게임타입
    public void viewMode(String type) {
        if (type.equals("start")) {
            gridLayout.setVisibility(View.VISIBLE);
            startBtn.setEnabled(false);
            answerBtn.setEnabled(true);
            count = 7;
        } else {
            gridLayout.setVisibility(View.INVISIBLE);
            startBtn.setEnabled(true);
            answerBtn.setEnabled(false);
        }
    }

    // 초기화
    public void reset() {
        min = 1;
        max = 100;
        count = 7;
        responseText.setText("");
        requestText.setText("기회 7번");
        resetButtons();
    }

    // 입력숫자 보여주기
    public void display() {
        requestText.setText(answer);
    }

    // 숫자 길이 체크
    public boolean checkLength(String number) {
        return number.length() < 3;
    }
}
