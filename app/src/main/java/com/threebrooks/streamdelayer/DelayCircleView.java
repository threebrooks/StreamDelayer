package com.threebrooks.streamdelayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

public class DelayCircleView extends View {

    Context mCtx = null;
    PlayerService mPlayerService = null;

    RectF mArcRect = null;

    public DelayCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCtx = context;
    }

    public void setPlayerService(PlayerService sp) {
        mPlayerService = sp;
    }

    private final Rect textBounds = new Rect();
    public void drawTextCentered(Canvas canvas, Paint paint, String text, float cx, float cy){
        paint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, cx - textBounds.exactCenterX(), cy - textBounds.exactCenterY(), paint);
    }

    private String formatDelayString(double delay) {
        int minutes = (int)(delay/60.0);
        double seconds = delay-60.0*minutes;
        String s = "Delay: ";
        if (minutes > 0) s += minutes+"m";
        s += String.format("%.1f",seconds)+"s";
        return s;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radius = 0.9f*Math.min(canvas.getWidth()/2, canvas.getHeight()/2);

        Paint p = new Paint();
        p.setColor(ContextCompat.getColor(mCtx, R.color.purple200Grey));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(radius/7.0f);
        canvas.drawCircle(getWidth()/2.0f, getHeight()/2.0f, radius, p);

        if (mArcRect == null) {
            mArcRect = new RectF(getWidth()/2.0f-radius, getHeight()/2.0f-radius, getWidth()/2.0f+radius, getHeight()/2.0f+radius);
        }

        AudioPlayer audioPlayer = mPlayerService.mPlayer;
        if (audioPlayer != null) {
            p.setColor(ContextCompat.getColor(mCtx, R.color.purple200));
            float headPerc = audioPlayer.getHeadPercentage();
            float tailPerc = audioPlayer.getTailPercentage();
            float sweepPerc = Math.max(0.01f,(headPerc - tailPerc+1.0f) % 1.0f);
            canvas.drawArc(
                    mArcRect,
                    360.0f * tailPerc-90.0f,
                    360.0f *sweepPerc,
                    false,
                    p);

            String delayText = formatDelayString(audioPlayer.getCurrentDelay());
            p.setColor(ContextCompat.getColor(mCtx, R.color.fontPrimary));
            p.setTextSize(100.0f);
            p.setStyle(Paint.Style.FILL);
            drawTextCentered(canvas, p, delayText, getWidth()/2.0f, getHeight()/2.0f);
        }

        if (mPlayerService != null) {
            String statusText = mPlayerService.getStatus();
            p.setColor(ContextCompat.getColor(mCtx, R.color.fontPrimary));
            p.setTextSize(50.0f);
            p.setStyle(Paint.Style.FILL);
            drawTextCentered(canvas, p, statusText, getWidth()/2.0f, 3.0f*getHeight()/4.0f);
        }

        postInvalidateDelayed(500);
    }
}
