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

    Context context = null;
    MediaCodec mDecoder = null;
    MediaExtractor mExtractor = null;
    HttpMediaSource mMediaSource = null;
    MediaFormat mFormat = null;
    RingBuffer mRingBuffer = null;
    String mUrl = null;

    public AudioPlayer(final Context ctx, final String url) {
        context = ctx;
        mUrl = url;
        play();
    }

    public void setDelay(float delay) {
        long delayInSamples = (long)(delay*mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        long delayInBytes = (long)(2*delayInSamples*mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
        Log.d(MainActivity.TAG, "Setting delay to "+Float.toString(delay)+" seconds, "+Long.toString(delayInBytes)+" bytes");
        mRingBuffer.setHeadOffset(delayInBytes);
    }

    private void play(){
        new Thread()
        {
            @Override
            public void run() {
                    try {
                        mExtractor = new MediaExtractor();
                        mMediaSource = new HttpMediaSource(mUrl);
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
                        mRingBuffer = new RingBuffer(2*10*mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)*mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT), 0);
                        mDecoder.start();
                        int inputIndex = mDecoder.dequeueInputBuffer(-1);
                        ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        byte[] httpAudioBuffer = null;
                        byte[] delayedAudioBuffer = null;
                        AudioTrack audioTrack = null;

                        int read = mExtractor.readSampleData(inputBuffer, 0);
                        while (read > 0) {
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
                        Log.d(MainActivity.TAG, e.getMessage());
                    }
            }
        }.start();
    }
}