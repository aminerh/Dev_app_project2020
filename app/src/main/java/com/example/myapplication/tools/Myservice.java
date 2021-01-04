package com.example.myapplication.tools;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.beans.Song;

import java.io.IOException;
import java.util.ArrayList;

//this is our music player service that launch in background
public class Myservice extends Service  implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,

        AudioManager.OnAudioFocusChangeListener  {
    private static final String TAG ="" ;
    private MyReceiver recv;
    private MediaPlayer mediaPlayer;
    ImageView play;
    ImageView pause;
    ImageView next;
    ImageView prev;
    ArrayList<String> songList=new ArrayList<>() ;
    private Myservice player;
    boolean serviceBound = false;
    ArrayList<Song> audioList;
    Notification notification;


    //path to the audio file
    private String mediaFile;
    private String mediaName;
    private String mediapause;
    private int resumePosition;
    private AudioManager audioManager;


    public int i=0;
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    public Myservice(){}
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        recv=new MyReceiver();
        registerReceiver(recv,new IntentFilter("precedant"));
        registerReceiver(recv,new IntentFilter("PlayPause"));
        registerReceiver(recv,new IntentFilter("next"));
        mediaPlayer=new MediaPlayer();
        // String intent =(String) intent.getExtras().getString("pause");

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        try {
            //An audio file is passed to the service through putExtra();
            mediaFile = intent.getExtras().getString("media");
            mediaName = intent.getExtras().getString("name");

            Bundle list=intent.getExtras();
            if (list !=null){
                audioList=( ArrayList<Song>)list.getSerializable("list");
            }


            Toast.makeText(Myservice.this, mediaName, Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }


        return START_STICKY;
    }

    @Override
    public void onAudioFocusChange(int focusState) {

        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }




    public class  MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action=intent.getAction();
            if (action.equals("PlayPause")){
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.pause();

                }
                else {mediaPlayer.start();
                }
            }
            if (action.equals("next")){
                if (mediaPlayer.isPlaying()){
                    nextsong();

                }
                else {mediaPlayer.start();
                }
            }
            if (action.equals("precedant")){
                if (mediaPlayer.isPlaying()){
                    prevsong();

                }
                else {mediaPlayer.start();
                }
            }

        }
    }
    public void nextsong(){
        if (mediaPlayer.isPlaying()) {
            i++;
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaFile = audioList.get(i ).getPath();
            mediaName= audioList.get(i).getName();

            Toast.makeText(getApplicationContext(), "music n "+i, Toast.LENGTH_LONG).show();
            initMediaPlayer();

            mediaPlayer.start();

        }
    }

    public void prevsong(){
        if (mediaPlayer.isPlaying()) {
            i--;
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaFile = audioList.get(i ).getPath();
            mediaName= audioList.get(i).getName();
            Toast.makeText(getApplicationContext(), "music n "+i, Toast.LENGTH_LONG).show();

            initMediaPlayer();

            // Uri content = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

            // mediaPlayer= MediaPlayer.create(getApplicationContext(), content);

            mediaPlayer.start();

        }
    }


    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(mediaFile);

        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }


    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();

        }
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMedia();
        //stop the service
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }



    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer.isPlaying())mediaPlayer.stop();
        unregisterReceiver(recv);
    }



    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        playMedia();
    }



    //Binding this Client to the AudioPlayer Service
    public class LocalBinder extends Binder {
        public Myservice getService() {
            return Myservice.this;
        }
    }
}

