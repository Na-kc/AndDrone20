package com.example.mygcs2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;


public class PlayerActivity extends YouTubeBaseActivity {
    YouTubePlayerView PlayerView;
    EditText TextView;
    YouTubePlayer.OnInitializedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_playerview);

        //UI 객체생성
        PlayerView = (YouTubePlayerView)findViewById(R.id.playerView);

        //데이터 가져오기
        Intent intent = getIntent();
        String data = intent.getStringExtra("data");

        TextView = (EditText)findViewById(R.id.text1);
        PlayerView = findViewById(R.id.playerView);
        listener = new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
                if (!wasRestored) {
                    Editable Url1 = TextView.getText();
                    youTubePlayer.loadVideo(String.valueOf(Url1));
                }

                //https://www.youtube.com/watch?v=NmkYHmiNArc 유투브에서 v="" 이부분이 키에 해당
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

            }
        };

    }

    //확인 버튼 클릭
    public void okClick(View v){
        //데이터 전달하기
        Intent intent = new Intent();
        //intent.putExtra("result", "Close Popup");
        setResult(RESULT_CANCELED, intent);
        TextView.setVisibility(View.INVISIBLE);
        PlayerView.setVisibility(View.VISIBLE);
        PlayerView.initialize("아무키", listener);
    }

    public void closeClick(View v){
        //데이터 전달하기
        Intent intent = new Intent();
        //intent.putExtra("result", "Close Popup");
        setResult(RESULT_CANCELED, intent);

        //액티비티(팝업) 닫기
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //바깥레이어 클릭시 안닫히게
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //안드로이드 백버튼 막기
        return;
    }
}