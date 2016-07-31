package com.stone.waveprogress.lib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.stone.waveprogress.R;

// 该波浪依据y=Asin(ωx+φ)+k，也可以采用贝塞尔曲线
public class WaveProgress extends View {
    //三种尺寸
    protected static final int LARGE = 1;
    protected static final int MIDDLE = 2;
    protected static final int LITTLE = 3;

    private final int WAVE_HEIGHT_LARGE = 16;
    private final int WAVE_HEIGHT_MIDDLE = 8;
    private final int WAVE_HEIGHT_LITTLE = 5;

    private final float WAVE_LENGTH_MULTIPLE_LARGE = 1.5f;
    private final float WAVE_LENGTH_MULTIPLE_MIDDLE = 1f;
    private final float WAVE_LENGTH_MULTIPLE_LITTLE = 0.5f;

    private final float WAVE_HZ_FAST = 0.13f;
    private final float WAVE_HZ_NORMAL = 0.09f;
    private final float WAVE_HZ_SLOW = 0.05f;

    private final int DEFAULT_ABOVE_WAVE_COLOR = Color.WHITE;
    private final int DEFAULT_BLOW_WAVE_COLOR = Color.WHITE;

    public final int DEFAULT_ABOVE_WAVE_ALPHA = 50;
    public final int DEFAULT_BLOW_WAVE_ALPHA = 30;
    public final int DEFAULT_WAVE_RING_W = 10;

    private final float X_SPACE = 20;
    private final double PI2 = 2 * Math.PI;

    private Path mAboveWavePath = new Path();
    private Path mBlowWavePath = new Path();

    private Paint mAboveWavePaint = new Paint();
    private Paint mBlowWavePaint = new Paint();
    private Paint ringPaint = new Paint();
    private Paint textPaint = new Paint();

    private int mAboveWaveColor;
    private int mBlowWaveColor;

    private float mWaveMultiple;
    private float mWaveLength;
    private int mWaveHeight;
    private float mMaxRight;
    private float mWaveHz;

    // wave animation
    private float mAboveOffset = 0.0f;
    private float mBlowOffset;

    private RefreshProgressRunnable mRefreshProgressRunnable;

    private int left, right, bottom, top, height, width;
    // ω
    private double omega;

    //圆形的外切正方形；
    private RectF rect;
    //矫正文字在正中间
    private Rect bounds;
    //圆环的厚度
    private int ringW;
    //画笔的上下渐变
    private Shader gradientShader;

    public WaveProgress(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WaveView, R.attr.waveViewStyle, 0);
        mAboveWaveColor = attributes.getColor(R.styleable.WaveView_above_wave_color, DEFAULT_ABOVE_WAVE_COLOR);
        mBlowWaveColor = attributes.getColor(R.styleable.WaveView_blow_wave_color, DEFAULT_BLOW_WAVE_COLOR);
        ringW = attributes.getDimensionPixelSize(R.styleable.WaveView_wave_ring_w, DEFAULT_WAVE_RING_W);
        int tempmWaveHeight = attributes.getInt(R.styleable.WaveView_wave_height, MIDDLE);
        int tempmWaveMultiple = attributes.getInt(R.styleable.WaveView_wave_length, LARGE);
        int tempmWaveHz = attributes.getInt(R.styleable.WaveView_wave_frequency, MIDDLE);
        attributes.recycle();

        initializePainters();
        initializeWaveSize(tempmWaveMultiple, tempmWaveHeight, tempmWaveHz);

//        setCurrent("20%");

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //画笔渐变色
        if (gradientShader == null) {
            gradientShader = new LinearGradient(left + height / 2, top, left + height / 2, bottom, 0xFFFE0464, 0xFF7D0EB1, Shader.TileMode.REPEAT);
            ringPaint.setShader(gradientShader);
        }

        if (rect == null)
            rect = new RectF(left + ringW, top + ringW, getMeasuredHeight() - ringW, getMeasuredHeight() - ringW);

        double curY = (1 - curPercent) * getMeasuredHeight();
        double r = getMeasuredHeight() / 2;

        if (height == 0) {

            height = getMeasuredHeight();
        }
        if (width == 0) {

            width = getMeasuredWidth();
        }

