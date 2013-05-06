/*
 * Copyright (C) 2007 The Android Open Source Project
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

/**
 * Modified
 * @author kenduemura@gmail.com (Ken D Uemura)
 */

package com.example.android.camera;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

// ----------------------------------------------------------------------

public class CameraActivity extends Activity {
    private CameraPreview mPreview;
    Size PreviewSize;
    Camera mCamera;
    Parameters parameters;
    FaceMatchTask myTask = new FaceMatchTask();
    static String[] allowedContentTypes = new String[] { "image/png", "image/jpeg" };
    
    private int mOrientation;
    private int mOrientationCompensation;
    private int mDisplayOrientation;
    private int imageFormat;
    private int defaultCameraId;
    private int pWidth;
    private int pHeight;
    private int previousMatchLevel = 0;
    private int trialCounter = 0;
    private Boolean notRequesting = true;
    private Boolean fdButtonClicked = false;
    Matrix matrix = new Matrix();
    
    private MyOrientationEventListener mOrientationListener;
    private ViewFinderView mViewFinderView;
    private InformationView informationView;
    private static ImageView imageView;
    private TextView textView;
    private TextView textServerView;
    private Button button;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        // mPreview = new Preview(this);
        setContentView(R.layout.main);
        mPreview = (CameraPreview) findViewById(R.id.surface_view);
        mViewFinderView = (ViewFinderView) findViewById(R.id.viewfinder_view);
        textView = (TextView) findViewById(R.id.textView1);
        textServerView = (TextView) findViewById(R.id.textView2);
        imageView = (ImageView) findViewById(R.id.imageView1);
        
        // Find the total number of cameras available
        button = (Button) findViewById(R.id.btn_face_detection);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (fdButtonClicked) {
                    fdButtonClicked = false;
                    
                    mCamera.stopFaceDetection();
                    button.setText("StartFaceDetection");
                } else {
                    fdButtonClicked = true;
                    notRequesting = true;
                    mCamera.startFaceDetection();
                    button.setText("StopFaceDetection");
                }
//                mCamera.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
                //mViewFinderView.setDisplayOrientation(mDisplayOrientation);
                Log.d("Activity", "Button Clicked FD state: "+ fdButtonClicked.toString());
            }
        });

        //mOrientationListener = new MyOrientationEventListener(this);
        //mOrientationListener.enable();
    }

    /**
     * Callback for taking photos.
     */
    ShutterCallback myShutterCallback = new ShutterCallback(){

        @Override
        public void onShutter() {
            // TODO Auto-generated method stub

        }
    };

    PictureCallback myPictureCallback_RAW = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub

        }
    };

    PictureCallback myPictureCallback_JPG = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
            Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);
            Log.d("face PCallback", "Pic Call back JPG");
            informationView.setFaces(Arrays.asList(bitmapPicture));
            
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        
        Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
            
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                
                if (notRequesting && mPreview.faces.size() >= 1 && imageFormat == ImageFormat.NV21) {

                    // Block Request.
                    notRequesting = false;
                    
                    try {
                        Camera.Parameters parameters = camera.getParameters(); 
                        Size size = parameters.getPreviewSize();
                        
                        textServerView.setText("Preparing Image to send");
                        YuvImage previewImg = new YuvImage(data, parameters.getPreviewFormat(), 
                                size.width, size.height, null);
                        pWidth = previewImg.getWidth();
                        pHeight = previewImg.getHeight();
                        
                        Log.d("face View", "Width: " + pWidth + " x Height: " + pHeight);
                        prepareMatrix(matrix, 0, pWidth, pHeight);
                        List<Rect> foundFaces = getFaces();

                        for (Rect cRect: foundFaces) {
                            // Cropping
                            ByteArrayOutputStream bao = new ByteArrayOutputStream();
                            previewImg.compressToJpeg(cRect, 100, bao);
                            byte[] mydata = bao.toByteArray();
                            
                            // Resizing
                            ByteArrayOutputStream sbao = new ByteArrayOutputStream();
                            Bitmap bm = BitmapFactory.decodeByteArray(mydata, 0, mydata.length); 
                            Bitmap sbm = Bitmap.createScaledBitmap(bm, 100, 100, true);
                            bm.recycle();
                            sbm.compress(Bitmap.CompressFormat.JPEG, 100, sbao);
                            byte[] mysdata = sbao.toByteArray();
                            
                            RequestParams params = new RequestParams();
                            params.put("upload", new ByteArrayInputStream(mysdata), "tmp.jpg");
                            
                            textServerView.setText("Sending Image to the Server");
                            
                            FaceMatchClient.post(":8080/match", params, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(JSONArray result) {
                                    Log.d("face onSuccess", result.toString());
                                    
                                    try {
                                        JSONObject myJson = (JSONObject) result.get(0);
                                        float dist = (float) Double.parseDouble(myJson.getString("dist"));
                                        Log.d("distance", ""+dist);
                                        int level = (int) ((1 - dist) * 100);
                                        if (level > previousMatchLevel) {
                                            textView.setText("Match " + level +"% with "+ myJson.getString("name") +" <"+ myJson.getString("email") +"> ");

                                            loadImage(myJson.getString("classes"), myJson.getString("username"));
                                        }
                                    
                                        previousMatchLevel = level;
                                        trialCounter++;
                                        
                                        if (trialCounter < 100 && level < 74) {
                                            textServerView.setText("Retrying...");
                                            notRequesting = true;
                                        } else if (trialCounter == 100){
                                            textServerView.setText("Fail...");
                                        } else {
                                            textServerView.setText("Found Good Match? If not try again!");
                                            fdButtonClicked = false;
                                            trialCounter = 0;
                                            previousMatchLevel = 0;
                                            mCamera.stopFaceDetection();
                                            button.setText("StartFaceDetection");
                                        }

                                    } catch (JSONException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }

//                                        informationView.showInfo(myJson);
                                }
                                
                                
                            });
                        }

                        textServerView.setText("POST Sent");
                        textServerView.setText("Awaiting for response");

                            
                    } catch (Exception e) {
                        e.printStackTrace();  
                        textServerView.setText("Error AsyncPOST");
                    }
                }
            }
        };
        
        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();
        mCamera.setPreviewCallback(previewCallback);

        // To use front camera
