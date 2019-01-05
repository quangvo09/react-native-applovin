package com.smartapp.rnapplovin;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;

import java.util.ArrayList;

public class RNAppLovinRewardedVideoAdModule extends ReactContextBaseJavaModule implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdVideoPlaybackListener, AppLovinAdRewardListener {

    public static final String REACT_CLASS = "RNAppLovinRewarded";

    public static final String EVENT_AD_LOADED = "rewardedVideoAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "rewardedVideoAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "rewardedVideoAdOpened";
    public static final String EVENT_AD_CLOSED = "rewardedVideoAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "rewardedVideoAdLeftApplication";
    public static final String EVENT_REWARDED = "rewardedVideoAdRewarded";
    public static final String EVENT_VIDEO_STARTED = "rewardedVideoAdVideoStarted";
    public static final String EVENT_VIDEO_COMPLETED = "rewardedVideoAdVideoCompleted";

    private AppLovinIncentivizedInterstitial mIncent;
    private Promise mRequestAdPromise;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public RNAppLovinRewardedVideoAdModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }
 
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @ReactMethod
    public void requestAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mIncent == null) {
                  mIncent = AppLovinIncentivizedInterstitial.create(getCurrentActivity());
                }

                if (mIncent.isAdReadyToDisplay()) {
                  promise.reject("E_AD_ALREADY_LOADED", "Ad is already loaded.");
                } else {
                  mRequestAdPromise = promise;
                  mIncent.preload(RNAppLovinRewardedVideoAdModule.this);
                }
            }
        });
    }

    @ReactMethod
    public void showAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mIncent && mIncent.isAdReadyToDisplay()) {
                    mIncent.show(getCurrentActivity(), RNAppLovinRewardedVideoAdModule.this, RNAppLovinRewardedVideoAdModule.this, RNAppLovinRewardedVideoAdModule.this);
                    promise.resolve(null);
                } else {
                    promise.reject("E_AD_NOT_READY", "Ad is not ready.");
                }
            }
        });
    }

    @ReactMethod
    public void isReady(final Callback callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              if (!mIncent) {
                callback.invoke(false);
              } else {
                callback.invoke(mIncent.isAdReadyToDisplay());
              }
            }
        });
    }

    // -------------------------
    // AppLovinAdLoadListener
    @Override
    public void adReceived(AppLovinAd appLovinAd) {
    }

    @Override
    public void failedToReceiveAd(int errorCode) {
      String errorString = "ERROR_UNKNOWN";
      String errorMessage = "Unknown error";
      switch (errorCode) {
          case AppLovinErrorCodes.FETCH_AD_TIMEOUT:
            errorString = "ERROR_CODE_FETCH_AD_TIMEOUT";
            errorMessage = "The network conditions prevented the SDK from receiving an ad";
            break;
          case AppLovinErrorCodes.UNABLE_TO_RENDER_AD:
            errorString = "ERROR_CODE_UNABLE_TO_RENDER_AD";
            errorMessage = "There has been a failure to render an ad on screen";
            break;
          case AppLovinErrorCodes.UNSPECIFIED_ERROR:
            errorString = "ERROR_CODE_UNSPECIFIED_ERROR";
            errorMessage = "The system is in unexpected state";
            break;
          case AppLovinErrorCodes.UNABLE_TO_PREPARE_NATIVE_AD:
            errorString = "ERROR_CODE_UNABLE_TO_PREPARE_NATIVE_AD";
            errorMessage = "There was an error while attempting to render a native ad";
            break;
          case AppLovinErrorCodes.UNABLE_TO_PRECACHE_RESOURCES:
            errorString = "ERROR_CODE_UNABLE_TO_PRECACHE_RESOURCES";
            errorMessage = "An attempt to cache a resource to the filesystem failed; the device may be out of space.";
            break;
          case AppLovinErrorCodes.UNABLE_TO_PRECACHE_IMAGE_RESOURCES:
            errorString = "ERROR_CODE_UNABLE_TO_PRECACHE_IMAGE_RESOURCES";
            errorMessage = "An attempt to cache an image resource to the filesystem failed; the device may be out of space.";
            break;
          case AppLovinErrorCodes.UNABLE_TO_PRECACHE_VIDEO_RESOURCES:
            errorString = "ERROR_CODE_UNABLE_TO_PRECACHE_IMAGE_RESOURCES";
            errorMessage = "An attempt to cache a video resource to the filesystem failed; the device may be out of space.";
            break;
          case AppLovinErrorCodes.NO_NETWORK:
            errorString = "ERROR_CODE_NETWORK_ERROR";
            errorMessage = "The ad request was unsuccessful due to network connectivity.";
            break;
          case AppLovinErrorCodes.NO_FILL:
            errorString = "ERROR_CODE_NO_FILL";
            errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
            break;
      }

      WritableMap event = Arguments.createMap();
      WritableMap error = Arguments.createMap();
      event.putString("message", errorMessage);
      sendEvent(EVENT_AD_FAILED_TO_LOAD, event);
      mRequestAdPromise.reject(errorString, errorMessage);
    }

    // -------------------------
    // AppLovinAdDisplayListener

    @Override
    public void adDisplayed(AppLovinAd appLovinAd) {
      sendEvent(EVENT_AD_OPENED, null);
    }
    @Override
    public void adHidden(AppLovinAd appLovinAd) {
      sendEvent(EVENT_AD_CLOSED, null);
    }

    // -------------------------
    //  AppLovinAdVideoPlaybackListener

    @Override
    public void videoPlaybackBegan(final AppLovinAd ad) {
      sendEvent(EVENT_VIDEO_STARTED, null);
    }

    @Override
    public void videoPlaybackEnded(final AppLovinAd ad, final double percentViewed, final boolean fullyWatched) {
      sendEvent(EVENT_VIDEO_COMPLETED, null);
    }

    // -------------------------
    // AppLovinAdRewardListener

    @Override
    public void userRewardVerified(final AppLovinAd ad, Map<String, String> response) {
      WritableMap reward = Arguments.createMap();
      sendEvent(EVENT_REWARDED, reward);
    }

    @Override
    public void userOverQuota(final AppLovinAd ad, Map<String, String> response) {
    }

    @Override
    public void userRewardRejected(final AppLovinAd ad, final Map<String, String> response) {
    }

    @Override
    public void validationRequestFailed(final AppLovinAd ad, final int errorCode) {
    }

    @Override
    public void userDeclinedToViewAd(final AppLovinAd ad) {
    }
}
