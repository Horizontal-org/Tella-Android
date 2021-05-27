package rs.readahead.washington.mobile.media;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.VaultUtils;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.Util;
import timber.log.Timber;


@SuppressWarnings("MethodOnlyUsedFromInnerClass")
public class AudioRecorder {
    private Executor executor;
    private Context context;

    private boolean recording;
    private boolean cancelled;
    private boolean paused;
    private long startTime;
    private long pausedTime = 0L;
    private long duration = 0L;

    @Nullable
    private AudioRecordInterface caller;
    private static final long REFRESH_TIME_MS = 500;
    private long callTime;

    private static final String ENCODER_TYPE = "audio/mp4a-latm";
    private static final long kTimeoutUs = 10000;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = 1;
    private static final int BIT_RATE = 32000;

    public interface AudioRecordInterface {
        void onDurationUpdate(long duration);
    }


    public AudioRecorder(Context context, @Nullable AudioRecordInterface caller) {
        this.context = context.getApplicationContext();
        this.caller = caller;
        this.executor = Executors.newFixedThreadPool(1);
        recording = true;
        cancelled = false;
    }

    public Observable<VaultFile> startRecording() {
        return Observable.fromCallable(() -> {
            VaultFile vaultFile = VaultUtils.INSTANCE.newAac();

            DigestOutputStream outputStream = MediaFileHandler.getOutputStream(context, vaultFile);

            if (outputStream == null) {
                return null;
            }

            startTime = Util.currentTimestamp();
            encode(outputStream); // heigh-ho, heigh-ho..

            if (isCancelled()) {
                MediaFileHandler.deleteFile(context, vaultFile);
                return null;
            }

            vaultFile.size = MediaFileHandler.getSize(context, vaultFile);
            vaultFile.hash = StringUtils.hexString(outputStream.getMessageDigest().digest());
            vaultFile.duration = duration;

            return vaultFile;
        }).subscribeOn(Schedulers.from(executor)).observeOn(AndroidSchedulers.mainThread());
    }

    public synchronized void stopRecording() {
        paused = false;
        recording = false;
    }

    public synchronized void cancelRecording() {
        recording = false;
        cancelled = true;
    }

    public synchronized void pauseRecording() {
        pausedTime = Util.currentTimestamp();
        paused = true;
    }

    public synchronized void cancelPause() {
        startTime = startTime + (Util.currentTimestamp() - pausedTime);
        paused = false;
    }

    private void encode(OutputStream outputStream) throws IOException {
        AudioRecord audioRecord = null;
        MediaCodec codec = null;

        try {
            // get required buffer size
            int bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            // create AudioRecorder
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 16);

            // create MediaCodec
            codec = createMediaCodec(bufferSize);

            audioRecord.startRecording();
            codec.start();

            byte[] audioRecordData = new byte[bufferSize];
            ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
            ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            int index;

            while (true) {
                int audioRecordDataLength = audioRecord.read(audioRecordData, 0, audioRecordData.length);

                if (audioRecordDataLength < 0) {
                    throw new IOException();
                }

                if (isRunning()) {
                    index = codec.dequeueInputBuffer(kTimeoutUs);

                    if (index != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        ByteBuffer buffer = codecInputBuffers[index];
                        buffer.clear();
                        buffer.put(audioRecordData);

                        codec.queueInputBuffer(
                                index,
                                0 /* offset */,
                                audioRecordDataLength,
                                0 /* timeUs */,
                                isRecording() ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }

                    index = codec.dequeueOutputBuffer(bufferInfo, 0);

                    while (index != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (index >= 0) {
                            int outBitsSize = bufferInfo.size;
                            int outPacketSize = outBitsSize + 7; // 7 is ADTS size
                            ByteBuffer outBuf = codecOutputBuffers[index];

                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                outBuf.position(bufferInfo.offset);
                                outBuf.limit(bufferInfo.offset + outBitsSize);
                                try {
                                    byte[] data = new byte[outPacketSize];
                                    addADTStoPacket(data, outPacketSize);
                                    outBuf.get(data, 7, outBitsSize);
                                    outBuf.position(bufferInfo.offset);
                                    outputStream.write(data, 0, outPacketSize);
                                } catch (IOException e) {
                                    Timber.e(e, getClass().getName());
                                }
                            }
                            outBuf.clear();
                            codec.releaseOutputBuffer(index, false /* render */);
                        } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            codecOutputBuffers = codec.getOutputBuffers();
                        }

                        index = codec.dequeueOutputBuffer(bufferInfo, 0);
                    }

                    if (isRecording()) {
                        updateDuration();
                    } else {
                        break;
                    }
                }
            }
        } catch (IllegalStateException e) {
            Timber.d(e, getClass().getName());
            throw new IOException(e);
        } finally {
            try {
                if (codec != null) {
                    codec.stop();
                    codec.release();
                }

                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void updateDuration() {
        if (this.caller == null) {
            return;
        }

        long now = Util.currentTimestamp();

        if (now - callTime > REFRESH_TIME_MS) {
            callTime = now;

            duration = now - startTime;
            caller.onDurationUpdate(duration);
        }
    }

    private synchronized boolean isRunning() {
        return !paused;
    }

    private synchronized boolean isRecording() {
        return recording;
    }

    private synchronized boolean isCancelled() {
        return cancelled;
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1; // 0xF9?
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private MediaCodec createMediaCodec(int bufferSize) throws IOException {
        MediaCodec mediaCodec = MediaCodec.createEncoderByType(ENCODER_TYPE);
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, ENCODER_TYPE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNELS);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);

        mediaCodec.configure(
                format,
                null, /* surface */
                null, /* crypto */
                MediaCodec.CONFIGURE_FLAG_ENCODE);

        return mediaCodec;
    }
}
