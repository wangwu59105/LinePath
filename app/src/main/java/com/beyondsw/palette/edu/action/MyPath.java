package com.beyondsw.palette.edu.action;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;

/**
 * 路径
 * <p/>
 * Created by Administrator on 2015/6/24.
 */
public class MyPath extends Action {
    private Path path;
    private Paint paint;

    private Float mLastX ;
    private Float mLastY ;
    public MyPath(Float x, Float y, Integer color, Integer size) {
        super(x, y, color, size);
        path = new Path();
        path.moveTo(x, y);
        path.lineTo(x, y);

        mLastX = x;
        mLastY = y;
    }

    @Override
    public boolean isSequentialAction() {
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (canvas == null) {
            return;
        }

        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

        if (paint == null) {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(color);
            paint.setStrokeWidth(size);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
        canvas.drawPath(path, paint);
    }

    @Override
    public void onUP(Canvas canvas) {



    }

    @Override
    public void onMove(float mx, float my) {

        path.quadTo(mLastX, mLastY, (mx + mLastX) / 2, (my + mLastY) / 2);

        mLastX = mx;
        mLastY = my;
        //path.lineTo(mx, my);
    }
}
