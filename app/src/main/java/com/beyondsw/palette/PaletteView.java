package com.beyondsw.palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.beyondsw.palette.edu.DoodleChannel;
import com.beyondsw.palette.edu.Transaction;
import com.beyondsw.palette.edu.action.Action;
import com.beyondsw.palette.edu.action.MyPath;
import com.beyondsw.palette.edu.marker.PressureCooker;
import com.beyondsw.palette.manager.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wensefu on 17-3-21.
 */
public class PaletteView extends View {

    private float mLastX;
    private float mLastY;
    private Bitmap mBufferBitmap;
    private Canvas mBufferCanvas;
    private Callback mCallback;

    private float paintOffsetY = 0.0f; // 绘制时的Y偏移（去掉ActionBar,StatusBar,marginTop等高度）
    private float paintOffsetX = 0.0f; // 绘制事的X偏移（去掉marginLeft的宽度）
    private DoodleChannel paintChannel; // 绘图通道，自己本人使用
    private float xZoom = 1.0f; // 收发数据时缩放倍数（归一化）
    private float yZoom = 1.0f;
    // <account, transactions> 可以用于互动交互同步数据
    private Map<String, List<Transaction>> userDataMap = new HashMap<>();


    //毛笔
    PressureCooker mPressureCooker;

    public PaletteView(Context context) {
        super(context);
        init();
    }

    public PaletteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PaletteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 设置绘制时画笔的偏移
     *
     * @param x DoodleView的MarginLeft的宽度
     * @param y ActionBar与StatusBar及DoodleView的MarginTop的高度的和
     */
    public void setPaintOffset(float x, float y) {
        this.paintOffsetX = x;
        this.paintOffsetY = y;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    private void init() {
        setDrawingCacheEnabled(true);



        //ww
        this.paintChannel = new DoodleChannel();
        mPressureCooker = new PressureCooker(getContext());


    }

    private void initBuffer() {
        mBufferBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);
    }





    /**
     * 是否可以返回
     * @return
     */
    public boolean canUndo() {

        if (paintChannel == null) {
            return false;
        }
        return paintChannel.actions != null && paintChannel.actions.size() > 0;
    }


    /**
     * 撤销一步
     * 返回
     *
     * @return 撤销是否成功
     */
    public boolean paintBack() {
        if (paintChannel == null) {
            return false;
        }
        boolean res = back(UserInfo.getAccount(), true);
        //transactionManager.sendRevokeTransaction(); 同步发送
        return res;
    }


    /**
     * 删除自己和别人的笔迹
     *
     * @param account
     * @param isPaintView true 自己的 目前版本先维持一个通道
     * @return
     */
    private boolean back(String account, boolean isPaintView) {
        DoodleChannel channel = paintChannel;// playbackChannelMap.get(account);
        if (channel == null) {
            return false;
        }

        if (channel.actions != null && channel.actions.size() > 0) {
            channel.actions.remove(channel.actions.size() - 1);
            saveUserData(account, null, true, false, false);
            if (mBufferCanvas == null) {
                return false;
            }
            drawHistoryActions(mBufferCanvas);


            if (mCallback != null) {
                mCallback.onUndoRedoStatusChanged();
            }
            return true;
        }
        return false;
    }


    private void drawHistoryActions(Canvas canvas) {
        if (canvas == null) {
            return;
        }

        //绘制图片、图标、别人的笔迹....

        // 绘制所有历史Action
        if (paintChannel != null && paintChannel.actions != null) {
            mBufferBitmap.eraseColor(Color.TRANSPARENT);

            for (Action a : paintChannel.actions) {
                a.onDraw(canvas);
            }
            // 绘制当前
            if (paintChannel.action != null) {
                paintChannel.action.onDraw(canvas);
            }
            invalidate();
        }


    }


    /**
     * 清空画板
     */
    public void clearAll() {
        saveUserData(UserInfo.getAccount(), null, false, true, false);
        // clear 回放的所有频道

        // clear 自己画的频道
        clear(paintChannel, true);
    }


    /**
     * 清空自己的和别人的点
     *
     * @param playbackChannel
     * @param isPaintView
     */
    private void clear(DoodleChannel playbackChannel, boolean isPaintView) {
        DoodleChannel channel = paintChannel;
        if (channel == null) {
            return;
        }
        if (channel.actions != null) {
            channel.actions.clear();
        }
        channel.action = null;
        if (mBufferCanvas == null) {
            return;
        }
        drawHistoryActions(mBufferCanvas);
        if (mCallback != null) {
            mCallback.onUndoRedoStatusChanged();
        }
    }




