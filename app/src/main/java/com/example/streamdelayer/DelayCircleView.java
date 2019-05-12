package com.example.streamdelayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

public class DelayCircleView extends View {

    Context mCtx = null;
    AudioPlayer mAudioPlayer = null;

    RectF mArcRect = null;

    public DelayCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCtx = context;
    }

    public void setAudioPlayer(AudioPlayer ap) {
        mAudioPlayer = ap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radius = 0.9f*Math.min(canvas.getWidth()/2, canvas.getHeight()/2);

        Paint p = new Paint();
        p.setColor(ContextCompat.getColor(mCtx, R.color.l2Whiten));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(radius/10.0f);
        canvas.drawCircle(getWidth()/2.0f, getHeight()/2.0f, radius, p);

        if (mArcRect == null) {
            mArcRect = new RectF(getWidth()/2.0f-radius, getHeight()/2.0f-radius, getWidth()/2.0f+radius, getHeight()/2.0f+radius);
        }

        if (mAudioPlayer != null) {
            p.setColor(ContextCompat.getColor(mCtx, R.color.ringAudio));
            float headPerc = mAudioPlayer.getHeadPercentage();
            float tailPerc = mAudioPlayer.getTailPercentage();
            float sweepPerc = Math.max(0.01f,(headPerc - tailPerc+1.0f) % 1.0f);
            canvas.drawArc(
                    mArcRect,
                    360.0f * tailPerc,
                    360.0f *sweepPerc,
                    false,
                    p);

            p.setColor(ContextCompat.getColor(mCtx, R.color.fontPrimary));
            p.setTextSize(100.0f);
            p.setStyle(Paint.Style.FILL);
            String delayText = "Delay: "+mAudioPlayer.getDelay()+"s";
            canvas.drawText(delayText, getWidth()/2.0f-p.measureText(delayText)/2, getHeight()/2.0f, p);

        }

        postInvalidateDelayed(500);
    }
}