//        mCamera = Camera.open(CameraActivity.getFrontCameraId());
        mPreview.setCamera(mCamera);
        parameters = mCamera.getParameters();
        imageFormat = parameters.getPreviewFormat();
        PreviewSize = parameters.getPreviewSize();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public static void loadImage(String classes, String username) {
        
        FaceMatchClient.getImage("/img/"+classes+"/original/"+username+"_original.JPEG", new BinaryHttpResponseHandler(allowedContentTypes){
            
            @Override
            public void onSuccess(byte[] fileData) {
                Bitmap bmp = BitmapFactory.decodeByteArray(fileData, 0, fileData.length); ;
                imageView.setImageBitmap(bmp);
            }
        });
    }

    
    public static int getFrontCameraId() {
        CameraInfo ci = new CameraInfo();
        for (int i = 0 ; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) return i;
        }
        return -1; // No front-facing camera found
    }
    
    public static int roundOrientation(int orientation) {
        return ((orientation + 45) / 90 * 90) % 360;
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    private class MyOrientationEventListener
            extends OrientationEventListener {

        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN)
                return;
            mOrientation = roundOrientation(orientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + CameraActivity.getDisplayRotation(CameraActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                setOrientationIndicator(mOrientationCompensation);
            }
        }
    }

    private void setOrientationIndicator(int degree) {
//        if (mViewFinderView != null)
//            mViewFinderView.setOrientation(degree);
    }

    public static void prepareMatrix(Matrix matrix, int displayOrientation,
            int viewWidth, int viewHeight) {

        Log.d("face CameraPreview", "Width: "+ viewWidth +" Height: "+viewHeight);
        
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);

        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        // Log.d("face View", "Width: " + viewWidth + " x Height: " + viewHeight);
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
//        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
        matrix.setTranslate(viewWidth / 2f, viewHeight / 2f);
    }
    
    private List<Rect> getFaces () {
        List<Rect> found = new ArrayList<Rect>();
        
        for (Face face : mPreview.faces) {
            if (face.score < 100)
                continue;

            Rect rect = new Rect();
            RectF rectf = new RectF();
            
            rectf.set(face.rect);
            mViewFinderView.dumpRect(rectf, "before");
            matrix.mapRect(rectf);
            mViewFinderView.dumpRect(rectf, "after");
            
            float adj = (rectf.bottom - rectf.top - rectf.right + rectf.left) / 2;
            rectf.set(rectf.left - adj, rectf.top, rectf.right + adj, rectf.bottom);
            mViewFinderView.dumpRect(rectf, "Squared");
            
            if (rectf.left < 0) {
                rectf.set(0, rectf.top, rectf.right - rectf.left, rectf.bottom);
                Log.d("face Adjust", "Left");
            } else if (rectf.right > pWidth) {
                rectf.set(rectf.left - rectf.right + pWidth, rectf.top, pWidth, rectf.bottom);
                Log.d("face Adjust", "Right");
            }
            
            if (rectf.top < 0) {
                rectf.set(rectf.left, 0, rectf.right, rectf.bottom - rectf.top);
                Log.d("face Adjust", "Top");
            } else if (rectf.bottom > pHeight) {
                rectf.set(rectf.left, rectf.top - rectf.bottom + pHeight, rectf.right, pHeight);
                Log.d("face Adjust", "Bottom");
            }
            
            mViewFinderView.dumpRect(rectf, "Shift Adjusted");
            rectf.round(rect);
            found.add(rect);
        }
        
        return found;
    }
    
    class FaceMatchTask extends AsyncTask <TaskData, Integer, String> {

        protected void onProgressUpdate(Integer... progress) {
            // setProgressPercent(progress[0]);
            Log.d("face Progress update", ""+ progress[0]);
        }

        protected void onPostExecute(String result) {
            //showDialog("Downloaded " + result + " bytes");
            Log.d("face Result", result);
            textServerView.setText("Awaiting for response");
        }

        @Override
        protected String doInBackground(TaskData... data) {
            
            // @TODO: Crop face area only and send it to the server.
            for (Face face : data[0].faces) {
                if (face.score < 100)
                    continue;

//                rectf.set(face.rect);
//                mViewFinderView.dumpRect(rectf, "before");
//                mViewFinderView.matrix.mapRect(rectf);
//                mViewFinderView.dumpRect(rectf, "after");
//                
//                float adj = (rectf.bottom - rectf.top - rectf.right + rectf.left) / 2;
//                rectf.set(rectf.left - adj, rectf.top, rectf.right + adj, rectf.bottom);
//                mViewFinderView.dumpRect(rectf, "Squared");
//                
//                if (rectf.left < 0) {
//                    rectf.set(0, rectf.top, rectf.right - rectf.left, rectf.bottom);
//                    Log.d("face Adjust", "Left");
//                } else if (rectf.right > pWidth) {
//                    rectf.set(rectf.left - rectf.right + pWidth, rectf.top, pWidth, rectf.bottom);
//                    Log.d("face Adjust", "Right");
//                }
//                
//                if (rectf.top < 0) {
//                    rectf.set(rectf.left, 0, rectf.right, rectf.bottom - rectf.top);
//                    Log.d("face Adjust", "Top");
//                } else if (rectf.bottom > pHeight) {
//                    rectf.set(rectf.left, rectf.top - rectf.bottom + pHeight, rectf.right, pHeight);
//                    Log.d("face Adjust", "Bottom");
//                }
//                
//                mViewFinderView.dumpRect(rect, "Shift Adjusted");
//                
//                rectf.set((float)(rectf.left * 0.75), (float)(rectf.top * 0.718), 
//                        (float)(rectf.right * 0.75), (float)(rectf.bottom * 0.718));
//                
//                mViewFinderView.dumpRect(rect, "Rounded");
//                
//                String filename = "cropped_"+ count +".jpg";
//                try {
//                    FileOutputStream fos = openFileOutput(filename, Context.MODE_WORLD_READABLE);
//                    previewImg.compressToJpeg(rect, 100, fos);
//                    fos.close();
//                } catch (FileNotFoundException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//
//                Log.d("face File Done", filename);
                
            }
//
//                
//                byte[] imageBytes = out.toByteArray();
//                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0,
//                        imageBytes.length);
//                detectedFaces.add(image);
//                
//                //you can create a new file name "test.jpg" in sdcard folder.
//
//            }
//            
//            informationView.setFaces(detectedFaces);
//            Log.d("face SentToIVIEW", "Number of detected faces: " + informationView.detectedFaces.size());
//            informationView.drawFaces();
            
//          AsyncHttpClient client = new AsyncHttpClient();
//          client.get("http://192.168.1.74:3000", new AsyncHttpResponseHandler() {
//              @Override
//              public void onSuccess(String response) {
//                  Log.d("face Async", response);
//              }
//          });
//          
//          for (int i = 0; i < count; i++) {
//              //totalSize += Downloader.downloadFile(urls[i]);
//              publishProgress((int) ((i / (float) count) * 100));
//              // Escape early if cancel() is called
//              if (isCancelled()) break;
//              try {
//                  Thread.sleep(10);
//              } catch (Exception e) {
//                  // TODO: handle exception
//              }
//              
//          }
            
            // @TODO Send image to the server using the HttpClient or Async-http.
            
            // @TODO Display returned information on view on InformationView.
            
            return "Done";
        }
    }
    
    
}

// ----------------------------------------------------------------------