        if (bounds == null) {
            bounds = new Rect();
        }
        //绘制背景
        canvas.drawCircle(width / 2 + left, height / 2 + top, width / 2, ringPaint);
        //绘制文字
        textPaint.getTextBounds(curPercentStr, 0, curPercentStr.length(), bounds);
        canvas.drawText(curPercentStr, getMeasuredWidth() / 2 - bounds.width() / 2, getMeasuredHeight() / 2 + bounds.height() / 2, textPaint);
        //绘制波浪
        canvas.drawPath(mBlowWavePath, mBlowWavePaint);
        canvas.drawPath(mAboveWavePath, mAboveWavePaint);

        float startArc;
        float byArc;

        //---------------绘制下半部分的圆弧--------------
        if (curY >= r) {

            //弧形起始角度
            startArc = getArcByPi(Math.asin((curY - r) / r));
            //圆形划过角度
            byArc = (90 - startArc) * 2;

            //---------------绘制上半部分的圆弧----------------
        } else {
            startArc = -getArcByPi(Math.asin((r - curY) / r));
            byArc = 360 - (90 + startArc) * 2;
        }

        canvas.drawArc(rect, startArc, byArc, false, mAboveWavePaint);

    }

    /**
     * 弧度转角度
     */
    private float getArcByPi(double arc) {
        return (float) (arc / Math.PI * 180);
    }

    public void setAboveWaveColor(int aboveWaveColor) {
        this.mAboveWaveColor = aboveWaveColor;
    }

    public void setBlowWaveColor(int blowWaveColor) {
        this.mBlowWaveColor = blowWaveColor;
    }

    public Paint getAboveWavePaint() {
        return mAboveWavePaint;
    }

    public Paint getBlowWavePaint() {
        return mBlowWavePaint;
    }

    public void initializeWaveSize(int waveMultiple, int waveHeight, int waveHz) {
        mWaveMultiple = getWaveMultiple(waveMultiple);
        mWaveHeight = getWaveHeight(waveHeight);
        mWaveHz = getWaveHz(waveHz);
        mBlowOffset = mWaveHeight * 0.4f;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                mWaveHeight * 2);
        setLayoutParams(params);
    }

    public void initializePainters() {
        //第一层波浪
        mAboveWavePaint.setColor(mAboveWaveColor);
        mAboveWavePaint.setAlpha(DEFAULT_ABOVE_WAVE_ALPHA);
        mAboveWavePaint.setStyle(Paint.Style.FILL);
        mAboveWavePaint.setAntiAlias(true);

        //第二层波浪
        mBlowWavePaint.setColor(mBlowWaveColor);
        mBlowWavePaint.setAlpha(DEFAULT_BLOW_WAVE_ALPHA);
        mBlowWavePaint.setStyle(Paint.Style.FILL);
        mBlowWavePaint.setAntiAlias(true);

        //外面环形的画笔
        ringPaint.setAntiAlias(true);
        ringPaint.setStyle(Paint.Style.FILL);
        //文字画笔
        textPaint.setColor(Color.GREEN);
        textPaint.setStrokeWidth(3);
        textPaint.setTextSize(40);


    }

    /**
     * 计算波浪移动路径
     */
    private void calculatePath() {
        mAboveWavePath.reset();
        mBlowWavePath.reset();

        getWaveOffset();


        int curY = top + ringW + (int) ((1 - curPercent) * (height - ringW * 2));

        float x1 = getX1WithY(curY);
        float x2 = getX2WithY(curY);

        float y;
        mAboveWavePath.moveTo(x1, curY);
        for (float x = x1; x <= x2; x += X_SPACE) {
            y = (float) (mWaveHeight * Math.sin(omega * x + mAboveOffset) + curY);
            mAboveWavePath.lineTo(x, y);
        }
        mAboveWavePath.lineTo(x2, curY);

        mBlowWavePath.moveTo(x1, curY);
        for (float x = x1; x <= x2; x += X_SPACE) {
            y = (float) (mWaveHeight * Math.sin(omega * x + mBlowOffset) + curY);
            mBlowWavePath.lineTo(x, y);
        }
        mBlowWavePath.lineTo(x2, curY);
    }

    /**
     * 设置波浪高度
     *
     * @param size
     * @return
     */
    private float getWaveMultiple(int size) {
        switch (size) {
            case LARGE:
                return WAVE_LENGTH_MULTIPLE_LARGE;
            case MIDDLE:
                return WAVE_LENGTH_MULTIPLE_MIDDLE;
            case LITTLE:
                return WAVE_LENGTH_MULTIPLE_LITTLE;
        }
        return 0;
    }

    /**
     * 设置波浪高度
     *
     * @param size
     * @return
     */
    private int getWaveHeight(int size) {
        switch (size) {
            case LARGE:
                return WAVE_HEIGHT_LARGE;
            case MIDDLE:
                return WAVE_HEIGHT_MIDDLE;
            case LITTLE:
                return WAVE_HEIGHT_LITTLE;
        }
        return 0;
    }

    /**
     * 设置波浪频率
     *
     * @param size
     * @return
     */
    private float getWaveHz(int size) {
        switch (size) {
            case LARGE:
                return WAVE_HZ_FAST;
            case MIDDLE:
                return WAVE_HZ_NORMAL;
            case LITTLE:
                return WAVE_HZ_SLOW;
        }
        return 0;
    }


    //圆的标准方程：(x - a)^2 + (y - b)^2 = r^2;

    /**
     * 获取左边的x
     *
     * @param y
     */
    private float getX1WithY(int y) {
        //r
        int r = (height - ringW * 2) / 2;
        //a
        int rX = left + r + ringW;
        //b
        int rY = top + r + ringW;

        float x1 = rX - (float) Math.sqrt(Math.pow(r, 2) - Math.pow((y - rY), 2));

        return x1;
    }

    /**
     * 获取右边的x
     *
     * @param y
     */
    private float getX2WithY(int y) {
        //r
        int r = (height - ringW * 2) / 2;
        //a
        int rX = left + r + ringW;
        //b
        int rY = top + r + ringW;

        float x2 = rX + (float) Math.sqrt(Math.pow(r, 2) - Math.pow((y - rY), 2));

        return x2;

    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (View.GONE == visibility) {
            removeCallbacks(mRefreshProgressRunnable);
        } else {
            removeCallbacks(mRefreshProgressRunnable);
            mRefreshProgressRunnable = new RefreshProgressRunnable();
            post(mRefreshProgressRunnable);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            if (mWaveLength == 0) {
                startWave();
            } else {
                removeCallbacks(mRefreshProgressRunnable);
                mRefreshProgressRunnable = new RefreshProgressRunnable();
                post(mRefreshProgressRunnable);
            }
        } else {
            removeCallbacks(mRefreshProgressRunnable);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mWaveLength == 0) {
            startWave();
        }
    }

    private void startWave() {
        if (getWidth() != 0) {
            int width = getWidth();
            mWaveLength = width * mWaveMultiple;
            left = getLeft();
            right = getRight();
            top = getTop();
            bottom = getBottom() + 2;
            mMaxRight = right + X_SPACE;
            omega = PI2 / mWaveLength;
        }
    }

    private void getWaveOffset() {
        if (mBlowOffset > Float.MAX_VALUE - 100) {
            mBlowOffset = 0;
        } else {
            mBlowOffset += mWaveHz;
        }

        if (mAboveOffset > Float.MAX_VALUE - 100) {
            mAboveOffset = 0;
        } else {
            mAboveOffset += mWaveHz;
        }
    }

    /**
     * 控制view不断重绘
     */
    private class RefreshProgressRunnable implements Runnable {
        public void run() {
            synchronized (WaveProgress.this) {
                long start = System.currentTimeMillis();

                calculatePath();

                invalidate();

                long gap = 16 - (System.currentTimeMillis() - start);
                postDelayed(this, gap < 0 ? 0 : gap);
            }
        }
    }

    private float curPercent;
    private String curPercentStr = "0%";

    /**
     * 设置当前百分比高度
     *
     * @param curPercentStr
     */
    public void setCurrent(String curPercentStr) {
        this.curPercentStr = curPercentStr;
        this.curPercent = Float.valueOf(curPercentStr.substring(0, curPercentStr.indexOf("%"))) / 100;
        //重绘
        removeCallbacks(mRefreshProgressRunnable);
        mRefreshProgressRunnable = new RefreshProgressRunnable();
        post(mRefreshProgressRunnable);
    }

}
