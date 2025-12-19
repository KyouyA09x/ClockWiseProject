package com.example.mainactivity;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * Custom view that creates a spotlight effect by dimming everything except a highlighted area
 * With smooth morphing transitions between highlights
 */
public class TutorialSpotlightView extends View {

    private Paint dimPaint;
    private Paint clearPaint;
    private RectF highlightRect;
    private RectF targetRect;  // Target rectangle for morphing
    private float cornerRadius = 16f;
    private float padding = 16f;
    private ValueAnimator pulseAnimator;
    private ValueAnimator morphAnimator;
    private float pulseScale = 1.0f;
    private float morphProgress = 1.0f;  // 0 = start rect, 1 = target rect

    public TutorialSpotlightView(Context context) {
        super(context);
        init();
    }

    public TutorialSpotlightView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        
        // Paint for dimmed background
        dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dimPaint.setColor(0xCC000000); // Semi-transparent black
        
        // Paint for clearing the spotlight area
        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        
        highlightRect = new RectF();
        targetRect = new RectF();
    }

    public void highlightView(View target) {
        if (target == null) {
            clearHighlight();
            return;
        }

        int[] location = new int[2];
        int[] myLocation = new int[2];
        target.getLocationOnScreen(location);
        getLocationOnScreen(myLocation);
        
        RectF newRect = new RectF(
            location[0] - myLocation[0] - padding,
            location[1] - myLocation[1] - padding,
            location[0] - myLocation[0] + target.getWidth() + padding,
            location[1] - myLocation[1] + target.getHeight() + padding
        );
        
        animateToRect(newRect);
    }

    public void highlightRect(float left, float top, float right, float bottom) {
        // Get this view's location to calculate relative position
        int[] myLocation = new int[2];
        getLocationOnScreen(myLocation);
        
        RectF newRect = new RectF(
            left - myLocation[0] - padding, 
            top - myLocation[1] - padding, 
            right - myLocation[0] + padding, 
            bottom - myLocation[1] + padding
        );
        
        animateToRect(newRect);
    }
    
    private void animateToRect(RectF newRect) {
        // Stop any ongoing morph animation
        if (morphAnimator != null && morphAnimator.isRunning()) {
            morphAnimator.cancel();
        }
        
        // If this is the first highlight, just set it directly
        if (highlightRect.isEmpty()) {
            highlightRect.set(newRect);
            targetRect.set(newRect);
            invalidate();
            startPulseAnimation();
            return;
        }
        
        // Store current rect as start and new rect as target
        targetRect.set(newRect);
        morphProgress = 0f;
        
        // Animate smooth morph from current to target
        morphAnimator = ValueAnimator.ofFloat(0f, 1f);
        morphAnimator.setDuration(400);  // Smooth 400ms morph
        morphAnimator.setInterpolator(new DecelerateInterpolator());
        morphAnimator.addUpdateListener(animation -> {
            morphProgress = (float) animation.getAnimatedValue();
            
            // Interpolate between current and target rect
            float left = highlightRect.left + (targetRect.left - highlightRect.left) * morphProgress;
            float top = highlightRect.top + (targetRect.top - highlightRect.top) * morphProgress;
            float right = highlightRect.right + (targetRect.right - highlightRect.right) * morphProgress;
            float bottom = highlightRect.bottom + (targetRect.bottom - highlightRect.bottom) * morphProgress;
            
            // Update the highlight rect during animation
            highlightRect.set(left, top, right, bottom);
            
            invalidate();
        });
        morphAnimator.start();
        
        startPulseAnimation();
    }

    public void clearHighlight() {
        stopPulseAnimation();
        if (morphAnimator != null && morphAnimator.isRunning()) {
            morphAnimator.cancel();
        }
        highlightRect.setEmpty();
        targetRect.setEmpty();
        invalidate();
    }

    private void startPulseAnimation() {
        stopPulseAnimation();
        
        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.05f, 1.0f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new DecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulseScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        pulseScale = 1.0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw dimmed background
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
        
        // Clear the spotlight area with pulsing effect
        if (!highlightRect.isEmpty()) {
            canvas.save();
            
            // Apply pulse scale from center
            float centerX = highlightRect.centerX();
            float centerY = highlightRect.centerY();
            canvas.scale(pulseScale, pulseScale, centerX, centerY);
            
            // Draw rounded rectangle spotlight
            canvas.drawRoundRect(highlightRect, cornerRadius, cornerRadius, clearPaint);
            
            canvas.restore();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPulseAnimation();
        if (morphAnimator != null && morphAnimator.isRunning()) {
            morphAnimator.cancel();
        }
    }
}
