package com.appiphany.nacc.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.screens.BackgroundService;
import com.appiphany.nacc.ui.controls.DatePreference;
import com.appiphany.nacc.ui.controls.TimePreference;

/**
 * Utilities class for UI.
 * 
 */
public class UIUtils {
	private static final String TAG = "CameraConfiguration";
	
	private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
	private static final int MAX_PREVIEW_PIXELS = 800 * 600; // more than large/HD
	
    public static void registerReminder(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean isReminderEnable = sharedPrefs.getBoolean("reminder_enable", false);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, BackgroundService.class);
        alarmIntent.setAction(BackgroundService.REMINDER_ACTION);
        PendingIntent alarmPendingIntent = PendingIntent.getService(context,
                4, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        if (isReminderEnable) {
            Calendar reminderDate = DatePreference.getDateFor(sharedPrefs, "reminder_date");
            Calendar reminderTime = TimePreference.getTimeFor(sharedPrefs, "reminder_time");
            String reminderFreq = sharedPrefs.getString("reminder_frequency", null);
            if (reminderDate != null && reminderTime != null && reminderFreq != null) {
                reminderDate.set(Calendar.HOUR_OF_DAY, reminderTime.get(Calendar.HOUR_OF_DAY));
                reminderDate.set(Calendar.MINUTE, reminderTime.get(Calendar.MINUTE));
                long alarmTime = reminderDate.getTimeInMillis();
                long repeatingTime = System.currentTimeMillis();

                switch (Integer.parseInt(reminderFreq)) {
                case 0://YEAR
                    reminderDate.add(Calendar.YEAR, 1);
                    break;
                case 1://6months
                    reminderDate.add(Calendar.MONTH, 6);
                    break;
                case 2://3months
                    reminderDate.add(Calendar.MONTH, 3);
                    break;
                case 3://1month
                    reminderDate.add(Calendar.MONTH, 1);
                    break;
                case 4://2weeks
                    reminderDate.add(Calendar.WEEK_OF_YEAR, 2);
                    break;
                case 5://Week
                    reminderDate.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                }
                repeatingTime = reminderDate.getTimeInMillis() - System.currentTimeMillis();
                while (repeatingTime < 0) {
                    repeatingTime += reminderDate.getTimeInMillis();
                }

                Ln.d("IS REMINDER ENABLE " + isReminderEnable + " " + alarmTime + " "
                                + repeatingTime);
                while (alarmTime < System.currentTimeMillis()) {
                    alarmTime += repeatingTime;
                }
                alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime,
                        repeatingTime,
                        alarmPendingIntent);
            }
        } else {
            alarmMgr.cancel(alarmPendingIntent);
        }
    }

    public static Site getBestSite(List<Site> sites, Location userLocation, Context context) {
        Site result = null;
        float distance;
        if (Config.isDemoMode(context)) {
            distance = Config.LOCATION_DISTANCE;
        } else {
            distance = Float.MAX_VALUE;
        }

        if (userLocation == null) {
        	return null;
        }
        
        if (sites == null || sites.size() == 0) {
        	return null;
        }
        
        for (Site site : sites) {
            float[] resultValue = new float[1];
            Location.distanceBetween(site.getLat(), site.getLng(), userLocation.getLatitude(),
                    userLocation.getLongitude(), resultValue);

            if (resultValue[0] <= distance) {
                distance = resultValue[0];
                result = site;
            }
        }

        return result;
    }
    
    public static Site getBestSiteForGuide(List<Site> sites, Location userLocation, Context context) {
    	Site result = null;
    	float distance = Config.LOCATION_DISTANCE;
    	
    	for (Site site : sites) {
    		Location location = new Location(LocationManager.GPS_PROVIDER);
    		location.setLatitude(site.getLat());
    		location.setLongitude(site.getLng());
    		float[] resultValue = new float[1];
    		if (userLocation == null) {
    			return null;
    		}
    		Location.distanceBetween(site.getLat(), site.getLng(), userLocation.getLatitude(),
    				userLocation.getLongitude(), resultValue);

    		if (resultValue[0] <= distance) {
    			distance = resultValue[0];
    			result = site;
    		}
    	}
    	
    	return result;
    }

	public static Site findNearestSite(List<Site> sites, Location userLocation) {
    	if(sites == null || userLocation == null) {
    		return null;
		}

		for (Site site : sites) {
			Location siteLocation = new Location(LocationManager.GPS_PROVIDER);
			siteLocation.setLatitude(site.getLat());
			siteLocation.setLongitude(site.getLng());

			double distance = userLocation.distanceTo(siteLocation);

			if (distance <= Config.LOCATION_NEAREST_DISTANCE) {
				return site;
			}
		}

		return null;
	}

    public static String getPhotoDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String result = "";
        if (date != null) {
            result = sdf.format(date);
        }
        return result;
    }

    public static Bitmap convertByteArrayToBitmap(byte[] data, int mCameraDegree, int maxWidth, int maxHeight) {
        Bitmap result = null;
        if (data != null) {
            Options bitmapOptions = new Options();

            bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmapOptions.inSampleSize = 2;
            Bitmap oriImage = BitmapFactory.decodeByteArray(data, 0, data.length, bitmapOptions);
            if (mCameraDegree != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(mCameraDegree);
                result = Bitmap.createBitmap(oriImage, 0, 0, oriImage.getWidth(), oriImage.getHeight(), matrix, false);
                oriImage.recycle();

            } else {
                result = oriImage;
            }
        }
        return result;
    }

    /**
     * Build a simple alert dialog.
     *
     */
    public static AlertDialog buildAlertDialog(Context context, int titleId, int messageId, boolean isCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);
        builder.setMessage(messageId);
        builder.setNeutralButton("OK", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        if (isCancel) {
            builder.setCancelable(true);
        }
        return builder.create();
    }
    
    public static AlertDialog showAlertDialog(final Context context, int titleId, final int messageId, boolean isCancel, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);
        builder.setMessage(messageId);
        builder.setNeutralButton("OK",listener);
        if (isCancel) {
            builder.setCancelable(true);
        }
        return builder.create();
    }

    public static boolean isEmailValid(String input) {
        String regEx = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        return input.matches(regEx);
    }

    public static boolean isStringEmpty(String input) {
        return (input == null || input.trim().equals(""));
    }

    public static void showKeyBoard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static void hideKeyboard(Context context, EditText edittext) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);
    }
    
    public static void hideSoftKeyboard(Activity activity) {
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (activity.getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
		}
	}
    
    public static Site getAvailableSiteByGuide(Context context, Location location, List<String> siteIdInGuide, List<Site> sites) {
    	if (siteIdInGuide == null || siteIdInGuide.size() == 0) {
    		return null;
    	}
    	
    	if (sites == null || sites.size() == 0) {
    		return null;
    	}
    	
    	List<Site> selectedSite = new ArrayList<Site>();
    	for (String siteId : siteIdInGuide) {
    		for (Site site : sites) {
    			if (site.getSiteId().equalsIgnoreCase(siteId)) {
    				selectedSite.add(site);
    			}
    		}
    	}
    	
    	return getBestSiteForGuide(selectedSite, location, context);
    }
    
    @SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static Point getScreenResolution(Context context){
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return new Point(size.x, size.y);
	}
    
    public static Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

		// Sort by size, descending
				List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes());
				Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
					@Override
					public int compare(Camera.Size a, Camera.Size b) {
						int aPixels = a.height * a.width;
						int bPixels = b.height * b.width;
						if (bPixels < aPixels) {
							return -1;
						}
						if (bPixels > aPixels) {
							return 1;
						}
						return 0;
					}
				});

				if (Log.isLoggable(TAG, Log.INFO)) {
					StringBuilder previewSizesString = new StringBuilder();
					for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
						previewSizesString.append(supportedPreviewSize.width).append('x').append(supportedPreviewSize.height).append(' ');
					}

					Ln.d("Supported preview sizes: " + previewSizesString);
				}

				Point bestSize = null;
				float screenAspectRatio = (float) screenResolution.x / (float) screenResolution.y;

				float diff = Float.POSITIVE_INFINITY;
				for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
					int realWidth = supportedPreviewSize.width;
					int realHeight = supportedPreviewSize.height;
					int pixels = realWidth * realHeight;
					if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
						continue;
					}
					
					boolean isCandidatePortrait = realWidth < realHeight;
					int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
					int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
					if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
						Point exactPoint = new Point(realWidth, realHeight);
						Ln.d("Found preview size exactly matching screen size: " + exactPoint);
						return exactPoint;
					}
					float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
					float newDiff = Math.abs(aspectRatio - screenAspectRatio);
					if (newDiff < diff) {
						bestSize = new Point(realWidth, realHeight);
						diff = newDiff;
					}
				}

				if (bestSize == null) {
					Camera.Size defaultSize = parameters.getPreviewSize();
					bestSize = new Point(defaultSize.width, defaultSize.height);
					Ln.d("No suitable preview sizes, using default: " + bestSize);
				}

				Ln.d("Found best approximate preview size: " + bestSize);
				return bestSize;
	}
    
    private static String findSettableValue(Collection<String> supportedValues, String... desiredValues) {
		Ln.d("Supported values: " + supportedValues);
		String result = null;
		if (supportedValues != null) {
			for (String desiredValue : desiredValues) {
				if (supportedValues.contains(desiredValue)) {
					result = desiredValue;
					break;
				}
			}
		}

		Ln.d("Settable value: " + result);
		return result;
	}
    
    private static Camera.Parameters doSetTorch(Camera.Parameters parameters, boolean newSetting, boolean safeMode) {
		String flashMode;
		if (newSetting) {
			flashMode = findSettableValue(parameters.getSupportedFlashModes(), Camera.Parameters.FLASH_MODE_ON);
		} else {
			flashMode = findSettableValue(parameters.getSupportedFlashModes(), Camera.Parameters.FLASH_MODE_OFF);
		}
		if (flashMode != null) {
			parameters.setFlashMode(flashMode);
		}
		
		return parameters;
	}
    
    public static Camera.Parameters setTorch(Camera.Parameters parameters, boolean newSetting) {
    	return doSetTorch(parameters, newSetting, false);
	}
    
	public static void sendEmail(Activity context, String filePath, String emailTarget, int requestCode) {
		try {
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			String subject = "Report bug";
			DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
			String asGmt = df.format(new Date());
			String emailtext = "Bug logged on " + asGmt + " \n ";
			String fileContent = readTextFile(filePath);
			emailtext += fileContent;
			emailIntent.setType("plain/text");

			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { emailTarget });

			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);

			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailtext);

			context.startActivityForResult(Intent.createChooser(emailIntent, "Send mail..."), requestCode);

		} catch (Exception t) {
			t.printStackTrace();
		}

	}
	
	public static String readTextFile(String filePath){
		StringBuilder text = new StringBuilder();
		BufferedReader br =null;
		try {
		    br = new BufferedReader(new FileReader(filePath));
		    String line;

		    while ((line = br.readLine()) != null) {
		        text.append(line);
		        text.append('\n');
		    }
		    
		    return text.toString();
		}
		catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return "Can't read log file";
	}

	public static void showToast(Context context, String message){
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
}
