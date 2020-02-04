package com.codepipes.ting.sliderview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews.RemoteView;

import com.codepipes.ting.R;
import com.codepipes.ting.sliderview.interfaces.ImageClickListener;
import com.codepipes.ting.sliderview.interfaces.ImageListener;
import com.codepipes.ting.sliderview.interfaces.ViewListener;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @attr ref R.styleable#SliderView_pageTransformer
 */
@RemoteView
public class SliderView extends FrameLayout {

    private final int DEFAULT_GRAVITY = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;

    private static final int DEFAULT_SLIDE_INTERVAL = 3500;
    private static final int DEFAULT_SLIDE_VELOCITY = 400;
    public static final int DEFAULT_INDICATOR_VISIBILITY = 0;


    private int mPageCount;
    private int slideInterval = DEFAULT_SLIDE_INTERVAL;
    private int mIndicatorGravity = DEFAULT_GRAVITY;
    private int indicatorMarginVertical;
    private int indicatorMarginHorizontal;
    private int pageTransformInterval = DEFAULT_SLIDE_VELOCITY;
    private int indicatorVisibility = DEFAULT_INDICATOR_VISIBILITY;

    private SliderViewPager containerViewPager;
    private CirclePageIndicator mIndicator;
    private ViewListener mViewListener = null;
    private ImageListener mImageListener = null;
    private ImageClickListener imageClickListener = null;

    private Timer swipeTimer;
    private SwipeTask swipeTask;

    private boolean autoPlay;
    private boolean disableAutoPlayOnUserInteraction;
    private boolean animateOnBoundary = true;

    private int previousState;

    private ViewPager.PageTransformer pageTransformer;

    public SliderView(Context context) {
        super(context);
    }

