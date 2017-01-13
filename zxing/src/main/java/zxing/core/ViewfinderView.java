/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zxing.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import zxing.core.camera.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class ViewfinderView extends View {
    protected static final String TAG = ViewfinderView.class.getSimpleName();

    protected CameraPreview cameraPreview;

    // Cache the framingRect and previewFramingRect, so that we can still draw it after the preview
    // stopped.
    protected Rect framingRect;

    private static final long ANIMATION_DELAY = 10L;
    private static final int OPAQUE = 0xFF;

    private int ScreenRate;

    private static final int CORNER_WIDTH = 6;

    private static final int MIDDLE_LINE_WIDTH = 4;

    private static final int MIDDLE_LINE_PADDING = 5;

    private static final int SPEEN_DISTANCE = 5;

    private static float density;

    private Paint paint;

    private int slideTop;

    private int slideBottom;

    private Bitmap resultBitmap;

    private final int maskColor;
    private final int resultColor;

    private final int resultPointColor;
    private Collection<ResultPoint> possibleResultPoints;
    private Collection<ResultPoint> lastPossibleResultPoints;

    boolean isFirst;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        density = context.getResources().getDisplayMetrics().density;
        ScreenRate = (int) (20 * density);

        paint = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.zxing_viewfinder_mask);
        resultColor = resources.getColor(R.color.zxing_custom_result_view);

        resultPointColor = resources.getColor(R.color.zxing_viewfinder_laser);
        possibleResultPoints = new HashSet<>(5);
    }

    public void setCameraPreview(CameraPreview view) {
        this.cameraPreview = view;
        view.addStateListener(new CameraPreview.StateListener() {
            @Override
            public void previewSized() {
                refreshSizes();
                invalidate();
            }

            @Override
            public void previewStarted() {

            }

            @Override
            public void previewStopped() {

            }

            @Override
            public void cameraError(Exception error) {

            }
        });
    }

    protected void refreshSizes() {
        if (cameraPreview == null) {
            return;
        }
        Rect framingRect = cameraPreview.getFramingRect();
        if (framingRect != null) {
            this.framingRect = framingRect;
        }
    }


    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        refreshSizes();
        if (framingRect == null) {
            return;
        }

        Rect frame = framingRect;

        if (!isFirst) {
            isFirst = true;
            slideTop = frame.top;
            slideBottom = frame.bottom;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        paint.setColor(resultBitmap != null ? resultColor : maskColor);

        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom, paint);
        canvas.drawRect(frame.right, frame.top, width, frame.bottom,
                paint);
        canvas.drawRect(0, frame.bottom, width, height, paint);


        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {

            paint.setColor(resultPointColor);
            canvas.drawRect(frame.left, frame.top, frame.left + ScreenRate,
                    frame.top + CORNER_WIDTH, paint);
            canvas.drawRect(frame.left, frame.top, frame.left + CORNER_WIDTH, frame.top
                    + ScreenRate, paint);
            canvas.drawRect(frame.right - ScreenRate, frame.top, frame.right,
                    frame.top + CORNER_WIDTH, paint);
            canvas.drawRect(frame.right - CORNER_WIDTH, frame.top, frame.right, frame.top
                    + ScreenRate, paint);
            canvas.drawRect(frame.left, frame.bottom - CORNER_WIDTH, frame.left
                    + ScreenRate, frame.bottom, paint);
            canvas.drawRect(frame.left, frame.bottom - ScreenRate,
                    frame.left + CORNER_WIDTH, frame.bottom, paint);
            canvas.drawRect(frame.right - ScreenRate, frame.bottom - CORNER_WIDTH,
                    frame.right, frame.bottom, paint);
            canvas.drawRect(frame.right - CORNER_WIDTH, frame.bottom - ScreenRate,
                    frame.right, frame.bottom, paint);


            slideTop += SPEEN_DISTANCE;
            if (slideTop >= frame.bottom) {
                slideTop = frame.top;
            }
            canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH / 2, frame.right - MIDDLE_LINE_PADDING, slideTop + MIDDLE_LINE_WIDTH / 2, paint);
        }

        Collection<ResultPoint> currentPossible = possibleResultPoints;
        Collection<ResultPoint> currentLast = lastPossibleResultPoints;
        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new HashSet<>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(OPAQUE);
            paint.setColor(resultPointColor);
            for (ResultPoint point : currentPossible) {
                canvas.drawCircle(frame.left + point.getX(), frame.top
                        + point.getY(), 6.0f, paint);
            }
        }
        if (currentLast != null) {
            paint.setAlpha(OPAQUE / 2);
            paint.setColor(resultPointColor);
            for (ResultPoint point : currentLast) {
                canvas.drawCircle(frame.left + point.getX(), frame.top
                        + point.getY(), 3.0f, paint);
            }
        }

        postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
                frame.right, frame.bottom);
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param result An image of the result.
     */
    public void drawResultBitmap(Bitmap result) {
        resultBitmap = result;
        invalidate();
    }

    /**
     * Only call from the UI thread.
     *
     * @param point a point to draw, relative to the preview frame
     */
    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }

    public void pause() {

    }

    public void resume() {

    }
}
