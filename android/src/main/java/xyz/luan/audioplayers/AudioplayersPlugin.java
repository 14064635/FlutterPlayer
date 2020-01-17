package xyz.luan.audioplayers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.os.Build;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class AudioplayersPlugin implements MethodCallHandler {

    private static final Logger LOGGER = Logger.getLogger(AudioplayersPlugin.class.getCanonicalName());

    private final MethodChannel channel;
    public static final Map<String, Player> mediaPlayers = new HashMap<>();
    private final Handler handler = new Handler();
    private Runnable positionUpdates;
    private final Context context;
    private boolean seekFinish;

    ///是否是第一次加载
    //private boolean fristLoaded = false;

    public static void registerWith(final Registrar registrar) {
        ///获取和Flutter的原生通道进行和Flutter的交互
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "xyz.luan/audioplayers");
        channel.setMethodCallHandler(new AudioplayersPlugin(channel, registrar.activeContext()));
    }

    private AudioplayersPlugin(final MethodChannel channel, Context context) {
        this.channel = channel;
        this.channel.setMethodCallHandler(this);
        this.context = context;
        this.seekFinish = false;
    }


    @Override
    public void onMethodCall(final MethodCall call, final MethodChannel.Result response) {
        try {
            handleMethodCall(call, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error!", e);
            response.error("Unexpected error!", e.getMessage(), e);
        }
    }


    /**
     * 注册广播事件实现播放器的控制
     */
    private void registerServiceIntent(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MediaplayService.MusicIntentPlayerMusic);
        intentFilter.addAction(MediaplayService.MusicHandleDuration);
        intentFilter.addAction(MediaplayService.NotificationPause);
        intentFilter.addAction(MediaplayService.NotificationPlay);
        intentFilter.addAction(MediaplayService.NotificationSwitchPlay);
        intentFilter.addAction(MediaplayService.NotificationClose);
        intentFilter.addAction(MediaplayService.MusichandleCompletion);
        intentFilter.addAction(MediaplayService.MusichandleResume);
        intentFilter.addAction(MediaplayService.MusichandlePause);
        context.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * 接受广播消息并且控制显示
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MediaplayService.MusicIntentPlayerMusic)){
                final Player player = getPlayer(playerId, mode);
                handleIsPlaying(player);
            }else if(intent.getAction().equals(MediaplayService.MusicHandleDuration)){
                final Player player = getPlayer(playerId, mode);
                handleDuration(player);
            }
            else if(intent.getAction().equals(MediaplayService.MusichandleResume)){
                final Player player = getPlayer(playerId, mode);
                handleResume(player);
            }
            else if(intent.getAction().equals(MediaplayService.MusichandlePause)){
                final Player player = getPlayer(playerId, mode);
                handlePause(player);
            }
            else if(intent.getAction().equals(MediaplayService.MusichandleCompletion)){
                final Player player = getPlayer(playerId, mode);
                handleCompletion(player);
            }else if(intent.getAction().equals(MediaplayService.MusichandleSeekComplete)){
                final Player player = getPlayer(playerId, mode);
                handleSeekComplete(player);
            }else if(intent.getAction().equals(MediaplayService.NotificationPlay)){
                Intent  intentResume = new Intent();
                intentResume.setAction(MediaplayService.MusicResume);
                context.getApplicationContext().sendBroadcast(intentResume);
                final Player player = getPlayer(playerId, mode);
                handleIsPlaying(player);
            }else if(intent.getAction().equals(MediaplayService.NotificationClose)){
                Intent  intentPause = new Intent();
                intentPause.setAction(MediaplayService.MusicPause);
                intentPause.putExtra("closed",true);
                context.getApplicationContext().sendBroadcast(intentPause);
            }else if(intent.getAction().equals(MediaplayService.NotificationSwitchPlay)){
                final Player player = getPlayer(playerId, mode);
                if(player.isActuallyPlaying()){
                    Intent  intentPause = new Intent();
                    intentPause.setAction(MediaplayService.MusicPause);
                    context.getApplicationContext().sendBroadcast(intentPause);
                }else{
                    Intent  intentResume = new Intent();
                    intentResume.setAction(MediaplayService.MusicResume);
                    context.getApplicationContext().sendBroadcast(intentResume);
                    handleIsPlaying(player);
                }
            }
        }
    };

    String playerId;
    String mode;
    private void handleMethodCall(final MethodCall call, final MethodChannel.Result response) {
        playerId = call.argument("playerId");
        mode = call.argument("mode");
       //final Player player = getPlayer(playerId, mode);

        //if(!fristLoaded){
            //启动播放线程
            Intent intentOne = new Intent(context.getApplicationContext(), MediaplayService.class);
            registerServiceIntent();

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.getApplicationContext().startForegroundService(intentOne);
            }else{
                context.getApplicationContext().startService(intentOne);
            }
        //}
        //获取播放器
        Intent intentGet = new Intent();
        intentGet.setAction(MediaplayService.IntentGetPlayerMusic);
        intentGet.putExtra("playerId",playerId);
        intentGet.putExtra("mode",mode);
        context.getApplicationContext().sendBroadcast(intentGet);

        switch (call.method){
            case "play": {
                final String url = call.argument("url");
                final double volume = call.argument("volume");
                final Integer position = call.argument("position");
                final boolean respectSilence = call.argument("respectSilence");
                final boolean isLocal = call.argument("isLocal");
                final boolean stayAwake = call.argument("stayAwake");
                final String title =  call.argument("title");

                Intent  intentPlay = new Intent();
                intentPlay.setAction(MediaplayService.IntentPlayStatus);

                intentPlay.putExtra("url",url);
                intentPlay.putExtra("volume",volume);
                intentPlay.putExtra("position",position);
                intentPlay.putExtra("respectSilence",respectSilence);
                intentPlay.putExtra("isLocal",isLocal);
                intentPlay.putExtra("stayAwake",stayAwake);
                intentPlay.putExtra("mode",mode);
                intentPlay.putExtra("title",title);
                intentPlay.putExtra("playerId",playerId);
                context.getApplicationContext().sendBroadcast(intentPlay);
                break;
            }
            case "resume": {
                Intent  intentResume = new Intent();
                intentResume.setAction(MediaplayService.MusicResume);
                context.getApplicationContext().sendBroadcast(intentResume);
                break;
            }
            case "pause": {
                Intent  intentPause = new Intent();
                intentPause.setAction(MediaplayService.MusicPause);
                context.getApplicationContext().sendBroadcast(intentPause);
                break;
            }
            case "stop": {
                Intent  intentStop = new Intent();
                intentStop.setAction(MediaplayService.MusicStop);
                context.getApplicationContext().sendBroadcast(intentStop);
                break;
            }
            case "release": {
                Intent  intentRelease = new Intent();
                intentRelease.setAction(MediaplayService.MusicRelease);
                context.getApplicationContext().sendBroadcast(intentRelease);
                break;
            }
            case "seek": {
                Intent  intentSeek = new Intent();
                intentSeek.setAction(MediaplayService.MusicSeek);
                final Integer position = call.argument("position");
                intentSeek.putExtra("position",position);
                context.getApplicationContext().sendBroadcast(intentSeek);
                break;
            }
            case "setVolume": {
                Intent  intentSetVolume = new Intent();
                final double volume = call.argument("volume");
                intentSetVolume.putExtra("volume",volume);
                intentSetVolume.setAction(MediaplayService.MusicSetVolume);
                context.getApplicationContext().sendBroadcast(intentSetVolume);
                break;
            }
            case "setUrl": {
                Intent  intentSetUrl = new Intent();
                intentSetUrl.setAction(MediaplayService.MusicUrl);
                final String url = call.argument("url");
                intentSetUrl.putExtra("url",url);

                final boolean isLocal = call.argument("isLocal");
                intentSetUrl.putExtra("isLocal",isLocal);

                context.getApplicationContext().sendBroadcast(intentSetUrl);
                break;
            }
            case "getDuration": {
                Intent  intentDuration = new Intent();
                intentDuration.setAction(MediaplayService.MusicGetDuration);
                context.getApplicationContext().sendBroadcast(intentDuration);
                response.success(getPlayer(playerId, mode).getDuration());
                return;
            }
            case "getCurrentPosition": {
                Intent  intentCurrentPosition = new Intent();
                intentCurrentPosition.setAction(MediaplayService.MusicCurrentPosition);
                context.getApplicationContext().sendBroadcast(intentCurrentPosition);

                response.success(getPlayer(playerId, mode).getCurrentPosition());
                return;
            }
            case "setReleaseMode": {
                Intent  intentSetReleaseMode = new Intent();
                intentSetReleaseMode.setAction(MediaplayService.MusicSetReleaseMode);
                final String releaseModeName = call.argument("releaseMode");
                intentSetReleaseMode.putExtra("releaseMode",releaseModeName);
                context.getApplicationContext().sendBroadcast(intentSetReleaseMode);
                break;
            }
            case "setPlaybackRate":{
                Intent  intentChangeSpeed = new Intent();
                intentChangeSpeed.setAction(MediaplayService.MusicSpeed);
                final double speedModel = call.argument("playbackRate");
                intentChangeSpeed.putExtra("playbackRate",speedModel);
                context.getApplicationContext().sendBroadcast(intentChangeSpeed);
                break;
            }
            default: {
                Intent  intentModel = new Intent();
                intentModel.setAction(MediaplayService.MusicDefault);
                context.getApplicationContext().sendBroadcast(intentModel);
                response.notImplemented();
                return;
            }
        }



        /**
        switch (call.method) {
            case "play": {
                final String url = call.argument("url");
                final double volume = call.argument("volume");
                final Integer position = call.argument("position");
                final boolean respectSilence = call.argument("respectSilence");
                final boolean isLocal = call.argument("isLocal");
                final boolean stayAwake = call.argument("stayAwake");
                player.configAttributes(respectSilence, stayAwake, context.getApplicationContext());
                player.setVolume(volume);
                player.setUrl(url, isLocal);
                if (position != null && !mode.equals("PlayerMode.LOW_LATENCY")) {
                    player.seek(position);
                }
                player.play();

                Intent intent = new Intent();
                intent.setAction(MediaplayService.IntentPlayStatus);
                context.getApplicationContext().sendBroadcast(intent);
                break;
            }
            case "resume": {
                player.play();
                break;
            }
            case "pause": {
                player.pause();
                Intent intent = new Intent();
                intent.setAction(MediaplayService.IntentPauseStatus);
                context.getApplicationContext().sendBroadcast(intent);
                break;
            }
            case "stop": {
                player.stop();
                break;
            }
            case "release": {
                player.release();
                break;
            }
            case "seek": {
                final Integer position = call.argument("position");
                player.seek(position);
                break;
            }
            case "setVolume": {
                final double volume = call.argument("volume");
                player.setVolume(volume);
                break;
            }
            case "setUrl": {
                final String url = call.argument("url");
                final boolean isLocal = call.argument("isLocal");
                player.setUrl(url, isLocal);
                break;
            }
            case "getDuration": {

                response.success(player.getDuration());
                return;
            }
            case "getCurrentPosition": {
                response.success(player.getCurrentPosition());
                return;
            }
            case "setReleaseMode": {
                final String releaseModeName = call.argument("releaseMode");
                final ReleaseMode releaseMode = ReleaseMode.valueOf(releaseModeName.substring("ReleaseMode.".length()));
                player.setReleaseMode(releaseMode);
                break;
            }
            default: {
                response.notImplemented();
                return;
            }
        }**/

        response.success(1);
    }

    /**
     * 获取播放器
     * @param playerId
     * @param mode
     * @return
     */

    private Player getPlayer(String playerId, String mode) {
        /**
        if (!mediaPlayers.containsKey(playerId)) {
            Player player =
                    mode.equalsIgnoreCase("PlayerMode.MEDIA_PLAYER") ?
                            new WrappedMediaPlayer(this, playerId) :
                            new WrappedSoundPool(this, playerId);
            mediaPlayers.put(playerId, player);
        }**/
        return mediaPlayers.get(playerId);
    }

    public void handleIsPlaying(Player player) {
        startPositionUpdates();
    }

    ///暂停播放
    public void handlePause(Player player){
        channel.invokeMethod("audio.pause", buildArguments(player.getPlayerId(), player.getDuration()));
    }

    ///恢复播放
    public void handleResume(Player player){
        channel.invokeMethod("audio.resume", buildArguments(player.getPlayerId(), player.getDuration()));
    }

    public void handleDuration(Player player) {
        channel.invokeMethod("audio.onDuration", buildArguments(player.getPlayerId(), player.getDuration()));
    }

    public void handleCompletion(Player player) {
        channel.invokeMethod("audio.onComplete", buildArguments(player.getPlayerId(), true));
    }

    public void handleSeekComplete(Player player) {
        this.seekFinish = true;
    }

    private void startPositionUpdates() {
        if (positionUpdates != null) {
            return;
        }
        positionUpdates = new UpdateCallback(mediaPlayers, channel, handler, this);
        handler.post(positionUpdates);
    }

    private void stopPositionUpdates() {
        positionUpdates = null;
        handler.removeCallbacksAndMessages(null);
    }

    private static Map<String, Object> buildArguments(String playerId, Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put("playerId", playerId);
        result.put("value", value);
        return result;
    }

    private static final class UpdateCallback implements Runnable {

        private final WeakReference<Map<String, Player>> mediaPlayers;
        private final WeakReference<MethodChannel> channel;
        private final WeakReference<Handler> handler;
        private final WeakReference<AudioplayersPlugin> audioplayersPlugin;

        private UpdateCallback(final Map<String, Player> mediaPlayers,
                               final MethodChannel channel,
                               final Handler handler,
                               final AudioplayersPlugin audioplayersPlugin) {
            this.mediaPlayers = new WeakReference<>(mediaPlayers);
            this.channel = new WeakReference<>(channel);
            this.handler = new WeakReference<>(handler);
            this.audioplayersPlugin = new WeakReference<>(audioplayersPlugin);
        }

        @Override
        public void run() {
            final Map<String, Player> mediaPlayers = this.mediaPlayers.get();
            final MethodChannel channel = this.channel.get();
            final Handler handler = this.handler.get();
            final AudioplayersPlugin audioplayersPlugin = this.audioplayersPlugin.get();

            if (mediaPlayers == null || channel == null || handler == null || audioplayersPlugin == null) {
                if (audioplayersPlugin != null) {
                    audioplayersPlugin.stopPositionUpdates();
                }
                return;
            }

            boolean nonePlaying = true;
            for (Player player : mediaPlayers.values()) {
                if (!player.isActuallyPlaying()) {
                    continue;
                }
                try {
                    nonePlaying = false;
                    final String key = player.getPlayerId();
                    final int duration = player.getDuration();
                    final int time = player.getCurrentPosition();
                    channel.invokeMethod("audio.onDuration", buildArguments(key, duration));
                    channel.invokeMethod("audio.onCurrentPosition", buildArguments(key, time));
                    if (audioplayersPlugin.seekFinish) {
                        channel.invokeMethod("audio.onSeekComplete", buildArguments(player.getPlayerId(), true));
                        audioplayersPlugin.seekFinish = false;
                    }
                } catch (UnsupportedOperationException e) {

                }
            }

            if (nonePlaying) {
                audioplayersPlugin.stopPositionUpdates();
            } else {
                handler.postDelayed(this, 200);
            }
        }
    }
}

