package com.example.flappybird;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;


import java.util.ArrayList;

public class GameView extends View {
    private Handler handler;
    private Runnable r;
    private ArrayList<Pipe> arrPipes;
    private Bird bird;

    private int sumpipe,distance;
    private int score,bestscore=0;
    private  boolean start;
    private Context context;

    private int soundJump;
    private float volume;
    private boolean loadedsound;

    private SoundPool soundPool;


    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
        SharedPreferences sp=context.getSharedPreferences("gamesetting",Context.MODE_PRIVATE);
        if(sp!=null){
            bestscore=sp.getInt("bestscore",0);
        }
        score=0;
        start=false;
        initBird();
        initPipe();

        handler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                invalidate();

            }
        };
        if(Build.VERSION.SDK_INT>=21){
            AudioAttributes audioAttributes=new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            SoundPool.Builder builder=new SoundPool.Builder();
            builder.setAudioAttributes(audioAttributes).setMaxStreams(5);
            this.soundPool=builder.build();
        }else {
            soundPool=new SoundPool(5, AudioManager.STREAM_MUSIC,0);
        }
        this.soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loadedsound=true;
            }
        });
        soundJump=this.soundPool.load(context,R.raw.jump_02,1);

    }

    private void initPipe() {
        sumpipe = 6;
        distance = 700 * Constants.SCREEN_HEIGHT / 1920;
        arrPipes = new ArrayList<>();

        for (int i = 0; i < sumpipe / 2; i++) {
            // Calculate X position for pipes
            float pipeX = Constants.SCREEN_WIDTH + i * ((Constants.SCREEN_WIDTH + 200 * Constants.SCREEN_WIDTH / 1080) / (sumpipe / 2));

            // Initialize and add top pipe
            Pipe topPipe = new Pipe(
                    pipeX,
                    0,
                    200 * Constants.SCREEN_WIDTH / 1080,
                    Constants.SCREEN_HEIGHT / 2
            );

            Bitmap topPipeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pipe2);
            if (topPipeBitmap != null) {
                topPipe.setBm(topPipeBitmap);
                topPipe.randomY();
                arrPipes.add(topPipe);
            }

            // Initialize and add bottom pipe
            Pipe bottomPipe = new Pipe(
                    pipeX,
                    topPipe.getY() + topPipe.getHeight() + distance,
                    200 * Constants.SCREEN_WIDTH / 1080,
                    Constants.SCREEN_HEIGHT / 2
            );

            Bitmap bottomPipeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pipe1);
            if (bottomPipeBitmap != null) {
                bottomPipe.setBm(bottomPipeBitmap);
                arrPipes.add(bottomPipe);
            }
        }
    }


    private void initBird(){
        bird = new Bird();
        bird.setWidth(100 * Constants.SCREEN_WIDTH / 1080);
        bird.setHeight(100 * Constants.SCREEN_HEIGHT / 1920);
        bird.setX(100 * Constants.SCREEN_WIDTH / 1080);
        bird.setY(Constants.SCREEN_HEIGHT / 2 - bird.getHeight() / 2);
        ArrayList<Bitmap> arrbms = new ArrayList<>();
        arrbms.add(BitmapFactory.decodeResource(getResources(), R.drawable.bird1));
        arrbms.add(BitmapFactory.decodeResource(getResources(), R.drawable.bird2));
        bird.setArrbms(arrbms);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (start) {
            bird.draw(canvas);
            for (int i = 0; i < arrPipes.size(); i++) {
                if (bird.getRect().intersect(arrPipes.get(i).getRect()) || bird.getY() - bird.getHeight() < 0 || bird.getY() > Constants.SCREEN_HEIGHT) {
                    Pipe.speed=0;
                    MainActivity.txt_score_over.setText(MainActivity.txt_score.getText());
                    MainActivity.txt_best_score.setText("Best Score:"+bestscore);
                    MainActivity.txt_best_score.setVisibility(VISIBLE);
                    MainActivity.rl_game_over.setVisibility(VISIBLE);
                }
            if (this.bird.getX() + this.bird.getWidth() > arrPipes.get(i).getX() + arrPipes.get(i).getWidth() / 2
                    && this.bird.getX() + this.bird.getWidth() <= arrPipes.get(i).getX() + arrPipes.get(i).getWidth() / 2 + Pipe.speed
                    && i < sumpipe / 2) {
                score++;
                if(score>bestscore){
                    bestscore=score;
                    SharedPreferences sp=context.getSharedPreferences("gamesetting",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor=sp.edit();
                    editor.putInt("bestscore",bestscore);
                    editor.apply();
                }
                MainActivity.txt_score.setText("" + score);
            }
            if (this.arrPipes.get(i).getX() < -arrPipes.get(i).getWidth()) {
                this.arrPipes.get(i).setX(Constants.SCREEN_WIDTH);
                if (i < sumpipe / 2) {
                    arrPipes.get(i).randomY();
                } else {
                    arrPipes.get(i).setY(this.arrPipes.get(i - sumpipe / 2).getY()
                            + this.arrPipes.get(i - sumpipe / 2).getHeight() + this.distance);
                }
            }
            this.arrPipes.get(i).draw(canvas);
        }
    }else{
            if(bird.getY()>Constants.SCREEN_HEIGHT/2){
                bird.setDrop(-15*Constants.SCREEN_HEIGHT/1920);
            }
            bird.draw(canvas);
        }
        handler.postDelayed(r, 10);
    }


    //make the bird soar when touch screen

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            bird.setDrop(-15);
            if(loadedsound){
                int streamId=this.soundPool.play(this.soundJump,(float)0.5,(float)0.5,1,0,1f);
            }
        }
        return true;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public void reset() {
        MainActivity.txt_score.setText("0");
        score=0;
        initPipe();
        initBird();
    }
}
