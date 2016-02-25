package com.appiphany.nacc.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Project;
import com.appiphany.nacc.model.ProjectResult;
import com.appiphany.nacc.model.Site;
import com.crashlytics.android.Crashlytics;
import com.flurry.org.codehaus.jackson.map.util.ISO8601Utils;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;

public class NetworkUtils {
    public static boolean isNetworkOnline(Context context) {
        boolean status = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                status = true;
            } else {
                netInfo = cm.getNetworkInfo(1);
                if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED)
                    status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return status;

    }

    public static boolean isGPSAvailable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Activity.LOCATION_SERVICE);

        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGPSEnabled && isNetworkEnabled) {
            // network provider is enabled
            return true;
        }
        return false;
    }
    
	public static boolean isServerAvailable(String serverUrl) {
		HttpURLConnection urlc = null;
		try {
			URL url = new URL(serverUrl);
			urlc = (HttpURLConnection) url.openConnection();
			urlc.setRequestProperty("Connection", "close");
			urlc.setConnectTimeout(3000); // Timeout 3 seconds.
			urlc.connect();

			if (urlc.getResponseCode() == 200) // Successful response.
			{
				return true;
			} else {

				return false;
			}
		} catch (Exception ex) {
			return false;
		}finally{
			if(urlc != null){
				urlc.disconnect();				
			}
		}
	}
    
    public static List<Site> getAllSite(Context context, String project) {
		if(TextUtils.isEmpty(project)){
			return null;
		}

		try {
            String url = Uri.parse(Config.getActiveServer(context) + "sites.json").buildUpon().
                    appendQueryParameter("project_id", project).build().toString();
			HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
			HttpRequest httpRequest = httpTransport.createRequestFactory().buildGetRequest(new GenericUrl(url));
			httpRequest.setConnectTimeout(Config.HTTP_CONNECT_TIMEOUT);
			httpRequest.setNumberOfRetries(10);
			httpRequest.setRetryOnExecuteIOException(true);
			HttpResponse httpResponse = httpRequest.execute();
			if (httpResponse != null && httpResponse.getStatusCode() == 200) {
				InputStream is = httpResponse.getContent();
				InputStreamReader reader = new InputStreamReader(is);
				JsonParser parser = new JsonParser();
				JsonElement jsonElement = parser.parse(reader);
				JsonArray sitesArray = jsonElement.getAsJsonArray();
				List<Site> result = new ArrayList<Site>();
				for (JsonElement element : sitesArray) {
					JsonObject siteObject = element.getAsJsonObject();
					String siteId = siteObject.get("ID").getAsString();
					String siteName = siteObject.get("Name").getAsString();
					double lat = siteObject.get("Latitude").getAsDouble();
					double lng = siteObject.get("Longitude").getAsDouble();
					String projectId = siteObject.get("ProjectId").getAsString();
					Site site = new Site(siteId, siteName, lat, lng, projectId);
					result.add(site);
				}

				return result;
			}
		} catch (Exception ex) {
			Ln.e(ex);
            Crashlytics.setString("getAllSite project id", project);
			Crashlytics.logException(ex);

		}

		return null;

	}
    
	public static List<Photo> getGuidePhotos(Context context, String project) {
		try {
			HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
			String url = Uri.parse(Config.getActiveServer(context) + "photos.json").buildUpon().
                    appendQueryParameter("access_token", Config.getAccessToken(context))
                    .appendQueryParameter("project_id", project).build().toString();
			HttpRequest httpRequest = httpTransport.createRequestFactory().buildGetRequest(new GenericUrl(url));

			Ln.d("server url: %s, access_token = %s", url, Config.getAccessToken(context));
			httpRequest.setConnectTimeout(30000);
			HttpResponse httpResponse = httpRequest.execute();
			if (httpResponse != null && httpResponse.getStatusCode() == 200) {
				InputStream is = httpResponse.getContent();
				InputStreamReader reader = new InputStreamReader(is);
				Type listType = new TypeToken<List<Photo>>() {
				}.getType();
				GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
					@Override
					public Date deserialize(JsonElement json, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
						try {
							return ISO8601Utils.parse(json.getAsString());
						} catch (Exception e) {
							return null;
						}
					}

				});

				Gson gson = gsonBuilder.create();

				List<Photo> result = gson.fromJson(reader, listType);
				return result;
			}
		} catch (Exception ex) {
			Ln.e(ex);
            Crashlytics.setString("getGuidePhotos project id", project);
            Crashlytics.logException(ex);
		}
		
		return null;

	}
	
	public static List<Project> getProjects(String serverUrl, String accessToken){
		try{
            if(!serverUrl.endsWith("/")){
                serverUrl += "/";
            }

			com.appiphany.nacc.utils.HttpRequest request = com.appiphany.nacc.utils.HttpRequest.get(serverUrl + "projects?access_token=" + accessToken);
			String json = request.body();
			ProjectResult result = new Gson().fromJson(json, ProjectResult.class);
			return result.getProjects();			
		}catch(Exception ex){
			ex.printStackTrace();
            Crashlytics.setString("getProjects serverUrl", serverUrl);
            Crashlytics.logException(ex);
		}
		
		return new ArrayList<>();
	}

}
