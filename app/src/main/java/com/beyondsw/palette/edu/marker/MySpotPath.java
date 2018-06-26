package com.beyondsw.palette.edu.marker;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import com.beyondsw.palette.DimenUtils;
import com.beyondsw.palette.edu.action.MyPath;

/**
 * Created by wangwu on 2018/6/26.
 */
public class MySpotPath extends MyPath implements SpotFilter.Plotter {

    private static final int SMOOTHING_FILTER_WLEN = 6;
    private static final float SMOOTHING_FILTER_POS_DECAY = 0.65f;
    private static final float SMOOTHING_FILTER_PRESSURE_DECAY = 0.9f;
    private SpotFilter spotFilter;
    private PressureCooker mPressureCooker;

    private static final float INVALIDATE_PADDING = 4.0f;
    private final float mPressureExponent = 2.0f;
    //缩放比例  目前不缩放
    private float mPanX = 0f, mPanY = 0f;

    //校准首页笔的开发开关
    public static final boolean ASSUME_STYLUS_CALIBRATED = true;

    private float mRadiusMin;
    private float mRadiusMax;
    final float[] mTmpPoint = new float[2];
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    int mInkDensity = 0xff; // set to 0x20 or so for a felt-tip look, 0xff for traditional Markers




    public MySpotPath(Float x, Float y, Integer color, Integer size,PressureCooker mPressureCooker) {
        super(x, y, color, size);
        spotFilter = new SpotFilter(SMOOTHING_FILTER_WLEN, SMOOTHING_FILTER_POS_DECAY, SMOOTHING_FILTER_PRESSURE_DECAY, this);
        this.mPressureCooker = mPressureCooker;
        //todo ww_ size挂钩mRadiusMin mRadiusMax  笔锋峰值
        mRadiusMin = DimenUtils.dp2px(2) * 0.5f;
        mRadiusMax =  DimenUtils.dp2px(10) * 0.5f;

        mInkDensity = 0xff;
        mPaint.setColor(color); // or collor? or color & (mInkDensity << 24)?
        mPaint.setAlpha(mInkDensity);

//                mPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        mPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));

    }


    //ontouch 同步添加  但是要添加历史附近数据
    public void add(Spot s ,Canvas canvas) {
        spotFilter.canvas = canvas;
        spotFilter.add(s);

    }

    @Override
    public void plot(Spot s,Canvas canvas) {
        final float pressureNorm;

        if(null==canvas){
            return;
        }




        if (ASSUME_STYLUS_CALIBRATED && s.tool == MotionEvent.TOOL_TYPE_STYLUS) {
            pressureNorm = s.pressure;
        } else {
            pressureNorm = mPressureCooker.getAdjustedPressure(s.pressure);
        }
        Log.e("pressureNorm", s.tool + ";" + s.size + ";" + pressureNorm);
        final float radius = lerp(mRadiusMin, mRadiusMax,
                (float) Math.pow(pressureNorm, mPressureExponent));
        mTmpPoint[0] = s.x - mPanX;
        mTmpPoint[1] = s.y - mPanY;

        final RectF dirtyF = strokeTo(canvas,
                mTmpPoint[0],
                mTmpPoint[1], radius);
    }



    private final RectF tmpDirtyRectF = new RectF();
    public RectF strokeTo(Canvas canvas, float x, float y, float r) {
        final RectF dirty = tmpDirtyRectF;
        dirty.setEmpty();
        drawStrokePoint(canvas,x,y,r,dirty);
        return dirty;
    }


    private final RectF tmpRF = new RectF();
    final void drawStrokePoint(Canvas canvas, float x, float y, float r, RectF dirty) {
        canvas.drawCircle(x, y, r, mPaint);
        dirty.union(x-r, y-r, x+r, y+r);
    }



    public  float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public  float clamp(float a, float b, float f) {
        return f < a ? a : (f > b ? b : f);
    }
}
