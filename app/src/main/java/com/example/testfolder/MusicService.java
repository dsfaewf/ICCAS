package com.example.testfolder;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import javax.annotation.Nullable;

public class MusicService extends Service {
    MediaPlayer mediaPlayer;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();

        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        mediaPlayer.setLooping(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startld){
        mediaPlayer.start();
        return super.onStartCommand(intent,flags,startld);
    }

    @Override
    public void onDestroy(){
        mediaPlayer.stop();
        mediaPlayer.release();
        super.onDestroy();
    }
}
