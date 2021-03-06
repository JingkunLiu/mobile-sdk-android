/*
 *    Copyright 2016 APPNEXUS INC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.appnexus.opensdk.instreamvideo;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.appnexus.opensdk.instreamvideo.utils.ANConstants;
import com.appnexus.opensdk.utils.AdvertistingIDUtil;
import com.appnexus.opensdk.utils.Clog;
import com.appnexus.opensdk.utils.Settings;


class InstreamVideoView extends FrameLayout {
    public static final String TAG = InstreamVideoView.class.getName();
    private VideoWebView videoWebView;

    /**
     * Begin Construction
     */
    InstreamVideoView(Context context) {
        this(context, null);
    }

    InstreamVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    InstreamVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(new MutableContextWrapper(context), attrs, defStyle);
        setup(context);
    }


    void setup(Context context) {

        AdvertistingIDUtil.retrieveAndSetAAID(context);

        // Store self.context in the settings for errors
        Clog.setErrorContext(this.getContext());

        Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.new_adview));

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        if (prefs.getBoolean("opensdk_first_launch", true)) {
            // This is the first launch, store a value to remember
            Clog.v(Clog.baseLogTag,
                    Clog.getString(R.string.first_opensdk_launch));
            Settings.getSettings().first_launch = true;
            prefs.edit().putBoolean("opensdk_first_launch", false).commit();
        } else {
            // Found the stored value, this is NOT the first launch
            Clog.v(Clog.baseLogTag,
                    Clog.getString(R.string.not_first_opensdk_launch));
            Settings.getSettings().first_launch = false;
        }

        // Store the UA in the settings
        try {
            Settings.getSettings().ua = new WebView(context).getSettings()
                    .getUserAgentString();
            Clog.v(Clog.baseLogTag,
                    Clog.getString(R.string.ua, Settings.getSettings().ua));
        } catch (Exception e) {
            // Catches PackageManager$NameNotFoundException for webview
            Settings.getSettings().ua = "";
            Clog.e(Clog.baseLogTag, " Exception: " + e.getMessage());
        }

        // Store the AppID in the settings
        Settings.getSettings().app_id = context.getApplicationContext()
                .getPackageName();
        Clog.v(Clog.baseLogTag,
                Clog.getString(R.string.appid, Settings.getSettings().app_id));

        Clog.v(Clog.baseLogTag, Clog.getString(R.string.making_adman));

        // Some AdMob creatives won't load unless we set their parent's viewgroup's padding to 0-0-0-0
        setPadding(0, 0, 0, 0);
    }


    void setVideoWebView(VideoWebView adVideoView) {
        videoWebView = adVideoView;
    }

    VideoWebView getVideoWebView() {
        return videoWebView;
    }

    boolean playAd(ViewGroup layout) {
        Clog.d(ANConstants.videoLogTag, "PlayAd called");
        if (!(layout instanceof FrameLayout || layout instanceof RelativeLayout)) {
            Clog.e(ANConstants.videoLogTag, "Invalid container - a RelativeLayout or FrameLayout is required");
        }
        updateMutableContext(layout);
        setupLayout(layout);
        videoWebView.playAd();
        return true;
    }


    private void setupLayout(ViewGroup layout) {
        if (layout != null && layout.getWidth() > 0 && layout.getHeight() > 0) {
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            this.setLayoutParams(params);
            this.addView(videoWebView);
            layout.addView(this);
        } else {
            Clog.d("Failure", "Failed to set layout params");
        }
    }


    boolean clearSelf() {
        Clog.d(ANConstants.videoLogTag, "clearSelf");
        removeAllViews();
        ViewGroup parentViewGroup = (ViewGroup) getParent();
        if (parentViewGroup != null) {
            ((ViewGroup) getParent()).removeView(this);
        }
        return true;
    }


    public void onDestroy() {
    }

    public void onPause() {
        if (videoWebView != null) {
            videoWebView.onPause();
        }
    }

    public void onResume() {
        if (videoWebView != null) {
            videoWebView.onResume();
            videoWebView.resumeVideo();
        }
    }


    private void updateMutableContext(ViewGroup layout) {
        // Update the MutableContext Wrapper. with the new activity context.
        if(this.getContext() instanceof MutableContextWrapper) {
            ((MutableContextWrapper)this.getContext()).setBaseContext(layout.getContext());
        }

        // Update the MutableContext Wrapper. with the new activity context.
        if(this.videoWebView.getContext() instanceof MutableContextWrapper) {
            ((MutableContextWrapper)this.videoWebView.getContext()).setBaseContext(layout.getContext());
        }
    }
}
