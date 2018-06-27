package com.beyondsw.palette.edu.marker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import com.beyondsw.palette.DimenUtils;
import com.beyondsw.palette.edu.action.MyPath;

import java.util.LinkedList;

/**
 * Created by wangwu on 2018/6/26.
 * 主Canvas画点
 */
public class MySpotPath extends MyPath implements SpotFilter.Plotter {

    //校准首页笔的开发开关
    public static final boolean ASSUME_STYLUS_CALIBRATED = true;
    private static final int SMOOTHING_FILTER_WLEN = 6;
    private static final float SMOOTHING_FILTER_POS_DECAY = 0.65f;
    private static final float SMOOTHING_FILTER_PRESSURE_DECAY = 0.9f;
    private static final float INVALIDATE_PADDING = 4.0f;
    final float[] mTmpPoint = new float[2];
    final Rect tmpDirtyRect = new Rect();
    private final float mPressureExponent = 2.0f;
    //让点连续
    private final RectF tmpDirtyRectF = new RectF();
    private final RectF tmpRF = new RectF();
    int mInkDensity = 0xff; // set to 0x20 or so for a felt-tip look, 0xff for traditional Markers
    LinkedList<Spot> mSpots = new LinkedList<Spot>();
    float mLastX = 0, mLastY = 0, mLastLen = 0, mLastR = -1;
    private SpotFilter spotFilter;
    private PressureCooker mPressureCooker;
    //缩放比例  目前不缩放
    private float mPanX = 0f, mPanY = 0f;
    private float mRadiusMin;
    private float mRadiusMax;
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mTan[] = new float[2];


    Bitmap penBitmap;


    public MySpotPath(float x, float y, int paintColor, int paintSize, PressureCooker mPressureCooker, Bitmap bitmap) {
        super(x, y, paintColor, paintSize);
        spotFilter = new SpotFilter(SMOOTHING_FILTER_WLEN, SMOOTHING_FILTER_POS_DECAY, SMOOTHING_FILTER_PRESSURE_DECAY, this);
        this.mPressureCooker = mPressureCooker;
        //todo ww_ size挂钩mRadiusMin mRadiusMax  笔锋峰值
        mRadiusMin = DimenUtils.dp2px(2) * 0.5f;
        mRadiusMax = DimenUtils.dp2px(5) * 0.5f;

        mInkDensity = 0xff;
        setPenColor(color);
        //设置抗锯齿，一般设为true
        mPaint.setAntiAlias(true);
        //抖动
        mPaint.setDither(true);
        penBitmap = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),bitmap.getConfig());


    }


    /**
     *
     * @param color
     */
    public void setPenColor(int color) {
        if (color == 0) {
            // eraser: DST_OUT
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            mPaint.setColor(0XFF000000);
        } else {
            mPaint.setXfermode(null);
            mPaint.setColor(color); // or collor? or color & (mInkDensity << 24)?
            mPaint.setAlpha(mInkDensity);
            mPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        }
    }



    Spot finishTag = new Spot();
    //维持点
    protected void addNoCopy(Spot c) {
        mSpots.add(0, c);
    }

    @Override
    public void onPenDraw(Canvas canvas) {
        if (null != spotFilter ) {
            spotFilter.canvas = canvas;
            for (int i = mSpots.size() - 1; i >= 0; i--) {
                if(mSpots.get(i) == finishTag){
                    spotFilter.finish();
                    reset();
                    continue;
                }
                spotFilter.add(mSpots.get(i));
            }
        }

    }

    // TODO: 2018/6/27 ww_ 去掉super.onDraw(canvas);，不用画线
    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);//test
    }

    //ontouch 同步添加  但是要添加历史附近数据
    @Override
    public void add(Spot s, Canvas canvas) {
        spotFilter.canvas = canvas;
        Spot spot = spotFilter.add(s);
        addNoCopy(spot);
    }

    //ontouch 毛笔
    @Override
    public void finish() {
        spotFilter.finish();
        reset();
        addNoCopy(finishTag);
    }

    @Override
    public void plot(Spot s, Canvas canvas) {
        //test



        final float pressureNorm;
        if (null == canvas) {
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

        dirty(dirtyF);
    }

    private void dirty(RectF r) {
        r.roundOut(tmpDirtyRect);
        tmpDirtyRect.inset((int) -INVALIDATE_PADDING, (int) -INVALIDATE_PADDING);
        // TODO: 2018/6/26 ww_ 等
        //invalidate();
    }

    public RectF strokeTo(Canvas canvas, float x, float y, float r) {
        final RectF dirty = tmpDirtyRectF;
        dirty.setEmpty();

        if (mLastR < 0) {
            // always draw the first point
            drawStrokePoint(canvas, x, y, r, dirty);
        } else {
            // connect the dots, la-la-la

            mLastLen = dist(mLastX, mLastY, x, y);
            float xi, yi, ri, frac;
            float d = 0;
            while (true) {
                if (d > mLastLen) {
                    break;
                }
                frac = d == 0 ? 0 : (d / mLastLen);
                ri = lerp(mLastR, r, frac);
                xi = lerp(mLastX, x, frac);
                yi = lerp(mLastY, y, frac);
                drawStrokePoint(canvas, xi, yi, ri, dirty);

                // for very narrow lines we must step (not much more than) one radius at a time
                final float MIN = 1f;
                final float THRESH = 16f;
                final float SLOPE = 0.1f; // asymptote: the spacing will increase as SLOPE*x
                if (ri <= THRESH) {
                    d += MIN;
                } else {
                    d += Math.sqrt(SLOPE * Math.pow(ri - THRESH, 2) + MIN);
                }
            }

        }

        mLastX = x;
        mLastY = y;
        mLastR = r;

        return dirty;
    }

    public void reset() {
        mLastX = mLastY = mTan[0] = mTan[1] = 0;
        mLastR = -1;
    }

    final void drawStrokePoint(Canvas canvas, float x, float y, float r, RectF dirty) {
        canvas.drawCircle(x, y, r, mPaint);
        dirty.union(x - r, y - r, x + r, y + r);
    }


    public float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public float clamp(float a, float b, float f) {
        return f < a ? a : (f > b ? b : f);
    }


    final float dist(float x1, float y1, float x2, float y2) {
        x2 -= x1;
        y2 -= y1;
        return (float) Math.sqrt(x2 * x2 + y2 * y2);
    }
}
