package com.beyondsw.palette.edu.action;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.beyondsw.palette.DimenUtils;

/**
 * 橡皮擦（与画布背景色相同的Path）
 * <p/>
 * Created by Administrator on 2015/6/24.
 */
public class MyEraser extends Action {
    private Path path;

    public MyEraser(Float x, Float y, Integer color, Integer size) {
        super(x, y, color, size);
        path = new Path();
        path.moveTo(x, y);
        path.lineTo(x, y);
    }

    @Override
    public boolean isSequentialAction() {
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {





        Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFilterBitmap(true);
        //设置抗锯齿，一般设为true
        mPaint.setAntiAlias(true);
        //抖动
        mPaint.setDither(true);

        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(30);
        mPaint.setColor(0XFF000000);
        PorterDuffXfermode mXferModeClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        mPaint.setXfermode(mXferModeClear);



 //       Paint mEraserPaint = new Paint();
//        mEraserPaint.setAlpha(0);
//        //这个属性是设置paint为橡皮擦重中之重
//        //这是重点
//        //下面这句代码是橡皮擦设置的重点
//        mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
//        //上面这句代码是橡皮擦设置的重点（重要的事是不是一定要说三遍）
//        mEraserPaint.setAntiAlias(true);
//        mEraserPaint.setDither(true);
//        mEraserPaint.setStyle(Paint.Style.STROKE);
//        mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
//        mEraserPaint.setStrokeCap(Paint.Cap.ROUND);
//        mEraserPaint.setStrokeWidth(30);
        canvas.drawPath(path, mPaint);
    }

    @Override
    public void onMove(float mx, float my) {
        path.lineTo(mx, my);
    }
}
