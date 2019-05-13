package com.example.streamdelayer;
import android.media.AudioTrack;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

public class AudioPlayer {

    private Context context = null;
    private MediaCodec mDecoder = null;
    private MediaExtractor mExtractor = null;
    private HttpMediaSource mMediaSource = null;
    private MediaFormat mFormat = null;
    private RingBuffer mRingBuffer = null;

    public static float MAX_DELAY_SECONDS  = 60.0f;
    private boolean mOk = false;
    private boolean mPlay = false;
    private float mCurrentDelay = 0.0f;

    public AudioPlayer(final Context ctx, final HttpMediaSource mediaSource) throws Exception {
        context = ctx;
        mMediaSource = mediaSource;
        mExtractor = new MediaExtractor();
        mExtractor.setDataSource(mMediaSource);
        Log.d(MainActivity.TAG, "Set data source");
        mExtractor.selectTrack(0);
        mFormat = mExtractor.getTrackFormat(0);

        Log.d(MainActivity.TAG, "Format: " + mFormat.getString(MediaFormat.KEY_MIME));

        mDecoder = MediaCodec.createDecoderByType(mFormat.getString(MediaFormat.KEY_MIME));
        mDecoder.configure(
                MediaFormat.createAudioFormat(
                        mFormat.getString(MediaFormat.KEY_MIME),
                        mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)),
                null, null, 0);
        mRingBuffer = new RingBuffer((int)(1.01f*MAX_DELAY_SECONDS*mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE))*mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)*2, 0);

        mOk = true;
        mPlay = true;
    }

    public float getHeadPercentage() {return mRingBuffer.getHeadPercentage();}
    public float getTailPercentage() {return mRingBuffer.getTailPercentage();}

    public void stop() {mPlay = false;}

    public boolean ok() {return mOk;}

    public void setDelay(float delay) {
        mCurrentDelay = delay;
        long delayInSamples = (long)(delay*mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        long delayInBytes = (long)(2*delayInSamples*mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
        Log.d(MainActivity.TAG, "Setting delay to "+Float.toString(delay)+" seconds, "+Long.toString(delayInBytes)+" bytes");
        mRingBuffer.setHeadOffset(delayInBytes);
    }

    public float getDelay() {return mCurrentDelay;}

    public void play(){
        new Thread()
        {
            @Override
            public void run() {
                    try {
                        mDecoder.start();
                        int inputIndex = mDecoder.dequeueInputBuffer(-1);
                        ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        byte[] httpAudioBuffer = null;
                        byte[] delayedAudioBuffer = null;
                        AudioTrack audioTrack = null;

                        int read = mExtractor.readSampleData(inputBuffer, 0);
                        while (mPlay && read > 0) {
                            mDecoder.queueInputBuffer(inputIndex, 0, read, mExtractor.getSampleTime(), 0);

                            mExtractor.advance();

                            int outputIndex = mDecoder.dequeueOutputBuffer(bufferInfo, -1);
                            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                            } else if (outputIndex >= 0) {

                                if (bufferInfo.size > 0) {

                                    ByteBuffer outputBuffer = mDecoder.getOutputBuffer(outputIndex);
                                    if (httpAudioBuffer == null || httpAudioBuffer.length < bufferInfo.size) {
                                        httpAudioBuffer = new byte[bufferInfo.size];
                                        delayedAudioBuffer = new byte[bufferInfo.size];
                                    }

                                    outputBuffer.rewind();
                                    outputBuffer.get(httpAudioBuffer, 0, bufferInfo.size);
                                    mRingBuffer.add(httpAudioBuffer, bufferInfo.size);
                                    mRingBuffer.get(delayedAudioBuffer, bufferInfo.size);
                                    mDecoder.releaseOutputBuffer(outputIndex, false);

                                    if (audioTrack == null) {
                                        int bufferSize = mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)*mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)*2;
                                        /*int bufferSize = AudioTrack.getMinBufferSize(
                                                mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                                mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                                                AudioFormat.ENCODING_PCM_16BIT) * 2; */
                                        Log.d(MainActivity.TAG, "Buffer size is "+Integer.toString(bufferSize));

                                        audioTrack = new AudioTrack(
                                                AudioManager.STREAM_MUSIC,
                                                mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                                mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                                                AudioFormat.ENCODING_PCM_16BIT,
                                                bufferSize,
                                                AudioTrack.MODE_STREAM);

                                        audioTrack.play();
                                    }

                                    audioTrack.write(delayedAudioBuffer, 0, bufferInfo.size);
                                }
                            }

                            inputIndex = mDecoder.dequeueInputBuffer(-1);
                            inputBuffer = mDecoder.getInputBuffer(inputIndex);

                            read = mExtractor.readSampleData(inputBuffer, 0);
                        }
                    } catch (Exception e) {
                        mOk = false;
                        Log.d(MainActivity.TAG, e.getMessage());
                    }
            }
        }.start();
    }
}