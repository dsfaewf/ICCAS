package com.example.testfolder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class ServeGame_NumberActivity extends AppCompatActivity implements View.OnClickListener {
    GridLayout gridLayout;
    TextView requestText, responseText;
    Button startBtn, answerBtn;
    Button[] buttons = new Button[10];
    String answer = "";
    int randomNumber;

    int min = 1;
    int max = 100;

    //도전할 횟수 카운트
    int count = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serve_game_number);

        //버튼 레이아웃
        gridLayout = findViewById(R.id.grid_layout);

        //입력된 숫자가 보여지는 텍스트뷰
        requestText = findViewById(R.id.request_text);

        //정답이 맞는지 보여주는 텍스트뷰
        responseText = findViewById(R.id.response_text);

        //정답버튼
        answerBtn = findViewById(R.id.answer_btn);  // Fixed the ID

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
                        Toast.makeText(ServeGame_NumberActivity.this, "정답입니다.", Toast.LENGTH_SHORT).show();

                        //버튼 숨김
                        viewMode("end");

                        //초기화
                        reset();
                        resetButtons(); //버튼 초기화
                    }

                    //입력창 초기화
                    requestText.setText("");

                    //정답 변수
                    answer = "";
                }
            }
        });
    }//onCreate();

    //버튼 클릭 이벤트
    @Override
    public void onClick(View view) {
        int id = view.getId();

        //숫자 길이 체크
        if (checkLength(answer)) {
            if (id == R.id.btn0) {
                answer = answer + "0";
                display();
            } else if (id == R.id.btn1) {
                answer = answer + "1";
                display();
            } else if (id == R.id.btn2) {
                answer = answer + "2";
                display();
            } else if (id == R.id.btn3) {
                answer = answer + "3";
                display();
            } else if (id == R.id.btn4) {
                answer = answer + "4";
                display();
            } else if (id == R.id.btn5) {
                answer = answer + "5";
                display();
            } else if (id == R.id.btn6) {
                answer = answer + "6";
                display();
            } else if (id == R.id.btn7) {
                answer = answer + "7";
                display();
            } else if (id == R.id.btn8) {
                answer = answer + "8";
                display();
            } else if (id == R.id.btn9) {
                answer = answer + "9";
                display();
            }
        }
    }

    //버튼 초기화
    private void resetButtons() {
        answer = ""; //정답 변수 초기화
        requestText.setText(""); //입력창 초기화
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
}
