package com.hua.customview.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.hua.customview.R;

/**
 *
 * Created by hzw on 2016/9/22.
 */
public class PolyToPolyView extends View {

    private static final String tag = "PolyToPolyView";

    private float mTriggerRadius = 180; //手指触发区域半径

    private Bitmap mBitmap;
    private Matrix mMatrix;
    private int mPointCount;
    private Paint mPointPaint;

    private float[] src = new float[8];
    private float[] dst = new float[8];

    public PolyToPolyView(Context context) {
        this(context, null);
    }

    public PolyToPolyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.img01);
        float[] temp = {
                0, 0,
                mBitmap.getWidth(), 0,
                mBitmap.getWidth(), mBitmap.getHeight(),
                0, mBitmap.getHeight()
        };
        src = temp.clone();
        dst = temp.clone();

        mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointPaint.setStrokeWidth(50);
        mPointPaint.setStrokeCap(Paint.Cap.ROUND);
        mPointPaint.setColor(getResources().getColor(R.color.colorAccent));

        mMatrix = new Matrix();
        mPointCount = 4;
        mMatrix.setPolyToPoly(src, 0, dst, 0, mPointCount);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(100, 100);

        canvas.drawBitmap(mBitmap, mMatrix, null);
        mMatrix.mapPoints(dst, src);
        for(int i = 0; i < mPointCount * 2; i+=2) {
            canvas.drawPoint(dst[i], dst[i + 1], mPointPaint);
        }
    }

    private void resetPolyMatrix(int pointCount) {
        mMatrix.reset();
        mMatrix.setPolyToPoly(src, 0, dst, 0, pointCount);
        invalidate();
    }

    public int getPointCount() {
        return mPointCount;
    }

    public void setPointCount(int pointCount) {
        mPointCount = pointCount > 4 || pointCount < 0 ? 4 : pointCount;
        dst = src.clone();
        resetPolyMatrix(mPointCount);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            for(int i = 0; i < mPointCount * 2; i+=2) {
                if(Math.abs(x - dst[i]) < mTriggerRadius && Math.abs(y - dst[i + 1]) < mTriggerRadius) {
                    dst[i] = x - 100;
                    dst[i+1] = y - 100;
                    break;
                }
            }
            resetPolyMatrix(mPointCount);
        }
        return true;
    }
}
