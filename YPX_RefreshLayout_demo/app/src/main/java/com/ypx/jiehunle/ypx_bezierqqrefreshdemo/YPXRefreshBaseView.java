package com.ypx.jiehunle.ypx_bezierqqrefreshdemo;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 *
 * 功能: 下拉刷新基础类,
 *      继承该类实现三个抽象方法可以定制任何样式下拉刷新
 *
 * 作者：yangpeixing on 17/1/18 14:52
 * 博客主页：http://blog.csdn.net/qq_16674697?viewmode=list
 */
public abstract class YPXRefreshBaseView extends LinearLayout {
    /**
     * 下拉刷新状态
     */
    public static final int REFRESH_BY_PULLDOWN = 0;
    /**
     * 松开刷新状态
     */
    public static final int REFRESH_BY_RELEASE = 1;

    /**
     * 正在刷新状态
     */
    public static final int REFRESHING = 2;
    /**
     * 刷新成功状态
     */
    public static final int REFRESHING_SUCCESS = 3;
    /**
     * 刷新失败状态
     */
    public static final int REFRESHING_FAILD = 4;

    /**
     * 收回到刷新位置状态
     */
    public static final int TAKEBACK_REFRESH = -1;
    /**
     * 收回到初始位置状态
     */
    public static final int TAKEBACK_RESET = -2;
    /**
     * 从头收到尾,不考虑中间状态
     */
    public static final int TAKEBACK_ALL = -3;


    protected RefreshListener refreshListener;
    private int lastY;
    protected int lastTop;
    /**
     * 刷新状态
     */
    protected int refreshState = REFRESH_BY_PULLDOWN;
    /**
     * 收回状态
     */
    protected int takeBackState = TAKEBACK_RESET;
    /**
     * 是否可刷新标记
     */
    private boolean isRefreshEnabled = true;

    protected int refreshTargetTop=-dp(60);//刷新头部高度
    protected View refreshView;
    protected ObjectAnimator anim;
    protected Context mContext;


    /**
     * 抽象方法,由用户实现,返回刷新头部布局
     * @return 刷新头布局
     */
    protected abstract View getRefreshHeaderView();

    /**
     * 抽象方法2,由用户实现,处理下滑事件
     * @param lp 刷新头布局的LayoutParams
     * @param lastTop  手指滑动的距离 ,lastTop∈[refreshTargetTop,+无穷]
     */
    protected abstract void doMovement(LayoutParams lp, int lastTop);

    /**
     * 抽象方法3,由用户实现,处理手指抬起后操作,过滤掉在刷新过程中以及触发刷新后的手指操作
     * @param lp LayoutParams
     */
    protected abstract void fling(LayoutParams lp);

    public YPXRefreshBaseView(Context context) {
        this(context,null);

    }

