package com.idreader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.app.Application;
import android.os.Environment;
import android.util.Log;
import android.util.Base64;
import android.graphics.Color;
import android.graphics.PixelFormat;


import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.react.common.annotations.VisibleForTesting;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.soloader.SoLoader;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.leptonica.android.WriteFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.lang.Math;
import java.lang.Thread;
import java.lang.Runnable;

public class IdReaderCoreModule extends ReactContextBaseJavaModule {

    @VisibleForTesting
    private static final String REACT_CLASS = "RNIdReaderCore";
    private static String DATA_PATH = Environment.getExternalStorageDirectory().toString() + File.separator;
    private TessBaseAPI tessBaseApi;

    public IdReaderCoreModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "IdReaderCore";
    }

    @ReactMethod
    public void read(int left, int top, int width, int height, Callback callback) {
//        new Thread(new Runnable() {
//            public void run() {
//
//            }
//        }).start();
        String res = read(left, top, width, height);
        callback.invoke(res);
    }

    public String read(int left, int top, int width, int height) {
        Log.d(REACT_CLASS, "Read left=" + left + ", top=" + top + ", width=" + width + ", height=" + height);
        String extractedText = "Empty result";
        try {
            this.tessBaseApi = new TessBaseAPI();
            tessBaseApi.init(DATA_PATH, "kat2");

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; //inSampleSize documentation --> http://goo.gl/KRrlvi
            Bitmap bitmap = BitmapFactory.decodeFile(DATA_PATH + "tessdata/id.jpg", options);
            int rectangleLeft = bitmap.getWidth() * left / 100;
            int rectangleTop = bitmap.getHeight() * top / 100;
            int rectangleWidth = bitmap.getWidth() * width / 100;
            int rectangleHeight = bitmap.getHeight() * height / 100;
            bitmap = Bitmap.createBitmap(bitmap, rectangleLeft, rectangleTop, rectangleWidth, rectangleHeight);
//            bitmap = setGrayscale(bitmap);
//            bitmap = removeNoise(bitmap);

            tessBaseApi.setImage(bitmap);

            Log.d(REACT_CLASS, "Image set. resolutions");
            Log.d(REACT_CLASS, bitmap.getHeight() + "x" + bitmap.getWidth());

            /************************************************
             0    Orientation and script detection (OSD) only.
             1    Automatic page segmentation with OSD.
             2    Automatic page segmentation, but no OSD, or OCR.
             3    Fully automatic page segmentation, but no OSD. (Default)
             4    Assume a single column of text of variable sizes.
             5    Assume a single uniform block of vertically aligned text.
             6    Assume a single uniform block of text.
             7    Treat the image as a single text line.
             8    Treat the image as a single word.
             9    Treat the image as a single word in a circle.
             10    Treat the image as a single character.
             11    Sparse text. Find as much text as possible in no particular order.
             12    Sparse text with OSD.
             13    Raw line. Treat the image as a single text line,
             *************************************************/

            // tessBaseApi.setPageSegMode(7); // single line
            tessBaseApi.setPageSegMode(8); // single world


            // Log.d(REACT_CLASS, "Rectangle dimensions " + rectangleLeft + " " + rectangleTop + " " + rectangleWidth + " " + rectangleHeight);
            // tessBaseApi.setRectangle(rectangleLeft, rectangleTop, rectangleWidth, rectangleHeight);

            try {
                extractedText = tessBaseApi.getUTF8Text();
                String[] lines = extractedText.split(System.getProperty("line.separator"));

                // Bitmap bitmapTheshold = WriteFile.writeBitmap(tessBaseApi.getRegions().getPix(0));
                Bitmap bitmapTheshold = WriteFile.writeBitmap(tessBaseApi.getThresholdedImage());

                extractedText = lines[0] + ", confidence=" + tessBaseApi.wordConfidences()[0];

                Date date = new Date();
                save(bitmapTheshold, "tessdata/" + date.getTime() + ".png");
                //  Log.d(REACT_CLASS, Base64.encodeToString(data, Base64.DEFAULT));

                Log.d(REACT_CLASS, "##### Extracted data start");
                Log.d(REACT_CLASS, extractedText);
                Log.d(REACT_CLASS, "##### Extracted data end");
            } catch (Exception e) {
                Log.d(REACT_CLASS, e.getMessage());
            }

            tessBaseApi.end();
            Log.d(REACT_CLASS, "Tessbase end");
        } catch (Exception e) {
            Log.e(REACT_CLASS, e.getMessage());
        }
        return extractedText;
    }

    public void save(Bitmap bmp, String filename) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(DATA_PATH + filename);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            Log.d(REACT_CLASS, "file saved" + DATA_PATH + filename);
        } catch (Exception e) {
            Log.d(REACT_CLASS, e.getMessage());
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.d(REACT_CLASS, e.getMessage());
            }
        }
    }

    // SetGrayscale
    private Bitmap setGrayscale(Bitmap img) {
        Bitmap bmap = img.copy(img.getConfig(), true);
        int c;
        for (int i = 0; i < bmap.getWidth(); i++) {
            for (int j = 0; j < bmap.getHeight(); j++) {
                c = bmap.getPixel(i, j);
                byte gray = (byte) (.299 * Color.red(c) + .587 * Color.green(c)
                        + .114 * Color.blue(c));

                bmap.setPixel(i, j, Color.argb(255, gray, gray, gray));
            }
        }
        return bmap;
    }

    // RemoveNoise
    private Bitmap removeNoise(Bitmap bmap) {
        for (int x = 0; x < bmap.getWidth(); x++) {
            for (int y = 0; y < bmap.getHeight(); y++) {
                int pixel = bmap.getPixel(x, y);
                if (Color.red(pixel) < 162 && Color.green(pixel) < 162 && Color.blue(pixel) < 162) {
                    bmap.setPixel(x, y, Color.BLACK);
                }
            }
        }
        for (int x = 0; x < bmap.getWidth(); x++) {
            for (int y = 0; y < bmap.getHeight(); y++) {
                int pixel = bmap.getPixel(x, y);
                if (Color.red(pixel) > 162 && Color.green(pixel) > 162 && Color.blue(pixel) > 162) {
                    bmap.setPixel(x, y, Color.WHITE);
                }
            }
        }
        return bmap;
    }
}