    public Bitmap buildBitmap() {
        Bitmap bm = getDrawingCache();
        Bitmap result = Bitmap.createBitmap(bm);
        destroyDrawingCache();
        return result;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBufferBitmap != null) {
            canvas.save();
            canvas.drawBitmap(mBufferBitmap, 0, 0, null);
            canvas.restore();
        }
    }

    @SuppressWarnings("all")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        int eAction = event.getAction();
        if (eAction == MotionEvent.ACTION_CANCEL) {
            return false;
        }
        final int action = eAction & MotionEvent.ACTION_MASK;
        float touchX = event.getX(0);
        float touchY = event.getY(0);
        float size = event.getSize(0);//：指压范围
        float pressure = event.getPressure(0);//： 压力值  0-1 便于计算DimenUtils.MAXFORCE 255的时候


        int N = event.getHistorySize();
        long time = event.getEventTime();




        switch (action) {
            case MotionEvent.ACTION_DOWN:
                onPaintActionStart(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mBufferBitmap == null) {
                    initBuffer();
                }
                onPaintActionMove(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                onPaintActionEnd(touchX, touchY);
                break;
            default:
                break;
        }


        return true;
    }


    //ww

    private void onPaintActionStart(float x, float y) {
        if (paintChannel == null) {
            return;
        }

        onActionStart(x, y);
        //发送实时数据
        //transactionManager.sendStartTransaction(x / xZoom, y / yZoom, paintChannel.paintColor);
        //保存数据
        saveUserData(UserInfo.getAccount(), new Transaction(Transaction.ActionStep.START, x / xZoom, y / yZoom, paintChannel.paintColor), false, false, false);
    }

    private void onPaintActionMove(float x, float y) {
        if (paintChannel == null) {
            return;
        }
        if (!isNewPoint(x, y)) {
            return;
        }
        onActionMove(x, y);
        //发送实时数据
        //transactionManager.sendMoveTransaction(x / xZoom, y / yZoom, paintChannel.paintColor);
        saveUserData(UserInfo.getAccount(), new Transaction(Transaction.ActionStep.MOVE, x / xZoom, y / yZoom, paintChannel.paintColor), false, false, false);
    }


    private void onPaintActionEnd(float x, float y) {
        if (paintChannel == null) {
            return;
        }
        onActionEnd();
        //发送实时数据
        //transactionManager.sendEndTransaction(lastX / xZoom, lastY / yZoom, paintChannel.paintColor);
        saveUserData(UserInfo.getAccount(), new Transaction(Transaction.ActionStep.END, mLastX / xZoom, mLastY / yZoom, paintChannel.paintColor), false, false, false);
    }


    private void onActionStart(float x, float y) {
        DoodleChannel channel = paintChannel;
        if (channel == null) {
            return;
        }
        mLastX = x;
        mLastY = y;
        channel.action = new MyPath(x, y, channel.paintColor, channel.paintSize);
        channel.action.onDraw(mBufferCanvas);
    }


    private void onActionMove(float x, float y) {
        DoodleChannel channel = paintChannel;
        if (channel == null) {
            return;
        }
        // 绘制当前Action
        if (channel.action == null) {
            // 有可能action被清空，此时收到move，重新补个start
            onPaintActionStart(x, y);
        }
        channel.action.onMove(x, y);
        channel.action.onDraw(mBufferCanvas);
        invalidate();
    }

    private void onActionEnd() {
        DoodleChannel channel = paintChannel;
        if (channel == null || channel.action == null) {
            return;
        }
        channel.action.onUP(mBufferCanvas);
        channel.actions.add(channel.action);
        channel.action = null;
    }


    /**
     * 保存数据
     *
     * @param account
     * @param t
     * @param isBack
     * @param isClear
     * @param isFlip
     */
    private void saveUserData(String account, Transaction t, boolean isBack, boolean isClear, boolean isFlip) {
        List<Transaction> list = userDataMap.get(account);
        if (isBack) {
            while (list != null && list.size() > 0 && list.get(list.size() - 1).getStep() != Transaction.ActionStep.START) {
                list.remove(list.size() - 1);
            }
            if (list != null && list.size() > 0) {
                list.remove(list.size() - 1);
            }
            userDataMap.put(account, list);
        } else if (isClear) {
            userDataMap.clear();
        } else if (isFlip) {
            if (list == null) {
                list = new ArrayList<>();
                list.add(t);
            } else {
                for (Transaction transaction : list) {
                    if (transaction.getStep() == Transaction.ActionStep.Flip) {
                        list.remove(transaction);
                        break;
                    }
                }
                list.add(t);
            }
            userDataMap.put(account, list);
        } else {
            if (list == null) {
                list = new ArrayList<>();
                list.add(t);
            } else {
                list.add(t);
            }
            userDataMap.put(account, list);
        }
        if (mCallback != null) {
            mCallback.onUndoRedoStatusChanged();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        xZoom = w;
        yZoom = h;
    }

    private boolean isNewPoint(float x, float y) {
        if (Math.abs(x - mLastX) <= 0.1f && Math.abs(y - mLastY) <= 0.1f) {
            return false;
        }
        mLastX = x;
        mLastY = y;
        return true;
    }



    public interface Callback {
        void onUndoRedoStatusChanged();
    }






    public void onResume() {
    }

    public void onDestroy(){

    }


    /**
     * ******************************* 绘图板 ****************************
     */

    /**
     * 设置绘制时的画笔颜色
     *
     * @param color
     */
    public void setPaintColor(int color) {
        this.paintChannel.setColor(convertRGBToARGB(color));
    }

    /**
     * rgb颜色值转换为argb颜色值
     *
     * @param rgb
     * @return
     */
    public int convertRGBToARGB(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb >> 0) & 0xFF;

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }


    /**
     * 设置画笔的粗细
     *
     * @param size
     */
    public void setPaintSize(int size) {
        if (size > 0) {
            this.paintChannel.paintSize = size;
        }
    }

    /**
     * 设置当前画笔的形状
     *
     * @param type
     */
    public void setPaintType(int type) {
        this.paintChannel.setType(type);
    }


    /**
     * 设置当前画笔为橡皮擦
     *
     * @param size 橡皮擦的大小（画笔的粗细)
     */
    public void setEraseType(int size) {
        //this.paintChannel.setEraseType(this.bgColor, size);
    }

}
