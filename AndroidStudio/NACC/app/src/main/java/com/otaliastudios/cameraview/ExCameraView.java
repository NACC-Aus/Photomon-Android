package com.otaliastudios.cameraview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

public class ExCameraView extends CameraView {
    public ExCameraView(@NonNull Context context) {
        super(context);
    }

    public ExCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected CameraController instantiateCameraController(CameraCallbacks callbacks) {
        return new ExCamera1(callbacks);
    }
}
