package com.appiphany.nacc.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.DisplayMetrics;
import android.util.Log;

public class BitmapUtil {

	//decodes image and scales it to reduce memory consumption
    public static Bitmap decodeFile(File f, Activity context){
    	try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);

            DisplayMetrics displaymetrics = new DisplayMetrics();
            context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int density = (int) displaymetrics.density;
            
            //The new size we want to scale to
            final int REQUIRED_SIZE=170 * density;

            //Find the correct scale value. It should be the power of 2.
            int scale=1;
            if (density == 1) {
            	scale = 8;
            } else if (density < 2) {
            	scale = 4;
            } else {
            	scale = 4;
            }

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            Ln.d("get sample size = " + scale);
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
    
    public static int getPhotoDegree(String photoPath) {
    	try {
    		ExifInterface ei = new ExifInterface(photoPath);
    		int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    		Ln.d("orientation = " + orientation);
    		
			switch (orientation) {
    		    case ExifInterface.ORIENTATION_ROTATE_90:
    		    	return 90;
    		    case ExifInterface.ORIENTATION_ROTATE_180:
    		    	return 180;
    		    case ExifInterface.ORIENTATION_ROTATE_270:
    		    	return 270;
		    	default:
    		    	return 0;
    		}
		} catch (IOException e) {
			return 0;
		}
    }
    
    public static int getPhotoGalleryDegree(String photoPath) {
    	try {
    		ExifInterface ei = new ExifInterface(photoPath);
    		int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    		Ln.d("orientation = " + orientation);
    		
    		switch (orientation) {
    		case ExifInterface.ORIENTATION_NORMAL:
    			return 90;
    		case ExifInterface.ORIENTATION_ROTATE_90:
    			return 90;
    		case ExifInterface.ORIENTATION_ROTATE_180:
    			return 90;
    		case ExifInterface.ORIENTATION_ROTATE_270:
    			return 90;
    		default:
    			return 0;
    		}
    	} catch (IOException e) {
    		return 0;
    	}
    }
    
}
