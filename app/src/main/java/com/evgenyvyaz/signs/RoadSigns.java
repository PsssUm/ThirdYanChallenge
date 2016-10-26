package com.evgenyvyaz.signs;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvRectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_ROUGH_SEARCH;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_FIND_BIGGEST_OBJECT;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_SCALE_IMAGE;

/**
 * Created by X550V on 21.10.2016.
 */

public class RoadSigns {
    private opencv_core.IplImage orgImg;
    private opencv_core.IplImage imgThreshold;
    private String signName;
    private String zebra = "haarcascade_zebra_new.xml";
    private String stop = "haarcascade_stop_sign.xml";
    private String single_road = "haarcascade_single_road.xml";
    private String train = "haarcascade_train_last.xml";
    private String prohibition = "haarcascade_movement_prohibition.xml";

    private opencv_objdetect.CascadeClassifier cascade;
    private OpenCVFrameConverter.ToMat.ToMat toMat = new OpenCVFrameConverter.ToMat();
    private ArrayList<Map<String, opencv_core.Rect>> allRects = new ArrayList<>();
    private OnFindObjectsListener onFindObjectsListener;

    public interface OnFindObjectsListener {
        public void onFinish(Bitmap photo, String signName);
    }

    private opencv_core.Size minSize = new opencv_core.Size(20, 20);

    public void setOnFindObjectsListener(OnFindObjectsListener onFindObjectsListener) {
        this.onFindObjectsListener = onFindObjectsListener;
    }

    public void convert(Activity activity, String pathToPhoto) throws FileNotFoundException {
        // opencv_core.IplImage orgImg = cvLoadImage(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/may.png");
        orgImg = cvLoadImage(pathToPhoto);

        imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);

        AndroidFrameConverter frameConverter = new AndroidFrameConverter();
        cvCvtColor(orgImg, imgThreshold, COLOR_RGB2GRAY);

        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/RoadSigns");
        System.out.println("folder = " + folder);
        if (!folder.exists()) {
            folder.mkdirs();
        }


        zebraSigns(saveFile(zebra, activity).getAbsolutePath());
         stopSigns(saveFile(stop, activity).getAbsolutePath());
         searchSignsSingleRoad(saveFile(single_road, activity).getAbsolutePath());
        searchSignsTrain(saveFile(train, activity).getAbsolutePath());
       searchSignsMovementProhibition(saveFile(prohibition, activity).getAbsolutePath());


