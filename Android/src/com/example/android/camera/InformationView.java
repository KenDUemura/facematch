package com.example.android.camera;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class InformationView extends View {
    private static final String TAG = "face InformationView";
    Paint txtPaint = new Paint();
    Paint fPaint;
    List<Bitmap> detectedFaces = new ArrayList<Bitmap>();
    Canvas canvas;
    static Matrix matrix = new Matrix();
    
    public InformationView(Context context) {
        super(context);
        prepareMatrix(matrix, 0, 1920, 1080);
    }
    
    public void dumpImgSize(Bitmap img, String msg) {
        Log.v(TAG, msg + "=(" + img.getWidth() + "," + img.getHeight());
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        for (Bitmap face : detectedFaces) {
            dumpImgSize(face, "OnDraw Size");
            canvas.drawBitmap(face, 200, 200, fPaint);
        }
        
        this.canvas = canvas;
    }
    
    public InformationView(Context context, AttributeSet attr) {
        super(context, attr);
//        paint.setColor(Color.WHITE);
//        paint.setStrokeWidth(2f);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setColor(Color.GREEN);
        txtPaint.setColor(Color.BLUE);
        txtPaint.setTextSize(24);
        txtPaint.setTypeface(Typeface.DEFAULT_BOLD);
        txtPaint.setAntiAlias(true);
    }

    public void showInfo(String str) {
        Log.d("face ShowingInfo", str);
        this.canvas.drawText(str, 50, 100, txtPaint);
        super.onDraw(this.canvas);
    }
    
    /**
     * @param asList
     */
    public void setFaces(List<Bitmap> faces) {
        this.detectedFaces = faces;
    }
    
    public void drawFaces() {
        
        for (Bitmap face : detectedFaces) {
            dumpImgSize(face, "drawFaces Size");
            this.canvas.drawBitmap(face, 200, 200, fPaint);
        }
        
        this.canvas.drawRect(200, 200, 300, 300, txtPaint);
        
        Log.d(TAG, "Information Drawing");
    }
    
    /**
     * Map matrix to camera width and height
     * @param matrix
     * @param displayOrientation
     * @param viewWidth
     * @param viewHeight
     */
    public static void prepareMatrix(Matrix matrix, int displayOrientation,
            int viewWidth, int viewHeight) {

        boolean mirror = (1 == CameraInfo.CAMERA_FACING_FRONT);
        matrix.setScale(mirror ? -1 : 1, 1);
        
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);

        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        Log.d(" face View", "Width: " + viewWidth + " x Height: " + viewHeight);
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
//        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
        matrix.setTranslate(viewWidth / 2f, viewHeight / 2f);
    }
}