package xyz.luan.audioplayers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MediaplayService extends Service {
    /**
     * 播放诵读了
     */
    public static String IntentPlayChantRead = "MusicServiceIntentSwitchPause";

    /**
     * 准备播放的状态
     */
    public static String IntentPlayStatus = "MusicServicePlayStatus";

    /**
     * 暂停播放的状态
     */
    public static String IntentPauseStatus = "MusicServicePauseStatus";

    /**
     * 获取播放器
     */
    public static String IntentGetPlayerMusic = "MusicPlayerGet";

    //播放器开始播放
    public static String MusicIntentPlayerMusic = "MusicPlayerPlayingMusic";

    //播放器进度
    public static String MusicHandleDuration = "handleDuration";

    //播放完成
    public static String MusichandleCompletion = "handleCompletion";

    //播放完成
    public static String MusichandleSeekComplete = "handleSeekComplete";

    //暂停播放
    public static String MusichandlePause = "handlePause";

    //恢复播放
    public static String MusichandleResume = "handleResume";


    public static String MusicResume = "resume";

    public static String MusicPause = "pause";

    //
    public static String MusicStop = "MusicStop";

    //
    public static String MusicRelease = "MusicRelease";

    //
    public static String MusicSeek = "seek";

    //
    public static String MusicSetVolume = "Volume";

    public static String MusicUrl = "setUrl";

    public static String MusicGetDuration = "GetDuration";

    ///
    public static String MusicCurrentPosition = "GetCurrentPosition";

    public static String MusicSetReleaseMode = "SetReleaseMode";

    //改变播放倍数
    public static String MusicSpeed = "SetMusicSpeed";

    ///
    public static String MusicDefault = "MusicDefault";


    ///Notification 的暂停
    public static String NotificationPause = "Notification_Pause";

    ///Notification 的播放
    public static String NotificationPlay = "Notification_Play";

    ///Notification 的停止
    public static String NotificationClose = "Notification_Close";

    //播放和暂停的切换
    public static String NotificationSwitchPlay = "Notification_SwitchPlay";

    //Service
    private boolean registerService = false;

   //private NotificationBarMediaplayer builder;

    //
    private NotificationManager notificationManager;
    private PendingIntent pendingIntent  = null;
    private  PendingIntent pendingIntent1 = null;
    private  PendingIntent pendingIntent2  = null;
    //上一曲
    private  PendingIntent pendingIntentUp =null;
    private RemoteViews contentViews  = null;
    private Bitmap iconBitmap = null;

    //通知栏
    private NotificationCompat.Builder builder;

    //音频焦点
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        if(builder==null){
            createNotification();
            //1 初始化AudioManager对象
        }
    }

    /**
     * 创建通知栏
     */
    void createNotification(){
        String channelId = "13";
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(builder==null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NotificationChannel channel  = new NotificationChannel(channelId,"naoxuejia", NotificationManager.IMPORTANCE_LOW);
                channel.setSound(null,null);
                notificationManager.createNotificationChannel(channel);
            }
            builder = new NotificationCompat.Builder(getApplicationContext(),channelId);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setOngoing(true);//用户无法滑动删除，也不能通过手机的清除键删除，类似于墨迹天气等app的通知栏
        }
        registerServiceIntent();
    }


    /**
     * 创建自定义消息通知栏
     * @param audioModelUrl
     * @param IntentType
     */
    void createPostCustomNotification(String audioModelUrl,String IntentType) {
        if (audioModelUrl == "") {
            if (IntentType == MediaplayService.IntentPlayChantRead) {
                cancelAllNotification();
            } else if (IntentType == MediaplayService.NotificationClose) {
                cancelAllNotification();
            }
            return;
        }

        if (iconBitmap != null) {
            iconBitmap.recycle();
            iconBitmap = null;
        }

        contentViews = new RemoteViews(getApplication().getPackageName(), R.layout.notifiction_play);
        contentViews.setImageViewResource(R.id.notifiction_play, android.R.drawable.ic_media_pause);
        contentViews.setTextViewText(
                R.id.notifiction_title,
                "脑学家"
        );
        if (IntentType == MediaplayService.IntentPauseStatus) {
            contentViews.setImageViewResource(R.id.notifiction_play, android.R.drawable.ic_media_play);
        } else if (IntentType == MediaplayService.IntentPlayStatus) {
            contentViews.setImageViewResource(R.id.notifiction_play, android.R.drawable.ic_media_pause);
        }

        contentViews.setImageViewResource(R.id.notifiction_head_image,R.mipmap.ic_launcher);

        if (pendingIntent == null) {
            createPending(contentViews);
        }

        if (builder == null) {
            createNotification();
        }

        builder.setContent(contentViews);
        //发出一条消息

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            startForeground(1, builder.build());
        }else{
            notificationManager.notify(1, builder.build());
        }
    }

    /**
     * 取消
     */
    private void cancelPending() {
        if (pendingIntent != null) {
            pendingIntent.cancel();
            pendingIntent = null;
        }

        if(pendingIntent1!=null){
            pendingIntent1.cancel();
            pendingIntent1 = null;
        }

        if(pendingIntent2!=null){
            pendingIntent2.cancel();
            pendingIntent2 = null;
        }

        if(pendingIntentUp!=null){
            pendingIntentUp.cancel();
            pendingIntentUp = null;
        }
    }

    /**
     * 取消Notification
     */
    void cancelAllNotification() {
        cancelPending();
        builder = null;
        if (iconBitmap != null) {
            iconBitmap.recycle();
            iconBitmap = null;
        }
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            stopForeground(true);
        }else{
            notificationManager.cancelAll();
        }
    }

    /**
     * 注册通知栏事件点击
     * @param contentViews RemoteViews
     */
     void createPending(RemoteViews contentViews) {
        /**
         val intent = Intent(MediaPlayerService.IntentNextPlay)
         pendingIntent =
         PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
         contentViews.setOnClickPendingIntent(R.id.notifiction_next, pendingIntent)**/
        if(Build.VERSION.SDK_INT >= 3){
            Intent intent2 = new Intent(MediaplayService.NotificationSwitchPlay);
            pendingIntent1 =
                    PendingIntent.getBroadcast(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
            contentViews.setOnClickPendingIntent(R.id.notifiction_play, pendingIntent1);

            Intent intent3 = new Intent(MediaplayService.NotificationClose);
            pendingIntent2 =
                    PendingIntent.getBroadcast(this, 0, intent3, PendingIntent.FLAG_UPDATE_CURRENT);
            contentViews.setOnClickPendingIntent(R.id.notifiction_close, pendingIntent2);
        }
        //上一曲
        /**
         val intentUp = Intent(MediaPlayerService.IntentUpPlay)
         pendingIntentUp = PendingIntent.getBroadcast(context,0,intentUp, PendingIntent.FLAG_UPDATE_CURRENT)
         PendingIntent.getBroadcast(context, 0, intent3, PendingIntent.FLAG_UPDATE_CURRENT)
         contentViews.setOnClickPendingIntent(R.id.notifiction_previous, pendingIntentUp)**/
    }


    /**
     * 注册广播事件实现播放器的控制
     */
    private void registerServiceIntent(){
        if(!registerService){
            registerService = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MediaplayService.IntentPauseStatus);
            intentFilter.addAction(MediaplayService.IntentPlayStatus);
            intentFilter.addAction(IntentGetPlayerMusic);
            intentFilter.addAction(MusicResume);
            intentFilter.addAction(MusicStop);
            intentFilter.addAction(MusicPause);
            intentFilter.addAction(MusicRelease);
            intentFilter.addAction(MusicSeek);
            intentFilter.addAction(MusicSetVolume);
            intentFilter.addAction(MusicUrl);
            intentFilter.addAction(MusicGetDuration);
            intentFilter.addAction(MusicCurrentPosition);
            intentFilter.addAction(MusicSetReleaseMode);
            intentFilter.addAction(MusicDefault);
            intentFilter.addAction(MusicSpeed);

            this.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
        }
    }


    private Player player;
    /**
     * 接受广播消息并且控制显示
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MediaplayService.IntentPlayStatus)){
                if(requestFocus()){
                    Log.d("krcm110","开始播放");
                    String url = intent.getStringExtra("url");
                    double volume = intent.getDoubleExtra("volume",100.0);
                    Integer position = intent.getIntExtra("position",100);
                    Boolean respectSilence = intent.getBooleanExtra("respectSilence",false);
                    Boolean isLocal = intent.getBooleanExtra("isLocal",false);
                    Boolean stayAwake = intent.getBooleanExtra("stayAwake",false);
                    String mode = intent.getStringExtra("mode");
                    String playerId = intent.getStringExtra("playerId");

                    player.configAttributes(respectSilence, stayAwake, context);
                    player.setVolume(volume);
                    player.setUrl(url, isLocal);

                    if (position != null && !mode.equals("PlayerMode.LOW_LATENCY")) {
                        player.seek(position);
                    }
                    player.play();
                    createPostCustomNotification("music",MediaplayService.IntentPlayStatus);
                }


            }else if(intent.getAction().equals(MediaplayService.IntentGetPlayerMusic)){
                final String playerId = intent.getStringExtra("playerId");
                final String mode = intent.getStringExtra("mode");
                player = getPlayer(playerId, mode);
                ///获取播放器
            }else  if(intent.getAction().equals(MusicResume)){
                if(requestFocus()){
                player.play();
                createPostCustomNotification("music",MediaplayService.IntentPlayStatus);
                }
            }else if(intent.getAction().equals(MusicPause)){
                player.pause();
                Boolean closed = intent.getBooleanExtra("closed",false);
                    if(closed){
                        createPostCustomNotification("",MediaplayService.NotificationClose);
                    }else{
                        createPostCustomNotification("music",MediaplayService.IntentPauseStatus);
                    }
            }else if(intent.getAction().equals(MusicStop)){
                player.stop();
                    createPostCustomNotification("",MediaplayService.NotificationClose);
            }else if(intent.getAction().equals(MusicRelease)){
                player.release();
                    createPostCustomNotification("music",MediaplayService.NotificationClose);
            }else if(intent.getAction().equals(MusicSeek)){
                final Integer position = intent.getIntExtra("position",0);
                player.seek(position);
            }else if(intent.getAction().equals(MusicSetVolume)){
                final Double volume = intent.getDoubleExtra("volume",50.0);
                player.setVolume(volume);
            }else if(intent.getAction().equals(MusicUrl)){
                final boolean isLocal = intent.getBooleanExtra("isLocal",false);
                final String url = intent.getStringExtra("url");
                player.setUrl(url, isLocal);
            }else if(intent.getAction().equals(MusicGetDuration)){
                //response.success(player.getDuration());
                return;
            }else if(intent.getAction().equals(MusicCurrentPosition)){
                //response.success(player.getCurrentPosition());
                return;
            }else if(intent.getAction().equals(MusicSetReleaseMode)){
                final String releaseModeStr = intent.getStringExtra("releaseMode");
                final ReleaseMode releaseMode = ReleaseMode.valueOf(releaseModeStr.substring("ReleaseMode.".length()));
                player.setReleaseMode(releaseMode);
            }else if(intent.getAction().equals(MusicSpeed)){
                final Double speed = intent.getDoubleExtra("playbackRate",1.0);
                if (player!=null){
                    player.setSpeed(speed);
                }
            }
            else if(intent.getAction().equals(MusicDefault)){
               // response.notImplemented();
                return;
            }
        }
    };


    /**
     * 音频焦点处理逻辑
     */
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_LOSS:
                    //长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
                    //会触发此回调事件，例如播放QQ音乐，网易云音乐等
                    //通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
                    Log.d("Android_krcm110", "AUDIOFOCUS_LOSS");
                    //stop();
                    //释放焦点，该方法可根据需要来决定是否调用
                    //若焦点释放掉之后，将不会再自动获得

                    Intent  intentPause = new Intent();
                    intentPause.setAction(MediaplayService.MusicPause);
                    getApplicationContext().sendBroadcast(intentPause);

                    audioManager.abandonAudioFocus(mAudioFocusChange);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
                    //会触发此回调事件，例如播放短视频，拨打电话等。
                    //通常需要暂停音乐播放
                    Intent  intentPause1 = new Intent();
                    intentPause1.setAction(MediaplayService.MusicPause);
                    getApplicationContext().sendBroadcast(intentPause1);
                    Log.d("Android_krcm110", "AUDIOFOCUS_LOSS_TRANSIENT");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //短暂性丢失焦点并作降音处理
                    Log.d("Android_krcm110", "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    Intent  intentPause2 = new Intent();
                    intentPause2.setAction(MediaplayService.MusicPause);
                    getApplicationContext().sendBroadcast(intentPause2);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //当其他应用申请焦点之后又释放焦点会触发此回调
                    //可重新播放音乐
                    Log.d("Android_krcm110", "AUDIOFOCUS_GAIN");
                    Intent  intentResume = new Intent();
                    intentResume.setAction(MediaplayService.MusicResume);
                    getApplicationContext().sendBroadcast(intentResume);
                    break;
            }
        }
    };

    ///获取焦点
    private boolean requestFocus() {
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(mAudioFocusChange,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }


    /**
     * 获取播放器
     * @param playerId
     * @param mode
     * @return
     */
    private Player getPlayer(String playerId, String mode) {
       // createNotification();
        if (!AudioplayersPlugin.mediaPlayers.containsKey(playerId)) {
            Player player =
                    mode.equalsIgnoreCase("PlayerMode.MEDIA_PLAYER") ?
                            new NewWrappedMediaPlayer(this, playerId) :
                            new NewWrappedSoundPool(this, playerId);
            AudioplayersPlugin.mediaPlayers.put(playerId, player);
        }
        return AudioplayersPlugin.mediaPlayers.get(playerId);
    };


    /**
     * 开始播放音频
     * @param player
     */
    public void handleIsPlaying(Player player) {
        //startPositionUpdates();
        Intent intent = new Intent();
        intent.setAction(MusicIntentPlayerMusic);
        this.getApplication().sendBroadcast(intent);
    }

    public void handleDuration(Player player) {
        Intent intent = new Intent();
        intent.setAction(MusicHandleDuration);
        this.getApplication().sendBroadcast(intent);
    }

    public void handleCompletion(Player player) {
       Intent intent = new Intent();
        intent.setAction(MusichandleCompletion);
        this.getApplication().sendBroadcast(intent);
        //channel.invokeMethod("audio.onComplete", buildArguments(player.getPlayerId(), true));
    }

    public void handleSeekComplete(Player player) {
        Intent intent = new Intent();
        intent.setAction(MusichandleSeekComplete);
        this.getApplication().sendBroadcast(intent);
    }

    ///暂停播放
    public void handlePause(Player player){
        Intent intent = new Intent();
        intent.setAction(MusichandlePause);
        this.getApplication().sendBroadcast(intent);
    }

    ///恢复播放
    public void handleResume(Player player){
        Intent intent = new Intent();
        intent.setAction(MusichandleResume);
        this.getApplication().sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Kathy", "onStartCommand - startId = " + startId + ", Thread ID = " + Thread.currentThread().getId());
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("Kathy", "onBind - Thread ID = " + Thread.currentThread().getId());
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i("Kathy", "onDestroy - Thread ID = " + Thread.currentThread().getId());
        //super.onDestroy();
        //unregisterReceiver(broadcastReceiver);
    }


    /**
    private void createNotification(){
        builder = new NotificationBarMediaplayer(this);
        builder.createPostCustomNotification("播放音频","");
    }**/
}
