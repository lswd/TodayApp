package com.example.refresh;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class GodRefreshLayout extends LinearLayout {
    private Context mContext;
    private BaseRefreshManager mRefreshManager;
    private View mHeadView;
    private int downY;
    private int minHeadViewHeight;
    private int maxHeadViewHeight;
    private RefreshingListener mRefreshListener;//正在刷新回调接口
    private RecyclerView mRecyClerView;
    private ScrollView mScrollView;

    public GodRefreshLayout(Context context) {
        super(context);
        initView(context);
    }

    public GodRefreshLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public GodRefreshLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        mContext = context;
    }

    /*
   开启下拉刷新 下拉刷新的效果 是默认的
 */
    public void setRefreshManager(BaseRefreshManager manager) {
        mRefreshManager = manager;
        intHeaderView();
    }

    /**
     * 刷新完成后的操作
     */
    public void refreshOver(){
        hideHeadView(getHeadViewLayoutParams());
    }

    public interface RefreshingListener{
        void onRefreshing();
    }

//    自定义回调接口
    public void setRefreshListener(RefreshingListener refreshListener){
        this.mRefreshListener=refreshListener;
    }

    public void setRefreshManager() {
        mRefreshManager = new DefaultRefreshManager(mContext);
        intHeaderView();
    }

    private void intHeaderView() {
        Log.e("GodRefreshLayout", "intHeaderView: ");
        setOrientation(VERTICAL);
        this.mHeadView = mRefreshManager.getHeaderView();//测量该布局的高度
        mHeadView.measure(0, 0);
        int measuredHeight = mHeadView.getMeasuredHeight();//获取测量后布局的高度
        int height = mHeadView.getHeight();//这里获取到的高度是没法获取到的 因为还没开始测量
        minHeadViewHeight = -measuredHeight;
        maxHeadViewHeight = (int) (measuredHeight * 0.3f);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, measuredHeight);
        params.topMargin = -measuredHeight;//设置高度
        addView(this.mHeadView, 0, params);//主要是利用LinearLayout  将子布局添加到布局的最前面
    }

//    这个方法回调时  可以获取当前ViewGroup子View
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View childAt = getChildAt(0);
        if(childAt instanceof RecyclerView){
            mRecyClerView = (RecyclerView) childAt;
        }

        if(childAt instanceof ScrollView){
            mScrollView = (ScrollView) childAt;
        }
    }

    //    事件处理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = (int) event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                int moveY = (int) event.getY();
                if(downY==0){
                    downY=interceptDownY;
                }
                int dy=moveY-downY;
                if(dy>0){
                   LayoutParams params = getHeadViewLayoutParams();
                   int topMargin= (int)Math.min(dy/1.8f+minHeadViewHeight,maxHeadViewHeight);

//                   这个事件的处理是为了不断回调这个比例 用于一些视觉效果
                    if(topMargin<=0){
//                        0~1进行变化
                        float percent = ((-minHeadViewHeight) - (-topMargin) * 1.0f) / (-minHeadViewHeight);
                        mRefreshManager.downRefreshPercent(percent);
                    }


                    if(topMargin<0&&currentRefreshState!=RefreshState.DOWNREFRESH){
                        currentRefreshState=RefreshState.DOWNREFRESH;
//                        提示下拉刷新的一个状态
                        handleRefreshState(currentRefreshState);
                    }else if(topMargin>=0&&currentRefreshState!=RefreshState.RELEASEREFRESH){
                        currentRefreshState=RefreshState.RELEASEREFRESH;
                        handleRefreshState(currentRefreshState);
                    }
                   params.topMargin=topMargin;
                    mHeadView.setLayoutParams(params);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if(handleEventUp(event)){
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }
    int interceptDownY;
    int interceptDownX;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                interceptDownY = (int) ev.getY();
                interceptDownX = (int) ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
//                1、确定滑动的一个方向 只有上下互动时才会触发
                int dy = (int) (ev.getY() - interceptDownY);
                int dx = (int) (ev.getX() - interceptDownX);
                if(Math.abs(dy)>Math.abs(dx)&&dy>0){
                    if(handleChildViewIsTop()){
                        return true;
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean handleChildViewIsTop() {
        if(mRecyClerView!=null){
            return RefreshScrollingUtil.isRecyclerViewToTop(mRecyClerView);
        }

        if(mScrollView!=null){
            return RefreshScrollingUtil.isScrollViewOrWebViewToTop(mScrollView);
        }

//        TODO 这里判断其他的 比如Scrollview
        return false;
    }

    private boolean handleEventUp(MotionEvent event) {
        downY=0;
       LayoutParams layoutParams = getHeadViewLayoutParams();
       if (currentRefreshState==RefreshState.DOWNREFRESH){
           hideHeadView(layoutParams);
       }else if(currentRefreshState==RefreshState.RELEASEREFRESH){
//           保持刷新的一个状态
           layoutParams.topMargin=0;
           mHeadView.setLayoutParams(layoutParams);
           currentRefreshState=RefreshState.REFRESHING;
           handleRefreshState(currentRefreshState);
           if(mRefreshListener!=null){
               mRefreshListener.onRefreshing();
           }
       }
       return layoutParams.topMargin>minHeadViewHeight;
    }

    private void hideHeadView(LayoutParams layoutParams) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(layoutParams.topMargin, minHeadViewHeight);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                layoutParams.topMargin=animatedValue;
                mHeadView.setLayoutParams(layoutParams);
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentRefreshState=RefreshState.IDDLE;
                handleRefreshState(currentRefreshState);
            }
        });
        valueAnimator.setDuration(500);
        valueAnimator.start();
    }

    private LayoutParams getHeadViewLayoutParams() {
        return (LayoutParams) mHeadView.getLayoutParams();
    }

    private void handleRefreshState(RefreshState currentRefreshState) {
        switch (currentRefreshState){
            case IDDLE:
                mRefreshManager.iddleRefresh();
                break;
            case REFRESHING:
                mRefreshManager.refreshing();
                break;
            case DOWNREFRESH:
                mRefreshManager.downRefresh();
                break;
            case RELEASEREFRESH:
                mRefreshManager.releaseRefresh();
                break;
        }

    }

    private RefreshState currentRefreshState=RefreshState.IDDLE;
    //定义下拉刷新的状态 ，依次为  静止、下拉刷新、释放刷新、正在刷新、刷新完成
    private enum RefreshState{
        IDDLE,DOWNREFRESH,RELEASEREFRESH,REFRESHING
    }
}





















