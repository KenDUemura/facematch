// Copyright 2011 Google Inc. All Rights Reserved.

package com.example.android.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Original Author
 * @author anirudhd@google.com (Anirudh Dewani)
 * 
 */
public class ViewFinderView extends View {

    private static final String TAG = "viewFinder";
    Paint paint = new Paint();
    List<Camera.Face> faces = new ArrayList<Camera.Face>();
    Matrix matrix = new Matrix();
    RectF rect = new RectF();
    private int mDisplayOrientation;
    private int mOrientation;
    private float eyeDistance;

    /**
     * @param context
     */
    public ViewFinderView(Context context) {
        super(context);

    }

    public void dumpRect(RectF rect, String msg) {
        Log.v(TAG, msg + "=(" + rect.left + "," + rect.top
                + "," + rect.right + "," + rect.bottom + ")");
    }
    
    public void dumpRect(Rect rect, String msg) {
        Log.v(TAG, msg + "=(" + rect.left + "," + rect.top
                + "," + rect.right + "," + rect.bottom + ")");
    }

    @Override
    public void onDraw(Canvas canvas) {
        prepareMatrix(matrix, 0, getWidth(), getHeight());
        //canvas.save();
        //matrix.postRotate(mOrientation); // postRotate is clockwise
        //canvas.rotate(-mOrientation); // rotate is counter-clockwise (for
                                      // canvas)
        
        for (Face face : faces) {
            rect.set(face.rect);
//            dumpRect(rect, "before");
            matrix.mapRect(rect);
//            dumpRect(rect, "after");
            canvas.drawRect(rect, paint);
//            Log.d("face facedetection", "Score: " + face.score);
        }
        
        //canvas.restore();
        //Log.v(TAG, "Drawing Faces - " + faces.size());
        super.onDraw(canvas);
    }

    public void setDisplayOrientation(int orientation) {
        mDisplayOrientation = orientation;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        invalidate();
    }

    /**
     * @param asList
     */
    public void setFaces(List<Camera.Face> faces) {
        this.faces = faces;
        invalidate();
    }

    public ViewFinderView(Context context, AttributeSet attr) {
        super(context, attr);
//        paint.setColor(Color.WHITE);
//        paint.setStrokeWidth(2f);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setColor(Color.GREEN);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE); 
        paint.setStrokeWidth(3);
        paint.setAntiAlias(true);
    }

    public static void prepareMatrix(Matrix matrix, int displayOrientation,
            int viewWidth, int viewHeight) {

        boolean mirror = (1 == CameraInfo.CAMERA_FACING_FRONT);
        matrix.setScale(mirror ? -1 : 1, 1);
        
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);

        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        // Log.d("face View", "Width: " + viewWidth + " x Height: " + viewHeight);
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
//        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
        matrix.setTranslate(viewWidth / 2f, viewHeight / 2f);
    }

}
