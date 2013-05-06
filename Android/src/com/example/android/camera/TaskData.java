package com.example.android.camera;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;

public class TaskData {
    private byte[] data;
    Camera.Parameters parameters;
    Size size;
    List<Camera.Face> faces = new ArrayList<Camera.Face>();

    
    public TaskData(byte[] data, Camera.Parameters parameters, Size size, List<Camera.Face> faces) {
        setData(data);
        this.parameters = parameters; 
        this.size = size;
        this.faces = faces;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

}