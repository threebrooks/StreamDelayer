package com.threebrooks.streamdelayer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public class AudioPlayer {

    private MediaCodec mDecoder = null;
    private MediaExtractor mExtractor = null;
    private HttpMediaSource mMediaSource = null;
    private MediaFormat mFormat = null;
    private RingBuffer mRingBuffer = null;

    public static float MAX_DELAY_SECONDS  = 60.0f;
    public static float MIN_DELAY_SECONDS  = 2.0f;
    public static float DELAY_RESOLUTION = 0.5f;
    public static float MAX_DRIFT  = 1.0f;
    private boolean mOk = false;
    private boolean mPlay = false;
    private float mTargetDelay = 0.0f;

    float mSmoothedDrift = 0.0f;
    float mSmoothFac = 0.9f;

    private int mBytesPerSecond = 0;

    public AudioPlayer(final HttpMediaSource mediaSource) throws Exception {
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
        mBytesPerSecond = mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)*mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)*2;
        mRingBuffer = new RingBuffer((int)(1.01f*MAX_DELAY_SECONDS*mBytesPerSecond), 0);

        mOk = true;
        mPlay = true;
    }

    public float getHeadPercentage() {return mRingBuffer.getHeadPercentage();}
    public float getTailPercentage() {return mRingBuffer.getTailPercentage();}

    public void stop() {mPlay = false;}

    public boolean ok() {return mOk;}

    public static float roundToFraction(float x, float resolution) {
        return Math.round(x / resolution) * resolution;
    }

    public void setTargetDelay(float delay) {
        mTargetDelay = Math.max(MIN_DELAY_SECONDS, Math.min(MAX_DELAY_SECONDS, delay));
        mTargetDelay = roundToFraction(mTargetDelay, DELAY_RESOLUTION);
        long delayInSamples = (long)(delay*mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        long delayInBytes = (2*delayInSamples*mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
        mRingBuffer.setHeadOffset(delayInBytes);
    }


    public float getTargetDelay() {return mTargetDelay;}

    private float getEffectiveDelay() {return (mRingBuffer.getHeadPos()-mRingBuffer.getTailPos())/(float)(mBytesPerSecond);}

    public float getDrift() {return mSmoothedDrift;}

    class WriteThread extends Thread {
        @Override
        public void run() {
            try {
                mDecoder.start();
                int inputIndex = mDecoder.dequeueInputBuffer(-1);
                ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                byte[] httpAudioBuffer = null;

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
                            }

                            Log.d(MainActivity.TAG,"Read "+bufferInfo.size);

                            outputBuffer.rewind();
                            outputBuffer.get(httpAudioBuffer, 0, bufferInfo.size);
                            mRingBuffer.add(httpAudioBuffer, bufferInfo.size);
                            mDecoder.releaseOutputBuffer(outputIndex, false);
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
    }

    class PlayThread extends Thread {
        @Override
        public void run() {
            try {
                byte[] delayedAudioBuffer = null;
                AudioTrack audioTrack = null;
                int chunkSize = mBytesPerSecond/4; // Quarter of a second

                while (mPlay) {
                    if (delayedAudioBuffer == null || delayedAudioBuffer.length < chunkSize) {
                        delayedAudioBuffer = new byte[chunkSize];
                    }

                    int read = mRingBuffer.get(delayedAudioBuffer, chunkSize);
                    if (read == -1) {
                        Thread.sleep(100);
                        continue;
                    }

                    if (audioTrack == null) {
                        audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                chunkSize,
                                AudioTrack.MODE_STREAM);

                        audioTrack.play();
                    }

                    Log.d(MainActivity.TAG,"Wrote "+chunkSize);

                    audioTrack.write(delayedAudioBuffer, 0, chunkSize);

                    float drift = getEffectiveDelay()-getTargetDelay();
                    mSmoothedDrift = mSmoothFac*mSmoothedDrift+(1.0f-mSmoothFac)*drift;
                    if (Math.abs(mSmoothedDrift) > MAX_DRIFT) {
                        setTargetDelay(getTargetDelay());
                    }
                }
            } catch (Exception e) {
                mOk = false;
                Log.d(MainActivity.TAG, e.getMessage());
            }
        }
    }

    public void play(){
        new WriteThread().start();
        new PlayThread().start();
    }
}