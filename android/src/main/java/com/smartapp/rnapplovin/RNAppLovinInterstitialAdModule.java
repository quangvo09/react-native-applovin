package com.smartapp.rnapplovin;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.applovin.sdk.AppLovinAd;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RNAppLovinInterstitialAdModule extends ReactContextBaseJavaModule {

    public static final String REACT_CLASS = "RNAppLovinInterstitial";

    public static final String EVENT_AD_LOADED = "interstitialAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "interstitialAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "interstitialAdOpened";
    public static final String EVENT_AD_CLOSED = "interstitialAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "interstitialAdLeftApplication";

    private String _adUnitID;

    private AppLovinAd loadedAd;
    private AppLovinInterstitialAdDialog interstitialAd;

    private Promise mRequestAdPromise;
    private boolean isReady; 

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public RNAppLovinInterstitialAdModule(ReactApplicationContext reactContext) {
        super(reactContext);

        interstitialAd = AppLovinInterstitialAd.create(AppLovinSdk.getInstance(reactContext), reactContext);

        interstitialAd.setAdDisplayListener(new AppLovinAdDisplayListener()
        {
          @Override
          public void adDisplayed(AppLovinAd appLovinAd)
          {
            sendEvent(EVENT_AD_OPENED, null);
          }

          @Override
          public void adHidden(AppLovinAd appLovinAd)
          {
            sendEvent(EVENT_AD_CLOSED, null);
          }
        });

        interstitialAd.setAdVideoPlaybackListener(new AppLovinAdVideoPlaybackListener()
        {
          @Override
          public void videoPlaybackBegan(AppLovinAd appLovinAd)
          {
          }

          @Override
          public void videoPlaybackEnded(AppLovinAd appLovinAd, double percentViewed, boolean wasFullyViewed)
          {
            isReady = false;
          }
        });
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @ReactMethod
    public void setAdUnitID(String adUnitID) {
        _adUnitID = adUnitID;
    }

    @ReactMethod
    public void requestAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                AppLovinSdk.getInstance(getReactApplicationContext()).getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, new AppLovinAdLoadListener()
                {
                    @Override
                    public void adReceived(AppLovinAd ad)
                    {
                      loadedAd = ad;
                      isReady = true;
                      promise.resolve(null);
                    }

                    @Override
                    public void failedToReceiveAd(int errorCode)
                    {
                      loadedAd = null;
                      isReady = false;
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
                      promise.reject(errorString, errorMessage);
                    }
                });
            }
        });
    }

    @ReactMethod
    public void showAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                if (isReady) {
                  interstitialAd.showAndRender(loadedAd);
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
            public void run () {
                callback.invoke(isReady);
            }
        });
    }
}
