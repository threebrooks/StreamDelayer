package com.threebrooks.streamdelayer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.transition.Transition;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class AudioPlayer {

    private MediaCodec mDecoder = null;
    private MediaExtractor mExtractor = null;
    private HttpMediaSource mMediaSource = null;
    private MediaFormat mFormat = null;
    private RingBuffer mRingBuffer = null;

    public static float MAX_DELAY_SECONDS  = 10*60.0f;
    private static double SMOOTH_ALPHA = 0.5;
    private static double SMOOTH_GAMMA = 0.5;
    private boolean mOk = false;
    private boolean mPlay = false;

    WriteThread mWriteThread = null;
    PlayThread mPlayThread = null;

    TimeSmoother mWriteSmoother = null;
    TimeSmoother mReadSmoother = null;

    private int mBytesPerSecond = 0;
    private int mBytesPerSample = 0;

    Context mCtx  = null;

    public AudioPlayer(Context ctx, final HttpMediaSource mediaSource, RingBuffer ringBuffer) throws Exception {
        mCtx = ctx;
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
        mBytesPerSample = mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)*2;
        mBytesPerSecond = mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)*mBytesPerSample;
        mRingBuffer = ringBuffer;

        mOk = true;
        mPlay = true;
    }

    public float getHeadPercentage() {return mRingBuffer.getHeadPercentage();}
    public float getTailPercentage() {return mRingBuffer.getTailPercentage();}

    public void play(){
        mWriteThread = new WriteThread();
        mWriteThread.start();
        mPlayThread = new PlayThread();
        mPlayThread.start();
    }

    public void destroy() {
        mPlay = false;
        try {
            if (mWriteThread != null) mWriteThread.join();
            if (mPlayThread != null) mPlayThread.join();
            mRingBuffer = null;
            mDecoder = null;
        } catch (Exception e) {
            Log.d(MainActivity.TAG, e.getMessage());
        }
    }

    public boolean ok() {return mOk;}

    public void setAbsoluteDelay(double delay) {
        mRingBuffer.setHeadOffset(secondsToSampleRoundedBytes(delay));
        if (mReadSmoother != null) mReadSmoother.resetTo(mRingBuffer.getTailPos()/(double)mBytesPerSecond, 1.0);
    }

    public void addToDelay(float delay) {
        mRingBuffer.addToHeadOffset(secondsToSampleRoundedBytes(delay));
        if (mReadSmoother != null) mReadSmoother.resetTo(mRingBuffer.getTailPos()/(double)mBytesPerSecond, 1.0);
    }

    public long secondsToSampleRoundedBytes(double s) {
        long val = ((long)(s*mBytesPerSecond)/mBytesPerSample)*mBytesPerSample;
        return val;
    }

    public double getCurrentDelay() {
        if (mWriteSmoother == null || mReadSmoother == null) return 0.0;
        return mWriteSmoother.getCurrentVal()-mReadSmoother.getCurrentVal();
    }

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

                            //Log.d(MainActivity.TAG,"Read "+bufferInfo.size);

                            outputBuffer.rewind();
                            outputBuffer.get(httpAudioBuffer, 0, bufferInfo.size);
                            while (mRingBuffer.add(httpAudioBuffer, bufferInfo.size) == -1) {
                                Thread.sleep(100);
                            };
                            mDecoder.releaseOutputBuffer(outputIndex, false);

                            if (mWriteSmoother == null) {
                                mWriteSmoother = new TimeSmoother(SMOOTH_ALPHA, SMOOTH_GAMMA, mRingBuffer.getHeadPos()/(double)mBytesPerSecond, 1.0);
                            }
                            mWriteSmoother.addVal(mRingBuffer.getHeadPos()/(double)mBytesPerSecond);
                            //Log.d(MainActivity.TAG,"###1 "+(mRingBuffer.getHeadPos()/(double)mBytesPerSecond)+" "+mWriteSmoother.getCurrentVal());
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

                    //Log.d(MainActivity.TAG,"Reading from "+mRingBuffer.getTailPos());
                    int read = mRingBuffer.get(delayedAudioBuffer, chunkSize);
                    if (read == -1) {
                        Thread.sleep(100);
                        continue;
                    }

                    if (mReadSmoother == null) {
                        mReadSmoother = new TimeSmoother(SMOOTH_ALPHA, SMOOTH_GAMMA, mRingBuffer.getTailPos()/(double)mBytesPerSecond, 1.0);
                    }
                    mReadSmoother.addVal(mRingBuffer.getTailPos()/(double)mBytesPerSecond);

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

                    audioTrack.write(delayedAudioBuffer, 0, chunkSize);
                }
            } catch (Exception e) {
                mOk = false;
                Log.d(MainActivity.TAG, e.getMessage());
            }
        }
    }
}