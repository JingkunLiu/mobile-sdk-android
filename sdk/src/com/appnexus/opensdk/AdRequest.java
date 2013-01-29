/**
 * 
 */
package com.appnexus.opensdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;

/**
 * @author jacob
 * 
 */
public class AdRequest extends AsyncTask<Void, Integer, AdResponse> {

	AdView owner;
	String hidmd5;
	String hidsha1;
	String devMake;
	String devModel;
	String carrier;
	String firstlaunch;
	String lat;
	String lon;
	String ua;
	String orientation;
	int width = -1;
	int height = -1;
	int maxWidth = -1;
	int maxHeight = -1;

	/**
	 * 
	 */
	public AdRequest(AdView owner) {
		this.owner = owner;
		Context context = owner.getContext();
		String aid = android.provider.Settings.Secure.getString(
				context.getContentResolver(), "android_id");

		// Do we have access to location?
		if (owner.getContext().checkCallingOrSelfPermission(
				"android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED
				|| owner.getContext().checkCallingOrSelfPermission(
						"android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
			// Get lat, long from any GPS information that might be currently
			// available
			LocationManager lm = (LocationManager) owner.getContext()
					.getSystemService(Context.LOCATION_SERVICE);
			Location lastLocation = lm.getLastKnownLocation(lm.getBestProvider(
					new Criteria(), false));
			lat = lastLocation != null ? "" + lastLocation.getLatitude() : null;
			lon = lastLocation != null ? "" + lastLocation.getLongitude()
					: null;
		}else{
			Clog.w("OPENSDK", "Permissions for location aren't set in the host app. This may affect demand.");
		}
		// Get orientation, the current rotation of the device
		orientation = owner.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? "landscape"
				: "portrait";
		// Get hidmd5, hidsha1, the devide ID hashed
		if (Settings.getSettings().hidmd5 == null) {
			Settings.getSettings().hidmd5 = HashingFunctions.md5(aid);
		}
		hidmd5 = Settings.getSettings().hidmd5;
		if (Settings.getSettings().hidsha1 == null) {
			Settings.getSettings().hidsha1 = HashingFunctions.sha1(aid);
		}
		hidsha1 = Settings.getSettings().hidsha1;
		// Get devMake, devModel, the Make and Model of the current device
		devMake = Settings.getSettings().deviceMake;
		devModel = Settings.getSettings().deviceModel;
		// Get carrier
		if (Settings.getSettings().carrierName == null) {
			Settings.getSettings().carrierName = ((TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE))
					.getNetworkOperatorName();
		}
		carrier = Settings.getSettings().carrierName;
		// Get firstlaunch and convert it to a string
		firstlaunch = "" + Settings.getSettings().first_launch;
		// Get ua, the user agent...
		ua = Settings.getSettings().ua;
		// Get wxh
		this.width = owner.getAdWidth();
		this.height = owner.getAdHeight();
		
		maxHeight = owner.getContainerHeight();
		maxWidth = owner.getContainerWidth();
		
	}

	String getRequestUrl() {
		return Settings.getSettings().BASE_URL
				+ (owner.getPlacementID() != null ? "id="
						+ Uri.encode(owner.getPlacementID())
						: "id=NO-PLACEMENT-ID")
				+ (hidmd5 != null ? "&hidmd5=" + Uri.encode(hidmd5) : "")
				+ (hidsha1 != null ? "&hidsha1=" + Uri.encode(hidsha1) : "")
				+ (devMake != null ? "&devmake=" + Uri.encode(devMake) : "")
				+ (devModel != null ? "&devmodel=" + Uri.encode(devModel) : "")
				+ (carrier != null ? "&carrier=" + Uri.encode(carrier) : "")
				+ (Settings.getSettings().app_id != null ? "&appid="
						+ Uri.encode(Settings.getSettings().app_id)
						: "&appid=NO-APP-ID")
				+ (firstlaunch != null ? "&firstlaunch=" + firstlaunch : "")
				+ (lat != null ? "&lat=" + lat : "")
				+ (lon != null ? "&lon=" + lon : "")
				+ (Settings.getSettings().test_mode ? "&istest=true" : "")
				+ (ua != null ? "&ua=" + Uri.encode(ua) : "")
				+ (orientation != null ? "&orientation=" + orientation : "")
				+ ((width > 0 && height > 0) ? "&size=" + width + "x" + height
						: "") 
				+ ((maxHeight > 0 && maxWidth>0)? "&max-size="+maxWidth+"x"+maxHeight:"")
				+ "&sdkver=" + Uri.encode(Settings.getSettings().sdkVersion);
	}

	/**
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected AdResponse doInBackground(Void... params) {
		// Double check network connectivity before continuing
		if (owner.getContext().checkCallingOrSelfPermission(
				"android.permission.ACCESS_NETWORK_STATE") != PackageManager.PERMISSION_GRANTED){
			Clog.e("OPENSDK", "The SDK needs permission ACCESS_NETWORK_STATE in the host app.");
			return null;
		}
		NetworkInfo ninfo = ((ConnectivityManager) owner.getContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		if (ninfo == null || !ninfo.isConnectedOrConnecting()) {
			Clog.d("OPENSDK-REQUEST",
					"Abandoning AdRequest because there is no network connectivity");
			return null;
		}
		String query_string = getRequestUrl();
		Clog.d("OPENSDK-REQUEST", "Fetching: " + query_string);
		DefaultHttpClient h = new DefaultHttpClient();
		HttpResponse r = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			r = h.execute(new HttpGet(query_string));
			r.getEntity().writeTo(out);
			out.close();
		} catch (ClientProtocolException e) {
			Clog.e("OPENSDK-REQUEST",
					"Couldn't reach the ad server even though network connectivity was detected. Is the server down?");
			return null;
		} catch (ConnectTimeoutException e) {
			Clog.e("OPENSDK-REQUEST", "Connection to ad server timed out.");
			return null;
		} catch (IOException e) {
			if (e instanceof HttpHostConnectException) {
				HttpHostConnectException he = (HttpHostConnectException) e;
				Clog.e("OPENSDK-REQUEST", he.getHost().getHostName() + ":"
						+ he.getHost().getPort() + " is unreachable.");
			} else {
				e.printStackTrace();
				Clog.e("OPENSDK-REQUEST",
						"Ad couldn't be fetched due to io error, probably http related.");
			}
			return null;
		} catch (SecurityException se){
			Clog.e("OPENSDK", "The SDK needs permission INTERNET in the host app.");
		}/* catch (Exception e) {
			e.printStackTrace();
			Clog.e("OPENSDK",
					"Ad couldn't be fetched due to io error, probably http related.");
			return null;
		}*///Leave this commented to figure out what other exceptions might come up during testing!
		return new AdResponse(owner, out.toString(), r.getAllHeaders());
	}

	@Override
	protected void onPostExecute(AdResponse result) {
		if (result == null){
			Clog.v("OPENSDK-RESPONSE", "Request received no response from the server");
			return; // http request failed
		}
		owner.display(result.getDisplayable());
	}

}
