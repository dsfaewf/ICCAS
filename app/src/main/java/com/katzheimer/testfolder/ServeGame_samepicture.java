package com.katzheimer.testfolder;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent; // 추가된 import 구문
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.katzheimer.testfolder.R;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ServeGame_samepicture extends AppCompatActivity {
    private android.widget.GridLayout gridLayout;
    private int[] images;
    private int[] buttonIds;
    private ImageButton firstSelected, secondSelected;
    private boolean isProcessing = false;
    private int cardsMatched = 0;
    private List<Integer> matchedButtons = new ArrayList<>();
    private List<Integer> selectedImages; // 클래스 멤버 변수로 변경

    private ProgressBar progressBar;
    private TextView timerTextView;
    private TextView coinText; // 코인 텍스트뷰 추가
    private int progressStatus = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean gameEnded = false;
    private long startTimeMillis;

    private static final int COIN_REWARD = 10; // 코인 보상 - 어려운 편이 아니었네 ㅎ 오류 수정후 다시 보상을 10으로 조정
    private static final int MAX_CLEARS_PER_DAY = 3; // 하루 최대 클리어 횟수

    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serve_game_samepicture);

        gridLayout = findViewById(R.id.gridLayout);
        progressBar = findViewById(R.id.progressBar1);
        timerTextView = findViewById(R.id.timerTextView);
        coinText = findViewById(R.id.coin_text); // 코인 텍스트뷰 초기화

        currentUser = SingletonJava.getInstance().getCurrentUser(); // 싱글톤으로 현재 사용자 가져오기
        SingletonJava.getInstance().loadUserCoins(coinText); // 코인 정보 가져옴

        images = new int[]{
                R.drawable.image1, R.drawable.image2, R.drawable.image3, R.drawable.image4,
                R.drawable.image5, R.drawable.image6, R.drawable.image7, R.drawable.image8,
                R.drawable.image9, R.drawable.image10, R.drawable.image11, R.drawable.image12,
                R.drawable.image13, R.drawable.image14, R.drawable.image15, R.drawable.image16,
                R.drawable.image17, R.drawable.image18, R.drawable.image19, R.drawable.image20
        };

        // Shuffle the images array to ensure randomness
        List<Integer> imageList = new ArrayList<>();
        for (int image : images) {
            imageList.add(image);
        }
        Collections.shuffle(imageList);
        images = new int[imageList.size()];
        for (int i = 0; i < imageList.size(); i++) {
            images[i] = imageList.get(i);
        }

        // Select 8 random images and duplicate them to form 16 cards
        int numCards = 8; // Number of different pairs
        selectedImages = new ArrayList<>();
        Random random = new Random();
        HashSet<Integer> selectedIndices = new HashSet<>(); // 중복 확인을 위한 Set

        while (selectedImages.size() < numCards * 2) {
            int imageIndex = random.nextInt(images.length);
            if (!selectedIndices.contains(imageIndex)) {
                selectedIndices.add(imageIndex);
                selectedImages.add(images[imageIndex]);
                selectedImages.add(images[imageIndex]); // Add the same image twice
            }
        }

        // Assign selected images to buttonIds
        buttonIds = new int[]{
                R.id.imageButton1, R.id.imageButton2, R.id.imageButton3, R.id.imageButton4,
                R.id.imageButton5, R.id.imageButton6, R.id.imageButton7, R.id.imageButton8,
                R.id.imageButton9, R.id.imageButton10, R.id.imageButton11, R.id.imageButton12,
                R.id.imageButton13, R.id.imageButton14, R.id.imageButton15, R.id.imageButton16
        };

        // Shuffle the buttonIds array to randomize positions
        List<Integer> buttonIdList = new ArrayList<>();
        for (int buttonId : buttonIds) {
            buttonIdList.add(buttonId);
        }
        Collections.shuffle(buttonIdList);
        buttonIds = new int[buttonIdList.size()];
        for (int i = 0; i < buttonIdList.size(); i++) {
            buttonIds[i] = buttonIdList.get(i);
        }

        // Initialize timer start time
        startTimeMillis = System.currentTimeMillis();

        // 이미지 버튼에 클릭 리스너 설정 및 초기 이미지 설정
        for (int i = 0; i < buttonIds.length; i++) {
            final ImageButton button = findViewById(buttonIds[i]);
            button.setScaleType(ImageButton.ScaleType.CENTER_CROP);
            button.setImageResource(R.drawable.test_cover_image); // 초기 커버 이미지 설정
            final int index = i; // 클로저에서 사용하기 위해 인덱스 final로 선언

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCardClicked(button, index);
                }
            });
        }

        startTimer(); // 타이머 시작
    }

    private void onCardClicked(final ImageButton selectedButton, final int index) {
        if (isProcessing || matchedButtons.contains(selectedButton.getId()) || !selectedButton.isEnabled()) return;

        // 이미지가 부드럽게 변경되도록 alpha 애니메이션 추가
        ObjectAnimator flipAnimator = ObjectAnimator.ofFloat(selectedButton, "alpha", 1f, 0f);
        flipAnimator.setDuration(250); // 0.25초 동안 애니메이션 실행

        // 애니메이션 완료 리스너 추가
        flipAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isProcessing = true; // 처리 중 플래그 설정
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                selectedButton.setImageResource(selectedImages.get(index));
                selectedButton.setTag(index); // 해당 버튼에 이미지 인덱스를 태그로 저장

                // 이미지가 설정된 후 다시 alpha 애니메이션 추가
                ObjectAnimator flipBackAnimator = ObjectAnimator.ofFloat(selectedButton, "alpha", 0f, 1f);
                flipBackAnimator.setDuration(250);
                flipBackAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        // 애니메이션 시작 시 필요한 작업
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        // 애니메이션 종료 시 처리할 작업
                        if (firstSelected == null) {
                            firstSelected = selectedButton;
                        } else if (secondSelected == null && selectedButton != firstSelected) {
                            secondSelected = selectedButton;

                            // 애니메이션이 모두 완료된 후에 비교 수행
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    checkMatch();
                                }
                            }, 500);
                        }
                        isProcessing = false; // 처리 완료 플래그 설정
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        // 애니메이션 취소 시 처리할 작업
                        isProcessing = false; // 처리 완료 플래그 설정
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                        // 애니메이션 반복 시 처리할 작업
                    }
                });
                flipBackAnimator.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                // 애니메이션 취소 시 처리할 작업
                isProcessing = false; // 처리 완료 플래그 설정
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                // 애니메이션 반복 시 처리할 작업
            }
        });

        flipAnimator.start();
    }

    private void checkMatch() {
        if (firstSelected == null || secondSelected == null) return;

        // 선택한 두 개의 카드가 같은지 확인
        int firstIndex = (int) firstSelected.getTag();
        int secondIndex = (int) secondSelected.getTag();

        boolean isMatched = selectedImages.get(firstIndex).equals(selectedImages.get(secondIndex));

        if (isMatched) {
            // 매치되었을 경우 처리
            Toast.makeText(this, "Matched!", Toast.LENGTH_SHORT).show();
            matchedButtons.add(firstSelected.getId());
            matchedButtons.add(secondSelected.getId());
            cardsMatched += 2;

            // 모든 카드가 매치되었는지 확인
            if (cardsMatched == buttonIds.length) {
                // 코인 보상
                SingletonJava.getInstance().checkAndRewardCoins(currentUser,
                        MAX_CLEARS_PER_DAY, COIN_REWARD, coinText, this);
                endGame(); // 게임 종료 처리
            }
        } else {
            // 매치되지 않았을 경우 처리
            Toast.makeText(this, "Not matched. Try again.", Toast.LENGTH_SHORT).show();

            // 다시 커버 이미지로 변경
            flipCard(firstSelected, R.drawable.test_cover_image);
            flipCard(secondSelected, R.drawable.test_cover_image);
        }

        // 선택한 카드 초기화
        firstSelected = null;
        secondSelected = null;
    }

    private void flipCard(final ImageButton button, final int imageResource) {
        // 이미지가 부드럽게 변경되도록 alpha 애니메이션 추가
        ObjectAnimator flipAnimator = ObjectAnimator.ofFloat(button, "alpha", 1f, 0f);
        flipAnimator.setDuration(250); // 0.25초 동안 애니메이션 실행

        // 애니메이션 완료 리스너 추가
        flipAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isProcessing = true; // 처리 중 플래그 설정
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                button.setImageResource(imageResource);

                // 이미지가 설정된 후 다시 alpha 애니메이션 추가
                ObjectAnimator flipBackAnimator = ObjectAnimator.ofFloat(button, "alpha", 0f, 1f);
                flipBackAnimator.setDuration(250);
                flipBackAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        // 애니메이션 시작 시 필요한 작업
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        // 애니메이션 종료 시 처리할 작업
                        isProcessing = false; // 처리 완료 플래그 설정
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        // 애니메이션 취소 시 처리할 작업
                        isProcessing = false; // 처리 완료 플래그 설정
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                        // 애니메이션 반복 시 처리할 작업
                    }
                });
                flipBackAnimator.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                // 애니메이션 취소 시 처리할 작업
                isProcessing = false; // 처리 완료 플래그 설정
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                // 애니메이션 반복 시 처리할 작업
            }
        });

        flipAnimator.start();
    }

    private void startTimer() {
        new Thread(() -> {
            while (progressStatus < 300 && !gameEnded) {  // 0.2 * 300 = 60초
                progressStatus += 1;
                handler.post(() -> {
                    progressBar.setProgress(progressStatus);
                    updateTimerText(); // 타이머 텍스트 업데이트
                });
                try {
                    Thread.sleep(200);   // 0.2초 대기
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!gameEnded) {
                handler.post(() -> {
                    endGame(); // 타이머가 종료되었을 때 게임 종료 처리
                });
            }
        }).start();
    }

    private void updateTimerText() {
        long elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;
        String time = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis),
                TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis)));
        timerTextView.setText(time);
    }

    private void endGame() {
        gameEnded = true;
        Toast.makeText(this, "Time's up! Game Over.", Toast.LENGTH_LONG).show();
        navigateToGameList(); // 게임 종료 시 게임 목록으로 이동
    }
    private void navigateToGameList() {
        Intent intent = new Intent(this, GamelistActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // 현재 액티비티 종료
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToGameList(); // 이전 화면으로 돌아갈 때 게임 목록으로 이동
    }
}
