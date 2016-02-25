package com.appiphany.nacc.ui.controls;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.appiphany.nacc.utils.BitmapUtil;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;

public class NaccBitmapDisplayer implements BitmapDisplayer {

	@Override
	public Bitmap display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
		if (bitmap != null) {
            if (imageAware.getWrappedView().getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams lp = (LayoutParams) imageAware.getWrappedView().getLayoutParams();
                if (bitmap.getWidth() < bitmap.getHeight()) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                    lp.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
                } else {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                    lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                }
                imageAware.getWrappedView().setLayoutParams(lp);
            }
            
            Matrix matrix = new Matrix();
            
            if(imageAware.getWrappedView() != null && imageAware.getWrappedView().getTag() != null 
            		&& imageAware.getWrappedView().getTag() instanceof String){
            	String imagePath = (String) imageAware.getWrappedView().getTag();
            	int degree = BitmapUtil.getPhotoDegree(imagePath);
            	matrix.postRotate(degree);
            }else{
            	matrix.postRotate(90);
            }            
    		
    		Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            imageAware.setImageBitmap(rotatedBitmap);
        }
        return bitmap;
	}
}