    public YPXRefreshBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initRefreshView();
        initAnimator();
    }

    private void initRefreshView() {
        //刷新视图顶端的的view
        refreshView = getRefreshHeaderView();
        LayoutParams lp = new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, -refreshTargetTop);
        lp.topMargin = refreshTargetTop;
        addView(refreshView, lp);
        lastTop = refreshTargetTop;
    }

    private void initAnimator() {
        anim = ObjectAnimator.ofFloat(refreshView, "ypx", 0.0f, 1.0f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float cVal = (Float) valueAnimator.getAnimatedValue();
                LayoutParams lp = (LayoutParams) refreshView.getLayoutParams();
                switch (takeBackState) {
                    case TAKEBACK_REFRESH:
                        lp.height = lp.height + (int) (cVal * (-refreshTargetTop - lp.height));
                        lp.topMargin = lp.topMargin + (int) (cVal * (0 - lp.topMargin));
                        break;
                    case TAKEBACK_RESET:
                        lp.topMargin = lp.topMargin + (int) (cVal * (refreshTargetTop - lp.topMargin));
                        lp.height = -refreshTargetTop;
                        break;
                    case TAKEBACK_ALL:
                        lp.topMargin = lp.topMargin + (int) (cVal * (refreshTargetTop - lp.topMargin));
                        lp.height = lp.height + (int) (cVal * (-refreshTargetTop - lp.height));
                        break;
                }

                refreshView.setLayoutParams(lp);
                refreshView.invalidate();
                invalidate();
                if (lp.height == -refreshTargetTop
                        && lp.topMargin == refreshTargetTop) {//动画完成
                    resetRefreshView();
                }
            }
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int y = (int) event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //记录下y坐标
                lastY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                //y移动坐标
                int m = y - lastY;
                doMovement(m);
                //记录下此刻y坐标
                this.lastY = y;
                break;

            case MotionEvent.ACTION_UP:
                fling();
                break;
        }
        return true;
    }


    /**
     * 下拉move事件处理
     *
     * @param moveY 移动的Y值
     */
    private void doMovement(float moveY) {
        if (refreshState == REFRESHING
                ||refreshState==REFRESHING_SUCCESS
                ||refreshState==REFRESHING_FAILD
                ||anim.isRunning()) {
            return;
        }
        LayoutParams lp = (LayoutParams) refreshView.getLayoutParams();
        lastTop += moveY * 0.5;
        if(lastTop<refreshTargetTop){
            return;
        }
        doMovement(lp,lastTop);
        refreshView.setLayoutParams(lp);
        refreshView.invalidate();
        invalidate();
    }

    /**
     * up事件处理
     */
    private void fling() {
        if (refreshState == REFRESHING
                ||refreshState==REFRESHING_SUCCESS
                ||refreshState==REFRESHING_FAILD
                ||anim.isRunning()) {
            return;
        }
        LayoutParams lp = (LayoutParams) refreshView.getLayoutParams();
        fling(lp);
    }



    /**
     * 结束刷新事件
     * @param isOK 是否刷新成功
     */
    public void finishRefresh(boolean isOK) {
        if (isOK) {
            refreshOK();
        } else {
            refreshFailed();
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                animRefreshView(500, TAKEBACK_RESET);
            }
        }, 300);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (!isRefreshEnabled) {
            return false;
        }
        int action = e.getAction();
        int y = (int) e.getRawY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                if (y > lastY && canScroll()) {
                    return true;
                }
                //记录下此刻y坐标
                this.lastY = y;
                break;
        }
        return false;
    }

    /**
     * 判断第二个子View是否可以向下滑动
     *
     * @return 是否可以滑动
     */
    public boolean canScroll(){
        View childView;
        if (getChildCount() > 1) {
            childView = this.getChildAt(1);
            if(ViewCompat.canScrollVertically(childView,-1)){
                return false;
            }else {
                return true;
            }
        }
        return true;
    }

    /**
     * @deprecated 已废弃
     *
     * @return 是否可以滑动
     */
    public boolean isCanScroll() {
        View childView;
        if (getChildCount() > 1) {
            childView = this.getChildAt(1);
            if (childView instanceof ListView) {
                int top = ((ListView) childView).getChildAt(0).getTop();
                int pad = ((ListView) childView).getListPaddingTop();
                if ((Math.abs(top - pad)) < 3 &&
                        ((ListView) childView).getFirstVisiblePosition() == 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (childView instanceof ScrollView) {
                if (((ScrollView) childView).getScrollY() == 0) {
                    return true;
                } else {
                    return false;
                }
            }else if (childView instanceof WebView) {
                if (((WebView) childView).getScrollY() == 0) {
                    return true;
                } else {
                    return false;
                }
            }else if (childView instanceof GridView) {
                int top = ((GridView) childView).getChildAt(0).getTop();
                int pad = ((GridView) childView).getListPaddingTop();
                if ((Math.abs(top - pad)) < 3 &&
                        ((GridView) childView).getFirstVisiblePosition() == 0) {
                    return true;
                } else {
                    return false;
                }
            }else if (childView instanceof RecyclerView) {
                if(ViewCompat.canScrollVertically(childView,-1)){
                    return false;
                }else {
                    return true;
                }

//                RecyclerView.LayoutManager manager=((RecyclerView)childView).getLayoutManager();
//                int top=0;
//                if(manager instanceof LinearLayoutManager){
//                    top = ((LinearLayoutManager)manager).findFirstVisibleItemPosition();
//                }else  if(manager instanceof StaggeredGridLayoutManager){
//                    top = ((StaggeredGridLayoutManager)manager).findFirstVisibleItemPositions(null)[0];
//                }
//
//                if(((RecyclerView)childView).getChildAt(0).getY()==0 &&top==0){
//                    return true;
//                } else {
//                    return false;
//                }
            }
        }
        return true;
    }


    /**
     * 从开始位置滑动到结束位置
     *
     * @param duration  滑动事件
     * @param takeBackState 收回状态
     */
    protected void animRefreshView(int duration, int takeBackState) {
        this.takeBackState = takeBackState;
        if (!anim.isRunning()) {
            anim.start();
            anim.setDuration(duration);
        }
    }

    /**
     * 初始化
     */
    protected void resetRefreshView() {
        lastTop = refreshTargetTop;
        takeBackState=TAKEBACK_RESET;
        LayoutParams lp = (LayoutParams) refreshView.getLayoutParams();
        lp.height = -refreshTargetTop;
        refreshView.setLayoutParams(lp);
        refreshView.invalidate();
        pullDownToRefresh();
    }

    /**
     * 下拉刷新状态
     */
    protected void pullDownToRefresh() {
        setRefreshState(REFRESH_BY_PULLDOWN);
    }

    /**
     * 松开刷新状态
     */
    protected void pullUpToRefresh() {
        setRefreshState(REFRESH_BY_RELEASE);
    }

    /**
     * 正在刷新状态
     */
    protected void refreshing() {
        setRefreshState(REFRESHING);
    }

    /**
     * 刷新成功状态
     */
    protected void refreshOK() {
        setRefreshState(REFRESHING_SUCCESS);
    }

    /**
     * 刷新失败状态
     */
    protected void refreshFailed() {
        setRefreshState(REFRESHING_FAILD);
    }

    /**
     * 设置是否可以刷新
     * @param b 是否可以刷新
     */
    public void setRefreshEnabled(boolean b) {
        this.isRefreshEnabled = b;
    }

    /**
     * 设置刷新回调
     * @param listener 刷新回调
     */
    public void setRefreshListener(RefreshListener listener) {
        this.refreshListener = listener;
    }

    /**
     * 获取当前刷新状态
     * @return 刷新状态
     */
    public int getRefreshState() {
        return refreshState;
    }

    /**
     * 设置当前刷新状态
     *
     * @param refreshState 设置刷新状态
     */
    protected void setRefreshState(int refreshState) {
        this.refreshState = refreshState;
    }

    /**
     * 刷新监听接口
     *
     * @author Nono
     */
    public interface RefreshListener {
        /**
         * 刷新回调
         */
        void onRefresh();
    }


    public int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, getContext().getResources().getDisplayMetrics());
    }

}