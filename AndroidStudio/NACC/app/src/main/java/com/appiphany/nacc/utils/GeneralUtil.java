package com.appiphany.nacc.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.Days;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Environment;

public class GeneralUtil {
	public static final String SIMPLE_DATE_PATTERN = "HH:mm dd/MM/yyyy";
	public static final int MIN_UPDATE_DAY = 1;
	private static DisplayImageOptions displayScaleOption;
	
	public static boolean isDebugMode(){
		try{
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				File debugKey = new File(Environment.getExternalStorageDirectory() + "/" + "nacc.debug");
				return debugKey.exists();
			}
		}catch(Exception ex){
			
		}
		return false;
	}
	
	public static boolean shouldSyncGuideImage(Context context) {
		String lastSync = Config.getLastSync(context);
		if (stringIsBlank(lastSync)) {
			return true;
		}

		String currentDate = dateToString(new Date(), SIMPLE_DATE_PATTERN);
		int differenceDays = differenceOfDays(lastSync, currentDate, SIMPLE_DATE_PATTERN);
		Ln.d("difference days: " + differenceDays);

		if (differenceDays < MIN_UPDATE_DAY) {
			return false;
		}

		return true;
	}
	
	public static boolean stringIsBlank(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return true;
		}

		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(str.charAt(i)) == false)) {
				return false;
			}
		}

		return true;
	}
	
	public static String dateToString(Date date, String pattern) {
		DateFormat df = new SimpleDateFormat(pattern, Locale.getDefault());
		return df.format(date);
	}
	
	public static int differenceOfDays(String strDate1, String strDate2, String pattern) {
		DateFormat df = new SimpleDateFormat(pattern, Locale.getDefault());
		try {
			Date date1 = df.parse(strDate1);
			Date date2 = df.parse(strDate2);
			int numberOfDays = Days.daysBetween(new DateTime(date1).toLocalDate(), new DateTime(date2).toLocalDate()).getDays();
			return numberOfDays;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Integer.MAX_VALUE;
	}
	
	public static DisplayImageOptions getScaleDisplayOption() {
		if (displayScaleOption == null) {
			displayScaleOption = new DisplayImageOptions.Builder()
			.cacheInMemory(true).cacheOnDisk(true).considerExifParams(true)
			.imageScaleType(ImageScaleType.EXACTLY)
			.resetViewBeforeLoading(true).build();
		}
		
		return displayScaleOption;
	}
	
	public static DisplayImageOptions getNewScaleOption(){
    	return new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true).imageScaleType(ImageScaleType.EXACTLY).considerExifParams(true)
				.resetViewBeforeLoading(true).build();
    }
	
	
	public static void saveBitmap(byte[] bitmapData, String fileName) throws Exception {
		File file = new File(fileName);
		FileOutputStream fos;
		BufferedOutputStream bos = null;
		try {
			final int bufferSize = 1024 * 4;
			fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos, bufferSize);
			bos.write(bitmapData);
			bos.flush();
		} catch (Exception ex) {
			throw ex;
		} finally {
			if (bos != null) {
				bos.close();
			}
		}
	}
	
	public static void saveBitmap(Bitmap bitmap, String fileName) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

		File f = new File(fileName);
		if(f.exists()){
			f.delete();
		}
		
		f.createNewFile();
		// write the bytes in file
		FileOutputStream fo = new FileOutputStream(f);
		fo.write(bytes.toByteArray());

		// close the FileOutput
		fo.close();

	}
	
	public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
		OutputStream out = null;
		try {
			out = openOutputStream(file);
			out.write(data);
		} finally {
			closeQuietly(out);
		}
	}

	public static FileOutputStream openOutputStream(File file) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				throw new IOException("File '" + file + "' exists but is a directory");
			}
			if (file.canWrite() == false) {
				throw new IOException("File '" + file + "' cannot be written to");
			}
		} else {
			File parent = file.getParentFile();
			if (parent != null && parent.exists() == false) {
				if (parent.mkdirs() == false) {
					throw new IOException("File '" + file + "' could not be created");
				}
			}
		}
		return new FileOutputStream(file);
	}

	public static void closeQuietly(OutputStream output) {
		try {
			if (output != null) {
				output.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}
	
	public static boolean saveAndRotateBitmap(byte[] bitmapData, int rotate, String imagePath, Location location) throws Exception {
		Boolean saveSuccess = false;		
		try {
			writeByteArrayToFile(new File(imagePath), bitmapData);
			if(rotate != 0){
				Matrix matrix = new Matrix();
				matrix.setRotate(rotate);
				DisplayImageOptions option = new DisplayImageOptions.Builder().considerExifParams(false)
						.bitmapConfig(android.graphics.Bitmap.Config.RGB_565).cacheInMemory(false).cacheOnDisc(false).build();
				Bitmap original = ImageLoader.getInstance().loadImageSync("file:///" + imagePath, new ImageSize(Config.CONFIG_WIDTH, Config.CONFIG_HEIGHT), option);
				Ln.d("size: " + original.getWidth() + " , " + original.getHeight());
				Bitmap cleaned = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
				Ln.d("size: " + cleaned.getWidth() + " , " + cleaned.getHeight());
				original.recycle();
				original = null;
				saveBitmap(cleaned, imagePath);
				cleaned.recycle();
				cleaned = null;
			}

			if(location != null) {
				loc2Exif(imagePath, location);
			}

			saveSuccess = true;
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			System.gc();
		}catch (Exception ex){
			ex.printStackTrace();
		}

		return saveSuccess;
	}

    private static void loc2Exif(String flNm, Location loc) {
        try {
            ExifInterface ef = new ExifInterface(flNm);
            ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE, dec2DMS(loc.getLatitude()));
            ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, dec2DMS(loc.getLongitude()));
            if (loc.getLatitude() > 0)
                ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
            else
                ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
            if (loc.getLongitude()>0)
                ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
            else
                ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");

            SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            String date = format.format(new Date());
            ef.setAttribute(ExifInterface.TAG_DATETIME, date);

            ef.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String dec2DMS(double coord) {
        coord = coord > 0 ? coord : -coord;  // -105.9876543 -> 105.9876543
        String sOut = Integer.toString((int)coord) + "/1,";   // 105/1,
        coord = (coord % 1) * 60;         // .987654321 * 60 = 59.259258
        sOut = sOut + Integer.toString((int)coord) + "/1,";   // 105/1,59/1,
        coord = (coord % 1) * 60000;             // .259258 * 60000 = 15555
        sOut = sOut + Integer.toString((int)coord) + "/1000";   // 105/1,59/1,15555/1000
        return sOut;
    }

	public static int getOrientationFromRatation(int rotate){
		int tagOrientation = ExifInterface.ORIENTATION_NORMAL;
        switch (rotate) {
        case 90:
            tagOrientation = ExifInterface.ORIENTATION_ROTATE_90;
            break;
        case 180:
            tagOrientation = ExifInterface.ORIENTATION_ROTATE_180;
            break;
        case 270:
            tagOrientation = ExifInterface.ORIENTATION_ROTATE_270;
            break;
        }
        
        return tagOrientation;
	}
	
	public static String getLogFilePath(Context context){
		return new File(context.getFilesDir() , "nacc_error_log.txt").getAbsolutePath();
	}
	
	public static boolean hasLogFile(Context context){
		return new File(context.getFilesDir() , "nacc_error_log.txt").exists();
	}
	
	public static boolean deleteLogFile(Context context){
		return new File(context.getFilesDir() , "nacc_error_log.txt").delete();
	}

	public static double parseDouble(String val) {
		try{
			return Double.parseDouble(val);
		}catch(Throwable e){
			return 0;
		}
	}
}
