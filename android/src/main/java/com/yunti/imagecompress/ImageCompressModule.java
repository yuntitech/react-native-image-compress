package com.yunti.imagecompress;


import android.net.Uri;
import android.text.TextUtils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;

import java.io.File;
import java.net.URI;


/**
 * @version V1.0 图片压缩的模块
 * @FileName: ImageCompressModule.java
 * @author: villa_mou
 * @date: 01-14:43
 * @desc
 */
@ReactModule(name = ImageCompressModule.NAME)
public class ImageCompressModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    static final String NAME = "YTImageCompress";

    public ImageCompressModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public void initialize() {
        super.initialize();

    }

    @ReactMethod
    public void compressImage(ReadableMap params, final Promise promise) {
        final String imageFileUri = params.getString("imageFileUri");
        final int maxWidthOrHeight = params.getInt("maxWidthOrHeight");
        final int maxFileSize = params.getInt("maxFileSize");
        if (TextUtils.isEmpty(imageFileUri) || maxWidthOrHeight == 0 || maxFileSize == 0 || getCurrentActivity() == null) {
            promise.reject("", "读取图片失败");
            return;
        }
        ThreadPoolManager.INSTANCE.getMNormalPool().execute(new Runnable() {
            @Override
            public void run() {
                Compresser compresser = new Compresser(maxFileSize / 1024, maxWidthOrHeight, maxWidthOrHeight, getCurrentActivity());
                try {
                    final File originFile = new File(new URI(imageFileUri));
                    if (!originFile.exists()) {
                        promise.reject("", "读取图片失败");
                        return;
                    }
                    File compressFile = compresser.customCompress(originFile);
                    WritableMap args = Arguments.createMap();
                    args.putString("compressedUri", Uri.fromFile(compressFile).toString());
                    promise.resolve(args);
                } catch (Exception e) {
                    promise.reject("", "读取图片失败");
                }
            }
        });
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {

    }
}
