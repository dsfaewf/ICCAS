package com.example.testfolder;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SingletonJava {

    private static SingletonJava instance;
    private FirebaseAuth auth;
    private DatabaseReference database;

    private SingletonJava() {
        // Private constructor to prevent instantiation
    }

    public static synchronized SingletonJava getInstance() {
        if (instance == null) {
            instance = new SingletonJava();
        }
        return instance;
    }

    public void initialize(FirebaseAuth auth, DatabaseReference database) {
        this.auth = auth;
        this.database = database;
    }

    public void loadUserCoins(final TextView coinText) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        DatabaseReference userRef = database.child("users").child(currentUser.getUid());

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
                Toast.makeText(coinText.getContext(), "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void checkAndResetDailyClears(FirebaseUser currentUser, DatabaseReference database, TextView coinText, Context context) {
        String userId = currentUser.getUid();
        DatabaseReference userRef = database.child("users").child(userId);

        userRef.child("lastResetDate").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastResetDate = snapshot.exists() ? snapshot.getValue(String.class) : "";
                String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

                if (!currentDate.equals(lastResetDate)) {
                    resetDailyClears(userRef, currentDate, coinText, context);
                } else {
                    loadUserCoins(coinText);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetDailyClears(DatabaseReference userRef, String currentDate, TextView coinText, Context context) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("dailyClears", 0);
        updates.put("lastResetDate", currentDate);

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                loadUserCoins(coinText);
                Toast.makeText(context, "반갑습니다! 오늘도 총 3번 서브게임 보상을 받을 수 있습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "보상 시스템 기회 업데이트 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void checkAndRewardCoins(FirebaseUser currentUser, DatabaseReference database, int maxClearsPerDay, int coinReward, TextView coinText, Context context) {
        String userId = currentUser.getUid();
        DatabaseReference userRef = database.child("users").child(userId);

        userRef.child("dailyClears").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long dailyClears = snapshot.exists() ? (long) snapshot.getValue() : 0;

                if (dailyClears < maxClearsPerDay) {
                    rewardCoins(userRef, dailyClears, coinReward, coinText, context);
                } else {
                    Toast.makeText(context, "오늘은 더 이상 보상을 받을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rewardCoins(DatabaseReference userRef, long dailyClears, int coinReward, TextView coinText, Context context) {
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
                            Toast.makeText(context, "정답입니다. " + coinReward + " 코인이 지급되었습니다.", Toast.LENGTH_SHORT).show();
                            coinText.setText(String.valueOf(finalCoins)); // 코인 텍스트 업데이트
                        } else {
                            Toast.makeText(context, "코인 지급 오류", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(context, "코인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private void checkAndResetDailyClears() {
//        String userId = currentUser.getUid();
//        DatabaseReference userRef = database.child("users").child(userId);
//
//        userRef.child("lastResetDate").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                String lastResetDate = snapshot.exists() ? snapshot.getValue(String.class) : "";
//                String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
//
//                if (!currentDate.equals(lastResetDate)) {
//                    resetDailyClears(userRef, currentDate);
//                } else {
//                    SingletonJava.getInstance().loadUserCoins(coinText);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(ServeGameBaseballActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void resetDailyClears(DatabaseReference userRef, String currentDate) {
//        Map<String, Object> updates = new HashMap<>();
//        updates.put("dailyClears", 0);
//        updates.put("lastResetDate", currentDate);
//
//        userRef.updateChildren(updates).addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                SingletonJava.getInstance().loadUserCoins(coinText);
//                Toast.makeText(ServeGameBaseballActivity.this, "dailyClears가 초기화되었습니다.", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(ServeGameBaseballActivity.this, "초기화 오류", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void checkAndRewardCoins() {
//        String userId = currentUser.getUid();
//        DatabaseReference userRef = database.child("users").child(userId);
//
//        userRef.child("dailyClears").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                long dailyClears = snapshot.exists() ? (long) snapshot.getValue() : 0;
//
//                if (dailyClears < maxClearsPerDay) {
//                    rewardCoins(userRef, dailyClears);
//                } else {
//                    Toast.makeText(ServeGameBaseballActivity.this, "오늘은 더 이상 보상을 받을 수 없습니다.", Toast.LENGTH_SHORT).show();
//                    viewMode("end");
//                    reset();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(ServeGameBaseballActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void rewardCoins(DatabaseReference userRef, long dailyClears) {
//        userRef.child("coins").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Long coins = snapshot.exists() ? snapshot.getValue(Long.class) : 0L;
//                if (coins != null) {
//                    coins += coinReward;
//
//                    Map<String, Object> updates = new HashMap<>();
//                    updates.put("coins", coins);
//                    updates.put("dailyClears", dailyClears + 1);
//
//                    Long finalCoins = coins;
//                    userRef.updateChildren(updates).addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            Toast.makeText(ServeGameBaseballActivity.this, "정답입니다. " + coinReward + " 코인이 지급되었습니다.", Toast.LENGTH_SHORT).show();
//                            coinText.setText(String.valueOf(finalCoins)); // 코인 텍스트 업데이트
//                        } else {
//                            Toast.makeText(ServeGameBaseballActivity.this, "코인 지급 오류", Toast.LENGTH_SHORT).show();
//                        }
//                        viewMode("end");
//                        reset();
//                    });
//                } else {
//                    Toast.makeText(ServeGameBaseballActivity.this, "코인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(ServeGameBaseballActivity.this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
}