        Frame frame1 = toMat.convert(orgImg);
        Bitmap bitmap1 = frameConverter.convert(frame1);
        onFindObjectsListener.onFinish(bitmap1, signName);




}
    private File saveFile(String cascadeName, Activity activity){
        File mCascadeFile = null;
        try {
            // load cascade file from application resources
            InputStream is = activity.getAssets().open(cascadeName);
            File cascadeDir = activity.getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, cascadeName);
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mCascadeFile;
    }



    public void zebraSigns(String pathXml) {

        cascade = new opencv_objdetect.CascadeClassifier(pathXml);
        cascade.load(pathXml);
        opencv_core.RectVector sign = new opencv_core.RectVector();

        System.out.println("true = ");
        //cascade.detectMultiScale(img, signs, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING, Size(30, 30), Size(200, 200));
        cascade.detectMultiScale(toMat.convert(toMat.convert(imgThreshold)), sign, 1.27, 6, CV_HAAR_DO_ROUGH_SEARCH & CV_HAAR_FIND_BIGGEST_OBJECT, minSize, null);

        //cvHaarDetectObjects()
        if (sign.size() != 0) {
            putInRectArray("Пешеходный переход", sign);
        }
        System.out.println("zebra size = " + sign.size());

    }

    public void stopSigns(String pathXml) {
        opencv_core.RectVector sign = new opencv_core.RectVector();
        cascade = new opencv_objdetect.CascadeClassifier(pathXml);
        cascade.load(pathXml);
        System.out.println("true = ");
        //cascade.detectMultiScale(img, signs, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING, Size(30, 30), Size(200, 200));
        cascade.detectMultiScale(toMat.convert(toMat.convert(imgThreshold)), sign, 1.25, 6, CV_HAAR_SCALE_IMAGE, minSize, null);
        //cvHaarDetectObjects()
        if (sign.size() != 0) {
            putInRectArray("Въезд запрещён", sign);
        }
        System.out.println("stop_sign size = " + sign.size());

    }

    public void searchSignsSingleRoad(String pathXml) {

        cascade = new opencv_objdetect.CascadeClassifier(pathXml);
        cascade.load(pathXml);
        opencv_core.RectVector sign = new opencv_core.RectVector();
        System.out.println("true = ");
        //cascade.detectMultiScale(img, signs, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING, Size(30, 30), Size(200, 200));
        cascade.detectMultiScale(toMat.convert(toMat.convert(imgThreshold)), sign, 1.27, 6, CV_HAAR_FIND_BIGGEST_OBJECT, minSize, null);
        //cvHaarDetectObjects()
        if (sign.size() != 0) {
            putInRectArray("Дорога с односторонним движением", sign);
        }
        System.out.println("single_road size = " + sign.size());

    }

    public void searchSignsMovementProhibition(String pathXml) {

        cascade = new opencv_objdetect.CascadeClassifier(pathXml);
        cascade.load(pathXml);
        opencv_core.RectVector sign = new opencv_core.RectVector();
        System.out.println("true = ");

        //cascade.detectMultiScale(img, signs, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING, Size(30, 30), Size(200, 200));
        cascade.detectMultiScale(toMat.convert(toMat.convert(imgThreshold)), sign, 1.42, 6, CV_HAAR_DO_ROUGH_SEARCH & CV_HAAR_FIND_BIGGEST_OBJECT, minSize, null);
        //cvHaarDetectObjects()
        if (sign.size() != 0) {
            putInRectArray("Движение запрещено", sign);
        }
        System.out.println("movement_prohibition size = " + sign.size());
        drawRectangle();
    }

    public void searchSignsTrain(String pathXml) {
        cascade = new opencv_objdetect.CascadeClassifier(pathXml);
        cascade.load(pathXml);
        opencv_core.RectVector sign = new opencv_core.RectVector();
        System.out.println("true = ");
        //cascade.detectMultiScale(img, signs, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING, Size(30, 30), Size(200, 200));
        cascade.detectMultiScale(toMat.convert(toMat.convert(imgThreshold)), sign, 1.3, 7, CV_HAAR_DO_CANNY_PRUNING, minSize, null);
        if (sign.size() != 0) {
            putInRectArray("Железнодорожный переезд без шлагбаума", sign);
        }
        // cascade.detectMultiScale(toMat.convert(toMat.convert(imgThreshold)), signs,);
        //cvHaarDetectObjects()
        System.out.println("train_new size = " + sign.size());

    }

    private void putInRectArray(String signName, opencv_core.RectVector sign) {
        for (int i = 0; i < sign.size(); i++) {
            System.out.println("put in array cicle");
            Map<String, opencv_core.Rect> currentMap = new HashMap<>();
            currentMap.put(signName, sign.get(i));
            allRects.add(currentMap);
        }
    }

    private void drawRectangle() {

        Map<Integer, Map<String, opencv_core.Rect>> vectorMap = new HashMap<>();
        int maxSquare = 0;

        System.out.println("allRects size = " + allRects.size());
        for (int i = 0; i < allRects.size(); i++) {
            for (String key : allRects.get(i).keySet()) {
                int square = allRects.get(i).get(key).height() * allRects.get(i).get(key).width();
                vectorMap.put(square, allRects.get(i));

                if (maxSquare == 0) {
                    maxSquare = square;
                } else {
                    if (maxSquare <= square) {
                        maxSquare = square;
                    }
                }
            }


        }
        for (int key : vectorMap.keySet()) {

            if (key >= maxSquare * 3 / 5) {
                for (String name : vectorMap.get(key).keySet()) {
                    opencv_core.Rect r = vectorMap.get(key).get(name);
                    cvRectangle(orgImg, new opencv_core.CvPoint(r.x(), r.y()), new opencv_core.CvPoint(r.x() + r.width(), r.y() + r.height()), new opencv_core.CvScalar(0, 0, 255, 0), 2, 8, 0);
                    if (signName != null && !signName.contains(name)) {
                        signName = name + ", " + name;
                    } else {
                        signName = name;
                    }
                }
            }
        }


    }
}

