package xyz.luan.audioplayers;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
import android.content.Context;

import java.io.IOException;

public class NewWrappedMediaPlayer extends Player implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {

    private String playerId;

    private String url;
    private double volume = 1.0;
    private boolean respectSilence;
    private boolean stayAwake;
    private ReleaseMode releaseMode = ReleaseMode.RELEASE;

    //是否已经释放了资源
    private boolean released = true;

    //播放器的准备工作是否已经完成
    private boolean prepared = false;
    //是否正在播放中
    boolean playing = false;

    private int shouldSeekTo = -1;

    private MediaPlayer player;
    private MediaplayService ref;



    NewWrappedMediaPlayer(MediaplayService ref, String playerId) {
        this.ref = ref;
        this.playerId = playerId;

    }

    /**
     * Setter methods
     */

    @Override
    void setUrl(String url, boolean isLocal) {
        //如果当前的播放对象为空
        if (!objectEquals(this.url, url)) {
            this.url = url;
            //这个播放器已经被释放了
            if (this.released) {
                this.player = createPlayer();
                this.released = false;
            } else {
                this.playing = false;
                this.prepared = false;
                this.player.stop();
                this.player.reset();
                this.player.release();
                this.player = null;
                this.player = createPlayer();
            }

            try{
                this.setSource(url);
                this.player.setVolume((float) volume, (float) volume);
                this.player.setLooping(this.releaseMode == ReleaseMode.LOOP);
                this.player.prepareAsync();
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }
            catch (RuntimeException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    void setVolume(double volume) {
        if (this.volume != volume) {
            this.volume = volume;
            if (!this.released) {
                this.player.setVolume((float) volume, (float) volume);
            }
        }
    }



    @Override
    void configAttributes(boolean respectSilence, boolean stayAwake, Context context) {
        if (this.respectSilence != respectSilence) {
            this.respectSilence = respectSilence;
            if (!this.released) {
                setAttributes(player);
            }
        }
        if (this.stayAwake != stayAwake) {
            this.stayAwake = stayAwake;
            if (!this.released && this.stayAwake) {
                this.player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
            }
        }
    }

    @Override
    void setReleaseMode(ReleaseMode releaseMode) {
        if (this.releaseMode != releaseMode) {
            this.releaseMode = releaseMode;
            if (!this.released) {
                this.player.setLooping(releaseMode == ReleaseMode.LOOP);
            }
        }
    }

    /**
     * Getter methods
     */

    @Override
    int getDuration() {
        if(this.player!=null && this.playing && this.prepared){
            return this.player.getDuration();
        }else{
            return 0;
        }
    }

    @Override
    int getCurrentPosition() {
        if(this.player!=null && this.playing && this.prepared){
            return this.player.getCurrentPosition();
        }else {
            return 0;
        }
    }

    @Override
    String getPlayerId() {
        return this.playerId;
    }

    @Override
    boolean isActuallyPlaying() {
        return this.playing && this.prepared;
    }

    /**
     * 开始播放
     * Playback handling methods
     */

    @Override
    void play() {
        if (!this.playing) {
            this.playing = true;
            if (this.released) {
                this.released = false;
                this.player = createPlayer();
                this.setSource(url);
                this.player.prepareAsync();
            } else if (this.prepared) {
                this.player.start();
                this.ref.handleIsPlaying(this);
                this.ref.handleResume(this);
            }
        }
    }

    /**
     * 停止播放
     */
    @Override
    void stop() {
        if (this.released) {
            return;
        }

        if (releaseMode != ReleaseMode.RELEASE) {
            if (this.playing) {
                this.playing = false;
                this.player.pause();
                this.player.seekTo(0);
            }
        } else {
            this.release();
        }
    }

    /**
     * 释放资源
     */
    @Override
    void release() {
        if (this.released) {
            return;
        }

        if (this.playing) {
            this.player.stop();
        }
        this.player.reset();
        this.player.release();
        this.player = null;

        this.prepared = false;
        this.released = true;
        this.playing = false;
    }

    /**
     * 暂停播放
     */
    @Override
    void pause() {
        if (this.playing) {
            this.playing = false;
            this.player.pause();
            this.ref.handlePause(this);
        }
    }

    // seek operations cannot be called until after
    // the player is ready.
    @Override
    void seek(int position) {
        if (this.prepared)
            this.player.seekTo(position);
        else
            this.shouldSeekTo = position;
    }

    /**
     * 当播放的网络资源已经准备好了
     * MediaPlayer callbacks
     */

    @Override
    public void onPrepared(final MediaPlayer mediaPlayer) {
        this.prepared = true;
        ref.handleDuration(this);
        if (this.playing) {
            this.player.start();
            ref.handleIsPlaying(this);
            ref.handleResume(this);
        }
        if (this.shouldSeekTo >= 0) {
            this.player.seekTo(this.shouldSeekTo);
            this.shouldSeekTo = -1;
        }
    }

    /**
     * 播放完成
     * @param mediaPlayer
     */
    @Override
    public void onCompletion(final MediaPlayer mediaPlayer) {
        if (releaseMode != ReleaseMode.LOOP) {
            this.stop();
        }
        ref.handleCompletion(this);
    }

    @Override
    public void onSeekComplete(final MediaPlayer mediaPlayer) {
        ref.handleSeekComplete(this);
    }

    /**
     * 创建播放器
     * Internal logic. Private methods
     */

    private MediaPlayer createPlayer() {
        MediaPlayer player = new MediaPlayer();
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnSeekCompleteListener(this);
        setAttributes(player);
        player.setVolume((float) volume, (float) volume);
        player.setLooping(this.releaseMode == ReleaseMode.LOOP);
        return player;
    }

    private void setSource(String url) {
        try {
            this.player.setDataSource(url);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to access resource", ex);
        }
    }

    @SuppressWarnings("deprecation")
    private void setAttributes(MediaPlayer player) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(respectSilence ? AudioAttributes.USAGE_NOTIFICATION_RINGTONE : AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            );
        } else {
            // This method is deprecated but must be used on older devices
            player.setAudioStreamType(respectSilence ? AudioManager.STREAM_RING : AudioManager.STREAM_MUSIC);
        }
    }

    /**
     * 改变倍速
     * @param speed
     */
    @Override
    void setSpeed(double speed) {
        setPlayerSpeed(speed);
    }

    /**
     *
     * @param speed
     */
    void setPlayerSpeed(double speed){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw new UnsupportedOperationException("The method 'setRate' is available only on Android SDK version " + Build.VERSION_CODES.M + " or higher!");
        }
        if (this.player != null) {
            try {
                this.player.setPlaybackParams(this.player.getPlaybackParams().setSpeed((float) speed));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}