package com.hua.camera.widget;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by hzw on 2016/9/23.
 */

public class StereoImageView extends ViewGroup{

    private int mTouchSlop;
    private Camera mCamera;
    private Matrix mMatrix;
    private VelocityTracker mVelocityTracker;
    private int mWidth;
    private int mHeight;
    private float mDownX, mDownY;
    private boolean isSliding = false;
    private float resistance = 1.8f;//滑动阻力
    private int mCurScreen = 1;//记录当前item
    private static final int standerSpeed = 2000;
    private static final int flingSpeed = 800;
    private int addCount;//记录手离开屏幕后，需要新增的页面次数
    private int alreadyAdd = 0;//对滑动多页时的已经新增页面次数的记录
    private boolean isAdding = false;//fling时正在添加新页面，在绘制时不需要开启camera绘制效果，否则页面会有闪动
    private State mState = State.Normal;

    private Scroller mScroller;
    private int mStartScreen = 1;
    private IStereoListener iStereoListener;
    private boolean isCan3D = true;//是否开启3D效果
    private float mAngle = 90;//两个item间的夹角

    public StereoImageView(Context context) {
        this(context, null);
    }

    public StereoImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // 是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件。
        // 如果小于这个距离就不触发移动控件，如viewpager就是用这个距离来判断用户是否翻页
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mCamera = new Camera();
        mMatrix = new Matrix();
        if(mScroller == null) {
            mScroller = new Scroller(context);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        scrollTo(0, mStartScreen * mHeight); //滑动到startScreen的位置
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childTop = 0;
        for(int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if(child.getVisibility() != View.GONE) {
                child.layout(0, childTop, child.getMeasuredWidth(), childTop + child.getMeasuredHeight());
                childTop = childTop + child.getMeasuredHeight();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN :
                isSliding = false;
                mDownX = x;
                mDownY = y;
                if(!mScroller.isFinished()) {
                    //当上一次滑动没有结束时，再次点击，强制滑动在点击位置结束
                    mScroller.setFinalY(mScroller.getCurrY());
                    mScroller.abortAnimation();
                    scrollTo(0, getScrollY());
                    isSliding = true;
                }
                break;

            case MotionEvent.ACTION_MOVE :
                if (!isSliding) {
                    isSliding = isCanSliding(ev);
                }
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isSliding;
    }

    private boolean isCanSliding(MotionEvent ev) {
        float moveX;
        float moveY;
        moveX = ev.getX();
        moveY = ev.getY();
        if(Math.abs(moveY - mDownY) > mTouchSlop && (Math.abs(moveX - mDownY) > Math.abs(moveX - mDownX))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mVelocityTracker == null) {
            //初始化速度追踪器
            //VelocityTracker主要用于追踪触摸屏事件（flinging事件和其他gestures手势事件）的速率
            mVelocityTracker = VelocityTracker.obtain();
        }
        //将事件加入到VelocityTracker类实例中
        mVelocityTracker.addMovement(event);

        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN :
                return true;

            case MotionEvent.ACTION_MOVE :
                if(isSliding) {
                    int realDelta = (int) (mDownY - y);
                    mDownY = y;
                    if(mScroller.isFinished()) {
                        recycleMove(realDelta);
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL :
            case MotionEvent.ACTION_UP :
                if(isSliding) {
                    isSliding = false;
                    //1秒内运动1000个像素
                    mVelocityTracker.computeCurrentVelocity(1000);
                    //垂直速度
                    float yVelocity = mVelocityTracker.getYVelocity();
                    //滑动的速度大于规定的速度，或者向上滑动时，上一页页面展现出的高度超过1/2。则设定状态为State.ToPre
                    if(yVelocity > standerSpeed || (getScrollY() + mHeight / 2) / mHeight < mStartScreen) {
                        mState = State.ToPre;
                    } else if (yVelocity < -standerSpeed || (getScrollY() + mHeight / 2) / mHeight > mStartScreen) {
                        //滑动的速度大于规定的速度，或者向下滑动时，下一页页面展现出的高度超过1/2。则设定状态为State.ToNext
                        mState = State.ToNext;
                    } else {
                        mState = State.Normal;
                    }
                    changeByState(yVelocity);
                }
                if(mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        //滑动没有结束时，进行的操作
        if(mScroller.computeScrollOffset()) {
            if(mState == State.ToPre) {
                scrollTo(mScroller.getCurrX(), mScroller.getCurrY() + mHeight * alreadyAdd);
                if(getScrollY() < (getHeight() + 2) && addCount > 0) {
                    isAdding = true;
                    addPre();
                    alreadyAdd ++;
                    addCount --;
                }
            } else if(mState == State.ToNext) {
                scrollTo(mScroller.getCurrX(), mScroller.getCurrY() - mHeight * alreadyAdd);
                if(getScrollY() > getHeight() && addCount > 0) {
                    isAdding = true;
                    addNext();
                    addCount--;
                    alreadyAdd ++;
                }
            } else {
                scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            }
            postInvalidate();
        }
        //滑动结束时相关用于计数变量复位
        if(mScroller.isFinished()) {
            alreadyAdd = 0;
            addCount = 0;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!isAdding && isCan3D) {
            //当开启3D效果并且当前状态不属于 computeScroll中 addPre() 或者addNext()
            //如果不做这个判断，addPre() 或者addNext()时页面会进行闪动一下
            //我当时写的时候就被这个坑了，后来通过log判断，原来是computeScroll中的onlayout,和子Child的draw触发的顺序导致的。
            //知道原理的朋友希望可以告知下
            for (int i = 0; i < getChildCount(); i++) {
                drawScreen(canvas, i, getDrawingTime());
            }
        } else {
            isAdding = false;
            super.dispatchDraw(canvas);
        }
    }

    private void drawScreen(Canvas canvas, int i, long drawingTime) {
        int curScreenY = mHeight * i;
        //屏幕中不显示的部分不进行绘制
        if(getScrollY() + mHeight < curScreenY) {
            return ;
        }

        if(curScreenY < getScrollY() - mHeight) {
            return ;
        }

        float centerX = mWidth / 2;
        float centerY = (getScrollY() > curScreenY) ? curScreenY + mHeight : curScreenY;
        float degree = mAngle * (getScrollY() - curScreenY) / mHeight;
        if(degree > 90 || degree < -90) {
            return ;
        }

        canvas.save();

        mCamera.save();
        mCamera.rotateX(degree);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();

        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);
        drawChild(canvas, getChildAt(i), drawingTime);

        canvas.restore();
    }

    private void changeByState(float yVelocity) {
        alreadyAdd = 0;
        if(getScrollY() != mHeight) {
            switch (mState) {
                case Normal:
                    toNormalAction();
                    break;

                case ToPre:
                    toPreAction(yVelocity);
                    break;

                case ToNext:
                    toNextAction(yVelocity);
                    break;
            }
            invalidate();
        }
    }

    private void toNormalAction() {
        int startY;
        int delta;
        int duration;
        mState = State.Normal;
        addCount = 0;
        startY = getScrollY();
        delta = mHeight * mStartScreen - getScrollY();
        duration = Math.abs(delta) * 4;
        mScroller.startScroll(0, startY, 0, delta, duration);
    }

    private void toPreAction(float yVelocity) {
        mState = State.ToPre;
        addPre();
        int flingSpeedCount = (yVelocity - standerSpeed) > 0 ? (int) (yVelocity - standerSpeed) : 0;
        addCount = flingSpeedCount / flingSpeed + 1;
        int startY = getScrollY() + mHeight;
        setScaleY(startY);
        int delta = -(startY - mStartScreen * mHeight) - (addCount - 1) * mHeight;
        int duration = (Math.abs(delta)) * 3;
        mScroller.startScroll(0, startY, 0, delta, duration);
        addCount--;
    }

    private void toNextAction(float yVelocity) {
        int startY;
        int delta;
        int duration;
        mState = State.ToNext;
        addNext();
        int flingSpeedCount = (Math.abs(yVelocity) - standerSpeed) > 0 ? (int) (Math.abs(yVelocity) - standerSpeed) : 0;
        addCount = flingSpeedCount / flingSpeed + 1;
        startY = getScrollY() - mHeight;
        setScrollY(startY);
        delta = mHeight * mStartScreen - startY + (addCount - 1) * mHeight;
        duration = (Math.abs(delta)) * 3;
        mScroller.startScroll(0, startY, 0, delta, duration);
        addCount--;
    }

    private void recycleMove(int delta) {
        delta = delta % mHeight;
        delta = (int) (delta / resistance);
        if(Math.abs(delta) > mHeight / 4) {
            return ;
        }
        scrollBy(0, delta);
        if(getScrollY() < 5 && mStartScreen != 0) {
            addPre();
            scrollBy(0, mHeight);
        } else if(getScrollY() > (getChildCount() - 1) * mHeight - 5) {
            addNext();
            scrollBy(0, -mHeight);
        }
    }

    private void addNext() {
        mCurScreen = (mCurScreen + 1) % getChildCount();
        int childCount = getChildCount();
        View view = getChildAt(0);
        removeViewAt(0);
        addView(view, childCount - 1);
        if(iStereoListener != null) {
            iStereoListener.toNext(mCurScreen);
        }
    }

    private void addPre() {
        mCurScreen = (mCurScreen - 1 + getChildCount()) % getChildCount();
        int childCount = getChildCount();
        View view = getChildAt(childCount - 1);
        removeViewAt(childCount - 1);
        addView(view, 0);
        if(iStereoListener != null) {
            iStereoListener.toPre(mCurScreen);
        }
    }

    public interface IStereoListener {
        //上滑一页时回调
        void toPre(int curScreen);

        //下滑一页时回调
        void toNext(int curScreen);
    }

    public enum State {
        Normal, ToPre, ToNext
    }
}
