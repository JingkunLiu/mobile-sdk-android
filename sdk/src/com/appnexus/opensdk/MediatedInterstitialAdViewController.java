/*
 *    Copyright 2013 APPNEXUS INC
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
package com.appnexus.opensdk;

import android.app.Activity;

import com.appnexus.opensdk.adresponsedata.BaseAdResponse;
import com.appnexus.opensdk.adresponsedata.CSMAdResponse;
import com.appnexus.opensdk.utils.Clog;
import com.appnexus.opensdk.utils.HTTPGet;
import com.appnexus.opensdk.utils.HTTPResponse;
import com.appnexus.opensdk.utils.Settings;

/**
* An object of this type is sent to the third-party SDK's {@link
* MediatedInterstitialAdView} object.  The third-party SDK uses this
* object from within its interstitial view implementation to send
* events back to the AppNexus SDK.
*/

public class MediatedInterstitialAdViewController extends MediatedAdViewController {

    static MediatedInterstitialAdViewController create(
            Activity activity, AdRequester requester,
            MediatedAd mediatedAd, AdDispatcher listener) {
        MediatedInterstitialAdViewController out = new MediatedInterstitialAdViewController(activity, requester, mediatedAd, listener);
        return out.hasFailed ? null : out;
    }

    private MediatedInterstitialAdViewController(
            Activity activity, AdRequester requester, MediatedAd mediatedAd,
            AdDispatcher listener) {
        super(requester, mediatedAd, listener, MediaType.INTERSTITIAL);

        if (!isValid(MediatedInterstitialAdView.class))
            return;

        // if controller is valid, request an ad
        Clog.d(Clog.mediationLogTag, Clog.getString(R.string.mediated_request));

        ResultCode errorCode = null;

        startTimeout();
        markLatencyStart();

        try {
            if(activity!=null){
                ((MediatedInterstitialAdView) mAV).requestAd(this,
                        activity,
                        currentAd.getParam(),
                        currentAd.getId(),
                        getTargetingParameters());
            }else{
                Clog.e(Clog.mediationLogTag, Clog.getString(R.string.mediated_request_null_activity));
                errorCode = ResultCode.INTERNAL_ERROR;
            }
        } catch (Exception e) {
            Clog.e(Clog.mediationLogTag, Clog.getString(R.string.mediated_request_exception), e);
            errorCode = ResultCode.INTERNAL_ERROR;
        } catch (Error e) {
            // catch errors. exceptions will be caught above.
            Clog.e(Clog.mediationLogTag, Clog.getString(R.string.mediated_request_error), e);
            errorCode = ResultCode.INTERNAL_ERROR;
        }

        if (errorCode != null)
            onAdFailed(errorCode);
    }

    static MediatedInterstitialAdViewController create(
            Activity activity, AdRequester requester,
            CSMAdResponse mediatedAd, AdDispatcher listener) {
        MediatedInterstitialAdViewController out = new MediatedInterstitialAdViewController(activity, requester, mediatedAd, listener);
        return out.hasFailed ? null : out;
    }

    private MediatedInterstitialAdViewController(
            Activity activity, AdRequester requester, CSMAdResponse mediatedAd,
            AdDispatcher listener) {
        super(requester, mediatedAd, listener, MediaType.INTERSTITIAL);

        if (!isValid(MediatedInterstitialAdView.class))
            return;

        // if controller is valid, request an ad
        Clog.d(Clog.mediationLogTag, Clog.getString(R.string.mediated_request));

        ResultCode errorCode = null;

        startTimeout();
        markLatencyStart();

        try {
            if(activity!=null){
                ((MediatedInterstitialAdView) mAV).requestAd(this,
                        activity,
                        currentCSMAd.getParam(),
                        currentCSMAd.getId(),
                        getTargetingParameters());
            }else{
                Clog.e(Clog.mediationLogTag, Clog.getString(R.string.mediated_request_null_activity));
                errorCode = ResultCode.INTERNAL_ERROR;
            }
        } catch (Exception e) {
            Clog.e(Clog.mediationLogTag, Clog.getString(R.string.mediated_request_exception), e);
            errorCode = ResultCode.INTERNAL_ERROR;
        } catch (Error e) {
            // catch errors. exceptions will be caught above.
            Clog.e(Clog.mediationLogTag, Clog.getString(R.string.mediated_request_error), e);
            errorCode = ResultCode.INTERNAL_ERROR;
        }

        if (errorCode != null)
            onAdFailed(errorCode);
    }

    void show() {
        if (mAV != null && !destroyed) {
            ((MediatedInterstitialAdView) mAV).show();
            fireImpressions(currentCSMAd);
        }
    }

    private void fireImpressions(BaseAdResponse responseData) {
        if(Settings.useUniversalTagV2) {
            if (responseData != null && responseData.getImpressionURLs() != null) {
                for (final String impressionUrl : responseData.getImpressionURLs()) {
                    Clog.i(Clog.baseLogTag, "Impressions: " + impressionUrl);
                    new HTTPGet() {
                        @Override
                        protected void onPostExecute(HTTPResponse response) {
                            if (response.getSucceeded()) {
                                Clog.i(Clog.baseLogTag, "Impression sent successfully ");
                            }
                        }

                        @Override
                        protected String getUrl() {
                            return impressionUrl;
                        }
                    }.execute();
                }
            }
        }
    }



    @Override
    boolean isReady() {
        return ((MediatedInterstitialAdView) mAV).isReady();
    }

    @Override
    public void onDestroy() {
        destroyed=true;
        if(mAV!=null) {
            mAV.onDestroy();
        }
    }

    @Override
    public void onPause() {
        if(mAV!=null) {
            mAV.onPause();
        }
    }

    @Override
    public void onResume() {
        if(mAV!=null) {
            mAV.onResume();
        }
    }
}