    public SliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs, 0, 0);
    }

    public SliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SliderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (isInEditMode()) { return; }
        else {
            View view = LayoutInflater.from(context).inflate(R.layout.view_slider_view, this, true);
            containerViewPager = (SliderViewPager) view.findViewById(R.id.containerViewPager);
            mIndicator = (CirclePageIndicator) view.findViewById(R.id.indicator);

            containerViewPager.addOnPageChangeListener(carouselOnPageChangeListener);

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderView, defStyleAttr, 0);

            try {

                indicatorMarginVertical = a.getDimensionPixelSize(R.styleable.SliderView_indicatorMarginVertical, getResources().getDimensionPixelSize(R.dimen.default_indicator_margin_vertical));
                indicatorMarginHorizontal = a.getDimensionPixelSize(R.styleable.SliderView_indicatorMarginHorizontal, getResources().getDimensionPixelSize(R.dimen.default_indicator_margin_horizontal));
                setPageTransformInterval(a.getInt(R.styleable.SliderView_pageTransformInterval, DEFAULT_SLIDE_VELOCITY));
                setSlideInterval(a.getInt(R.styleable.SliderView_slideInterval, DEFAULT_SLIDE_INTERVAL));
                setOrientation(a.getInt(R.styleable.SliderView_indicatorOrientation, LinearLayout.HORIZONTAL));
                setIndicatorGravity(a.getInt(R.styleable.SliderView_indicatorGravity, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
                setAutoPlay(a.getBoolean(R.styleable.SliderView_autoPlay, true));
                setDisableAutoPlayOnUserInteraction(a.getBoolean(R.styleable.SliderView_disableAutoPlayOnUserInteraction, false));
                setAnimateOnBoundary(a.getBoolean(R.styleable.SliderView_animateOnBoundary, true));
                setPageTransformer(a.getInt(R.styleable.SliderView_pageTransformer, SliderViewPagerTransformer.DEFAULT));

                indicatorVisibility = a.getInt(R.styleable.SliderView_indicatorVisibility, SliderView.DEFAULT_INDICATOR_VISIBILITY);

                setIndicatorVisibility(indicatorVisibility);

                if (indicatorVisibility == View.VISIBLE) {
                    int fillColor = a.getColor(R.styleable.SliderView_fillColor, 0);
                    if (fillColor != 0) { setFillColor(fillColor); }

                    int pageColor = a.getColor(R.styleable.SliderView_pageColor, 0);
                    if (pageColor != 0) { setPageColor(pageColor); }

                    float radius = a.getDimensionPixelSize(R.styleable.SliderView_radius, 0);
                    if (radius != 0) { setRadius(radius); }
                    setSnap(a.getBoolean(R.styleable.SliderView_snap, getResources().getBoolean(R.bool.default_circle_indicator_snap)));

                    int strokeColor = a.getColor(R.styleable.SliderView_strokeColor, 0);
                    if (strokeColor != 0) { setStrokeColor(strokeColor); }

                    float strokeWidth = a.getDimensionPixelSize(R.styleable.SliderView_strokeWidth, 0);
                    if (strokeWidth != 0) { setStrokeWidth(strokeWidth); }
                }
            } finally { a.recycle(); }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resetScrollTimer();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        playCarousel();
    }

    public int getSlideInterval() {
        return slideInterval;
    }

    /**
     * Set interval for one slide in milliseconds.
     *
     * @param slideInterval integer
     */
    public void setSlideInterval(int slideInterval) {
        this.slideInterval = slideInterval;

        if (null != containerViewPager) {
            playCarousel();
        }
    }

    /**
     * Set interval for one slide in milliseconds.
     *
     * @param slideInterval integer
     */
    public void reSetSlideInterval(int slideInterval) {
        setSlideInterval(slideInterval);

        if (null != containerViewPager) {
            playCarousel();
        }
    }

    /**
     * Sets speed at which page will slide from one to another in milliseconds
     *
     * @param pageTransformInterval integer
     */
    public void setPageTransformInterval(int pageTransformInterval) {
        if (pageTransformInterval > 0) {
            this.pageTransformInterval = pageTransformInterval;
        } else {
            this.pageTransformInterval = DEFAULT_SLIDE_VELOCITY;
        }

        containerViewPager.setTransitionVelocity(pageTransformInterval);
    }

    public ViewPager.PageTransformer getPageTransformer() {
        return pageTransformer;
    }

    /**
     * Sets page transition animation.
     *
     * @param pageTransformer Choose from zoom, flow, depth, slide_over .
     */
    public void setPageTransformer(ViewPager.PageTransformer pageTransformer) {
        this.pageTransformer = pageTransformer;
        containerViewPager.setPageTransformer(true, pageTransformer);
    }

    /**
     * Sets page transition animation.
     *
     * @param transformer Pass {@link SliderViewPagerTransformer#FLOW}, {@link SliderViewPagerTransformer#ZOOM}, {@link SliderViewPagerTransformer#DEPTH} or {@link SliderViewPagerTransformer#SLIDE_OVER}
     * @attr
     */
    public void setPageTransformer(@SliderViewPagerTransformer.Transformer int transformer) {
        setPageTransformer(new SliderViewPagerTransformer(transformer));

    }

    /**
     * Sets whether to animate transition from last position to first or not.
     *
     * @param animateOnBoundary .
     */
    public void setAnimateOnBoundary(boolean animateOnBoundary) {
        this.animateOnBoundary = animateOnBoundary;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    private void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public boolean isDisableAutoPlayOnUserInteraction() {
        return disableAutoPlayOnUserInteraction;
    }

    private void setDisableAutoPlayOnUserInteraction(boolean disableAutoPlayOnUserInteraction) {
        this.disableAutoPlayOnUserInteraction = disableAutoPlayOnUserInteraction;
    }

    private void setData() {
        CarouselPagerAdapter carouselPagerAdapter = new CarouselPagerAdapter(getContext());
        containerViewPager.setAdapter(carouselPagerAdapter);
        if(getPageCount() > 1) {
            mIndicator.setViewPager(containerViewPager);
            mIndicator.requestLayout();
            mIndicator.invalidate();
            containerViewPager.setOffscreenPageLimit(getPageCount());
            playCarousel();
        }
    }

    private void stopScrollTimer() {
        if (null != swipeTimer) {
            swipeTimer.cancel();
        }

        if (null != swipeTask) {
            swipeTask.cancel();
        }
    }


    private void resetScrollTimer() {
        stopScrollTimer();

        swipeTask = new SwipeTask();
        swipeTimer = new Timer();
    }


    /**
     * Starts auto scrolling if
     */
    public void playCarousel() {

        resetScrollTimer();

        if (autoPlay && slideInterval > 0 && containerViewPager.getAdapter() != null && containerViewPager.getAdapter().getCount() > 1) {

            swipeTimer.schedule(swipeTask, slideInterval, slideInterval);
        }
    }

    /**
     * Pause auto scrolling unless user interacts provided autoPlay is enabled.
     */
    public void pauseCarousel() {

        resetScrollTimer();
    }

    /**
     * Stops auto scrolling.
     */
    public void stopCarousel() {

        this.autoPlay = false;
    }


    private class CarouselPagerAdapter extends PagerAdapter {
        private Context mContext;

        public CarouselPagerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {

            Object objectToReturn;

            if (mImageListener != null) {

                ImageView imageView = new ImageView(mContext);
                imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));  //setting image position
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                objectToReturn = imageView;
                mImageListener.setImageForPosition(position, imageView);

                collection.addView(imageView);

            } else if (mViewListener != null) {

                View view = mViewListener.setViewForPosition(position);

                if (null != view) {
                    objectToReturn = view;
                    collection.addView(view);
                } else {
                    throw new RuntimeException("View can not be null for position " + position);
                }

            } else {
                throw new RuntimeException("View must set " + ImageListener.class.getSimpleName() + " or " + ViewListener.class.getSimpleName() + ".");
            }

            return objectToReturn;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getCount() {
            return getPageCount();
        }
    }

    ViewPager.OnPageChangeListener carouselOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

        @Override
        public void onPageSelected(int position) { }

        @Override
        public void onPageScrollStateChanged(int state) {

            if (previousState == ViewPager.SCROLL_STATE_DRAGGING
                    && state == ViewPager.SCROLL_STATE_SETTLING) {

                if (disableAutoPlayOnUserInteraction) {
                    pauseCarousel();
                } else {
                    playCarousel();
                }

            } else if (previousState == ViewPager.SCROLL_STATE_SETTLING
                    && state == ViewPager.SCROLL_STATE_IDLE) {
            }

            previousState = state;
        }
    };

    private class SwipeTask extends TimerTask {
        public void run() {
            containerViewPager.post(new Runnable() {
                public void run() {
                    int nextPage = (containerViewPager.getCurrentItem() + 1) % getPageCount();
                    containerViewPager.setCurrentItem(nextPage, 0 != nextPage || animateOnBoundary);
                }
            });
        }
    }

    public void setImageListener(ImageListener mImageListener) {
        this.mImageListener = mImageListener;
    }

    public void setViewListener(ViewListener mViewListener) {
        this.mViewListener = mViewListener;
    }

    public void setImageClickListener(ImageClickListener imageClickListener) {
        this.imageClickListener = imageClickListener;
        containerViewPager.setImageClickListener(imageClickListener);
    }

    public int getPageCount() {
        return mPageCount;
    }

    public void setPageCount(int mPageCount) {
        this.mPageCount = mPageCount;

        setData();
    }

    public void addOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        containerViewPager.addOnPageChangeListener(listener);
    }

    public void clearOnPageChangeListeners() {
        containerViewPager.clearOnPageChangeListeners();
    }

    public void setCurrentItem(int item) {
        containerViewPager.setCurrentItem(item);
    }
    
    public void setCurrentItem(int item, boolean smoothScroll) {
        containerViewPager.setCurrentItem(item, smoothScroll);
    }

    public int getCurrentItem() {
        return containerViewPager.getCurrentItem();
    }

    public int getIndicatorMarginVertical() {
        return indicatorMarginVertical;
    }

    public void setIndicatorMarginVertical(int _indicatorMarginVertical) {
        indicatorMarginVertical = _indicatorMarginVertical;
        LayoutParams params = (LayoutParams) getLayoutParams();
        params.topMargin = indicatorMarginVertical;
        params.bottomMargin = indicatorMarginVertical;
    }

    public int getIndicatorMarginHorizontal() {
        return indicatorMarginHorizontal;
    }
    
    public SliderViewPager getContainerViewPager() {
        return containerViewPager;
    }

    public void setIndicatorMarginHorizontal(int _indicatorMarginHorizontal) {
        indicatorMarginHorizontal = _indicatorMarginHorizontal;
        LayoutParams params = (LayoutParams) getLayoutParams();
        params.leftMargin = indicatorMarginHorizontal;
        params.rightMargin = indicatorMarginHorizontal;
    }

    public int getIndicatorGravity() {
        return mIndicatorGravity;
    }

    public void setIndicatorGravity(int gravity) {
        mIndicatorGravity = gravity;
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = mIndicatorGravity;
        params.setMargins(indicatorMarginHorizontal, indicatorMarginVertical, indicatorMarginHorizontal, indicatorMarginVertical);
        mIndicator.setLayoutParams(params);
    }

    public void setIndicatorVisibility(int visibility) {
        mIndicator.setVisibility(visibility);
    }

    public int getOrientation() {
        return mIndicator.getOrientation();
    }

    public int getFillColor() {
        return mIndicator.getFillColor();
    }

    public int getStrokeColor() {
        return mIndicator.getStrokeColor();
    }

    public void setSnap(boolean snap) {
        mIndicator.setSnap(snap);
    }

    public void setRadius(float radius) {
        mIndicator.setRadius(radius);
    }

    public float getStrokeWidth() {
        return mIndicator.getStrokeWidth();
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
    }

    public Drawable getIndicatorBackground() {
        return mIndicator.getBackground();
    }

    public void setFillColor(int fillColor) {
        mIndicator.setFillColor(fillColor);
    }

    public int getPageColor() {
        return mIndicator.getPageColor();
    }

    public void setOrientation(int orientation) {
        mIndicator.setOrientation(orientation);
    }

    public boolean isSnap() {
        return mIndicator.isSnap();
    }

    public void setStrokeColor(int strokeColor) {
        mIndicator.setStrokeColor(strokeColor);
    }

    public float getRadius() {
        return mIndicator.getRadius();
    }

    public void setPageColor(int pageColor) {
        mIndicator.setPageColor(pageColor);
    }

    public void setStrokeWidth(float strokeWidth) {
        mIndicator.setStrokeWidth(strokeWidth);
        int padding = (int) strokeWidth;
        mIndicator.setPadding(padding, padding, padding, padding);
    }
}
