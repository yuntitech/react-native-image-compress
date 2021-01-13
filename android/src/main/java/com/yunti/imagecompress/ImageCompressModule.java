package com.yunti.imagecompress;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


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
            promise.reject("", "图片出错");
            return;
        }
        ThreadPoolManager.INSTANCE.getMNormalPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getCurrentActivity()
                            .getContentResolver().openInputStream(Uri.parse(imageFileUri)));
                    int initialWidth = bitmap.getWidth();
                    int initialHeight = bitmap.getHeight();
                    if (maxWidthOrHeight < initialWidth || maxWidthOrHeight < initialHeight) {
                        int max = Math.max(initialWidth, initialHeight);
                        int min = Math.min(initialWidth, initialHeight);
                        float scale = max * 1f / min;
                        max = maxWidthOrHeight;
                        min = (int) (max / scale);
                        if (initialWidth > initialHeight) {
                            bitmap = scaleCompress(bitmap, max, min);
                        } else {
                            bitmap = scaleCompress(bitmap, min, max);
                        }
                    }
                    bitmap = qualityCompress(bitmap, maxFileSize);
                    saveImage(getCurrentActivity(), bitmap, promise);
                } catch (FileNotFoundException e) {
                    promise.reject("", "图片出错");
                }
            }
        });
    }


    /**
     * 质量压缩
     *
     * @param bmp        要压缩的图片
     * @param targetSize 最终要压缩的大小（bt）
     * @return
     */
    private Bitmap qualityCompress(Bitmap bmp, int targetSize) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int originLength = baos.toByteArray().length;
        byte[] bytes;
        if (originLength > targetSize) {
            int options = 100;
            int reduceNumber = 10;
            while (baos.toByteArray().length > targetSize) {
                baos.reset();
                bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
                options -= reduceNumber;
                if (options <= 0) {
                    break;
                }
            }

            int min = options;
            int max = options + 10;
            bytes = binarySearch(bmp, baos, max, min, targetSize);
        } else {
            bytes = baos.toByteArray();
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * 二分查找最接近最大尺寸的图片
     */
    private byte[] binarySearch(Bitmap bmp, ByteArrayOutputStream baos, int max, int min, int targetSize) {
        int compressCount = 0;
        int minValue = min;
        int maxValue = max;
        byte[] finalBytes;
        boolean halfIsBigThanTarget = false;
        while (true) {
            int currentValue = (int) ((maxValue + minValue) * 1f / 2);
            baos.reset();
            bmp.compress(Bitmap.CompressFormat.JPEG, currentValue, baos);
            if (compressCount == 0) {
                halfIsBigThanTarget = baos.toByteArray().length > targetSize;
                minValue = baos.toByteArray().length < targetSize ? currentValue : min;
                maxValue = baos.toByteArray().length > targetSize ? currentValue : max;
            } else {
                if (halfIsBigThanTarget) {
                    if (baos.toByteArray().length < targetSize) {
                        finalBytes = baos.toByteArray();
                    } else {
                        baos.reset();
                        bmp.compress(Bitmap.CompressFormat.JPEG, min, baos);
                        finalBytes = baos.toByteArray();
                    }
                } else {
                    if (baos.toByteArray().length < targetSize) {
                        finalBytes = baos.toByteArray();
                    } else {
                        baos.reset();
                        bmp.compress(Bitmap.CompressFormat.JPEG, (int) ((min + max) * 1f / 2), baos);
                        finalBytes = baos.toByteArray();
                    }
                }
                break;
            }
            compressCount++;
        }
        return finalBytes;
    }


    /**
     * 尺寸压缩
     *
     * @param bmp
     * @param targetW 目标图片的宽（像素）
     * @param targetH 目标图片的高（像素）
     * @return .
     */
    private Bitmap scaleCompress(Bitmap bmp, int targetW, int targetH) {
        Bitmap targetBmp = Bitmap.createScaledBitmap(bmp, targetW, targetH, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        targetBmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return targetBmp;
    }

    private void saveImage(Context context, Bitmap image, Promise promise) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        + "/" + context.getApplicationContext().getPackageName() + "/Camera");
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }
        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            try {
                OutputStream fOut = new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
            } catch (Exception e) {
                promise.reject("", "图片出错");
            }
            WritableMap args = Arguments.createMap();
            args.putString("compressedUri", Uri.fromFile(imageFile).toString());
            promise.resolve(args);
        }

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
