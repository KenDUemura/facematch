package com.example.android.camera;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class FaceMatchClient {
    public static final String BASE_URL = "http://ec2-50-17-205-114.compute-1.amazonaws.com";
    
    private final static AsyncHttpClient client = new AsyncHttpClient();

    public void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }
    
    public static void getImage(String url, BinaryHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), responseHandler);
    }
    
    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }
    
    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
