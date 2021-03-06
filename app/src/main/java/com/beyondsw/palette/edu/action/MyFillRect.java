package com.beyondsw.palette.edu.action;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * 实心方块
 * <p/>
 * Created by huangjun on 2015/6/24.
 */
public class MyFillRect extends Action {
    public MyFillRect(Float x, Float y, Integer color, Integer size) {
        super(x, y, color, size);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setStrokeWidth(size);
        canvas.drawRect(startX, startY, stopX, stopY, paint);
    }

    @Override
    public void onMove(float mx, float my) {
        stopX = mx;
        stopY = my;
    }
}
