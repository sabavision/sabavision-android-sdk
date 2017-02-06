// Copyright (C) 2015 by Tapjoy Inc.
//
// This file is part of the Tapjoy SDK.
//
// By using the Tapjoy SDK in your software, you agree to the terms of the Tapjoy SDK License Agreement.
//
// The Tapjoy SDK is bound by the Tapjoy SDK License Agreement and can be found here: https://www.tapjoy.com/sdk/license

package com.sabavision.mobileads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.sabavision.common.logging.SabaVisionLog;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyLog;

import java.util.Map;

// Tested with Tapjoy SDK 11.8.2
public class TapjoyInterstitial extends CustomEventInterstitial implements TJPlacementListener {
    private static final String TAG = TapjoyInterstitial.class.getSimpleName();
    private static final String TJC_SABAVISION_NETWORK_CONSTANT = "sabavision";
    private static final String TJC_SABAVISION_ADAPTER_VERSION_NUMBER = "4.1.0";

    // Configuration keys
    public static final String SDK_KEY = "sdkKey";
    public static final String DEBUG_ENABLED = "debugEnabled";
    public static final String PLACEMENT_NAME = "name";

    private TJPlacement tjPlacement;
    private CustomEventInterstitialListener mInterstitialListener;
    private Handler mHandler;

    static {
        TapjoyLog.i(TAG, "Class initialized with network adapter version " + TJC_SABAVISION_ADAPTER_VERSION_NUMBER);
    }

    @Override
    protected void loadInterstitial(final Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        SabaVisionLog.d("Requesting Tapjoy interstitial");

        mInterstitialListener = customEventInterstitialListener;
        mHandler = new Handler(Looper.getMainLooper());

        final String placementName = serverExtras.get(PLACEMENT_NAME);
        if (TextUtils.isEmpty(placementName)) {
            SabaVisionLog.d("Tapjoy interstitial loaded with empty 'name' field. Request will fail.");
        }

        boolean canRequestPlacement = true;
        if (!Tapjoy.isConnected()) {
            // Check if configuration data is available
            boolean enableDebug = Boolean.valueOf(serverExtras.get(DEBUG_ENABLED));
            Tapjoy.setDebugEnabled(enableDebug);

            String sdkKey = serverExtras.get(SDK_KEY);
            if (!TextUtils.isEmpty(sdkKey)) {
                SabaVisionLog.d("Connecting to Tapjoy via SabaVision dashboard settings...");
                Tapjoy.connect(context, sdkKey, null, new TJConnectListener() {
                    @Override
                    public void onConnectSuccess() {
                        SabaVisionLog.d("Tapjoy connected successfully");
                        createPlacement(context, placementName);
                    }

                    @Override
                    public void onConnectFailure() {
                        SabaVisionLog.d("Tapjoy connect failed");
                    }
                });

                // If sdkKey is present via SabaVision dashboard, we only want to request placement
                // after auto-connect succeeds
                canRequestPlacement = false;
            } else {
                SabaVisionLog.d("Tapjoy interstitial is initialized with empty 'sdkKey'. You must call Tapjoy.connect()");
            }
        }

        if (canRequestPlacement) {
            createPlacement(context, placementName);
        }
    }

    private void createPlacement(Context context, String placementName) {
        tjPlacement = new TJPlacement(context, placementName, this);
        tjPlacement.setMediationName(TJC_SABAVISION_NETWORK_CONSTANT);
        tjPlacement.setAdapterVersion(TJC_SABAVISION_ADAPTER_VERSION_NUMBER);
        tjPlacement.requestContent();
    }

    @Override
    protected void onInvalidate() {
        // No custom cleanup to do here.
    }

    @Override
    protected void showInterstitial() {
        SabaVisionLog.d("Tapjoy interstitial will be shown");
        tjPlacement.showContent();
    }

    // Tapjoy

    @Override
    public void onRequestSuccess(final TJPlacement placement) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (placement.isContentAvailable()) {
                    SabaVisionLog.d("Tapjoy interstitial request successful");
                    mInterstitialListener.onInterstitialLoaded();
                } else {
                    SabaVisionLog.d("No Tapjoy interstitials available");
                    mInterstitialListener.onInterstitialFailed(SabaVisionErrorCode.NETWORK_NO_FILL);
                }
            }
        });
    }

    @Override
    public void onRequestFailure(TJPlacement placement, TJError error) {
        SabaVisionLog.d("Tapjoy interstitial request failed");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialFailed(SabaVisionErrorCode.NETWORK_NO_FILL);
            }
        });
    }

    @Override
    public void onContentShow(TJPlacement placement) {
        SabaVisionLog.d("Tapjoy interstitial shown");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialShown();
            }
        });
    }

    @Override
    public void onContentDismiss(TJPlacement placement) {
        SabaVisionLog.d("Tapjoy interstitial dismissed");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialDismissed();
            }
        });
    }

    @Override
    public void onContentReady(TJPlacement placement) {
    }

    @Override
    public void onPurchaseRequest(TJPlacement placement, TJActionRequest request,
            String productId) {
    }

    @Override
    public void onRewardRequest(TJPlacement placement, TJActionRequest request, String itemId,
            int quantity) {
    }

}
