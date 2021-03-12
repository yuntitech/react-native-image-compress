package com.yunti.imagecompress;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;

class Compresser {
    /**
     * 单位为kb，不是bt
     */
    private int mMaxSize;
    private int mMaxWidth;
    private int mMaxHeight;

    private ByteArrayOutputStream mByteArrayOutputStream;
    Context mContext;

    Compresser(int maxSize, int maxWidth, int maxHeight, Context context) {
        mMaxSize = maxSize;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mContext = context;
    }


    public File customCompress(@NonNull File file) throws IOException {
        String thumbFilePath = getCacheFilePath(mContext);
        String filePath = file.getAbsolutePath();

        int angle = getImageSpinAngle(filePath);
        long fileSize = mMaxSize > 0 && mMaxSize < file.length() / 1024 ? mMaxSize
                : file.length() / 1024;

        int[] size = getImageSize(filePath);
        int width = size[0];
        int height = size[1];

        if (mMaxSize > 0 && mMaxSize < file.length() / 1024f) {
            // find a suitable size
            float scale = (float) Math.sqrt(file.length() / 1024f / mMaxSize);
            width = (int) (width / scale);
            height = (int) (height / scale);
        }

        // check the width&height
        if (mMaxWidth > 0) {
            width = Math.min(width, mMaxWidth);
        }
        if (mMaxHeight > 0) {
            height = Math.min(height, mMaxHeight);
        }
        float scale = Math.min((float) width / size[0], (float) height / size[1]);
        width = (int) (size[0] * scale);
        height = (int) (size[1] * scale);

        // 不压缩
        if (mMaxSize > file.length() / 1024f && scale == 1) {
            return file;
        }

        return compress(filePath, thumbFilePath, width, height, angle, fileSize);
    }

    private String getCacheFilePath(Context context) {
        StringBuilder name = new StringBuilder("bookln_book" + System.currentTimeMillis());
        name.append(".jpg");
        return getPhotoCacheDir(context).getAbsolutePath() + File.separator + name;
    }

    private File getPhotoCacheDir(Context context) {
        File cacheDir = context.getCacheDir();
        if (cacheDir != null) {
            File result = new File(cacheDir, "bookln_book");
            if (!result.mkdirs() && (!result.exists() || !result.isDirectory())) {
                return null;
            }
            return result;
        }
        return null;
    }


    /**
     * obtain the image's width and height
     *
     * @param imagePath the path of image
     */
    public int[] getImageSize(String imagePath) {
        int[] res = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;
        BitmapFactory.decodeFile(imagePath, options);

        res[0] = options.outWidth;
        res[1] = options.outHeight;

        return res;
    }

    /**
     * obtain the thumbnail that specify the size
     *
     * @param imagePath the target image path
     * @param width     the width of thumbnail
     * @param height    the height of thumbnail
     * @return {@link Bitmap}
     */
    private Bitmap compress(String imagePath, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        int outH = options.outHeight;
        int outW = options.outWidth;
        int inSampleSize = 1;

        while (outH / inSampleSize > height || outW / inSampleSize > width) {
            inSampleSize *= 2;
        }

        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(imagePath, options);
    }

    /**
     * obtain the image rotation angle
     *
     * @param path path of target image
     */
    private int getImageSpinAngle(String path) {
        int degree = 0;
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(path);
        } catch (IOException e) {
            // 图片不支持获取角度
            return 0;
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degree = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degree = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degree = 270;
                break;
        }
        return degree;
    }

    /**
     * 指定参数压缩图片
     * create the thumbnail with the true rotate angle
     *
     * @param largeImagePath the big image path
     * @param thumbFilePath  the thumbnail path
     * @param width          width of thumbnail
     * @param height         height of thumbnail
     * @param angle          rotation angle of thumbnail
     * @param size           the file size of image
     */
    private File compress(String largeImagePath, String thumbFilePath, int width, int height,
                          int angle, long size) throws IOException {
        Bitmap thbBitmap = compress(largeImagePath, width, height);

        thbBitmap = rotatingImage(angle, thbBitmap);

        return saveImage(thumbFilePath, thbBitmap, size);
    }

    /**
     * 旋转图片
     * rotate the image with specified angle
     *
     * @param angle  the angle will be rotating 旋转的角度
     * @param bitmap target image               目标图片
     */
    private static Bitmap rotatingImage(int angle, Bitmap bitmap) {
        //rotate image
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        //create a new image
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                true);
    }

    /**
     * 保存图片到指定路径
     * Save image with specified size
     *
     * @param filePath the image file save path 储存路径
     * @param bitmap   the image what be save   目标图片
     * @param size     the file size of image   期望大小
     */
    private File saveImage(String filePath, Bitmap bitmap, long size) throws IOException {

        File result = new File(filePath.substring(0, filePath.lastIndexOf("/")));

        if (!result.exists() && !result.mkdirs()) {
            return null;
        }

        if (mByteArrayOutputStream == null) {
            mByteArrayOutputStream = new ByteArrayOutputStream(
                    bitmap.getWidth() * bitmap.getHeight());
        } else {
            mByteArrayOutputStream.reset();
        }

        int options = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, options, mByteArrayOutputStream);
        while (mByteArrayOutputStream.size() / 1024 > size && options > 6) {
            mByteArrayOutputStream.reset();
            options -= 6;
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, mByteArrayOutputStream);
        }
        bitmap.recycle();

        FileOutputStream fos = new FileOutputStream(filePath);
        mByteArrayOutputStream.writeTo(fos);
        fos.close();

        return new File(filePath);
    }
}