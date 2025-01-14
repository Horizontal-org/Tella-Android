package org.horizontal.tella.mobile.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.horizontal.tella.mobile.util.ThreadUtil;


public class AudioPlayer {
    private final Context context;
    private final WeakReference<Listener> listener;

    private MediaPlayer mediaPlayer;
    private MediaFileHttpServer mediaFileHttpServer;
    private ProgressEventHandler progressEventHandler;

    private int pausedTime = 0;

    public interface Listener {
        void onStart(int duration);

        void onStop();

        void onProgress(int currentPosition);
    }


    public AudioPlayer(@NonNull Context context, @NonNull Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = new WeakReference<>(listener);
        this.progressEventHandler = new ProgressEventHandler(this);
    }

    public void play(VaultFile vaultFile) {
        try {
            if (mediaPlayer != null) return; // there can be only one..

            mediaFileHttpServer = new MediaFileHttpServer(context, vaultFile);
            mediaFileHttpServer.start();

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, mediaFileHttpServer.getUri());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnPreparedListener(mp -> {
                synchronized (AudioPlayer.this) {
                    if (mediaPlayer == null) return;
                    mediaPlayer.start();
                    mediaPlayer.seekTo(pausedTime);
                }

                notifyOnStart(mediaPlayer.getDuration());
                progressEventHandler.sendEmptyMessage(0);
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                synchronized (AudioPlayer.this) {
                    if (mediaFileHttpServer != null) {
                        mediaFileHttpServer.stop();
                        mediaFileHttpServer = null;
                    }
                }

                notifyOnStop();
                progressEventHandler.removeMessages(0);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                synchronized (AudioPlayer.this) {
                    if (mediaFileHttpServer != null) {
                        mediaFileHttpServer.stop();
                        mediaFileHttpServer = null;
                    }
                }

                notifyOnStop();
                progressEventHandler.removeMessages(0);

                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ffwd(int delay) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + delay);
        }
    }

    public void rwd(int delay) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - delay);
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            pausedTime = getCurrentPosition();
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.seekTo(pausedTime);
            notifyOnStart(mediaPlayer.getDuration());
            progressEventHandler.sendEmptyMessage(0);
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (mediaFileHttpServer != null) {
            mediaFileHttpServer.stop();
            mediaFileHttpServer = null;
        }
    }

    @NonNull
    private Listener getListener() {
        Listener target = listener.get();

        if (target != null) {
            return target;
        } else {
            return new Listener() {
                @Override
                public void onStart(int duration) {
                }

                @Override
                public void onStop() {
                }

                @Override
                public void onProgress(int currentPosition) {
                }
            };
        }
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void notifyOnStart(final int duration) {
        ThreadUtil.runOnMain(() -> getListener().onStart(duration));
    }

    private void notifyOnStop() {
        ThreadUtil.runOnMain(() -> getListener().onStop());
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void notifyOnProgress(final int currentPosition) {
        ThreadUtil.runOnMain(() -> getListener().onProgress(currentPosition));
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private int getCurrentPosition() {
        if (mediaPlayer == null || mediaPlayer.getCurrentPosition() <= 0 || mediaPlayer.getDuration() <= 0) {
            return 0;
        } else {
            return mediaPlayer.getCurrentPosition();
        }
    }

    private static class ProgressEventHandler extends Handler {
        private final WeakReference<AudioPlayer> playerReference;

        private ProgressEventHandler(@NonNull AudioPlayer player) {
            this.playerReference = new WeakReference<>(player);
        }

        @Override
        public void handleMessage(Message msg) {
            AudioPlayer player = playerReference.get();

            if (player == null || player.mediaPlayer == null || !player.mediaPlayer.isPlaying()) {
                return;
            }

            int currentPosition = player.getCurrentPosition();
            player.notifyOnProgress(currentPosition);

            sendEmptyMessageDelayed(0, 50);
        }
    }
}
