package com.sabavision.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.sabavision.common.LifecycleListener;
import com.sabavision.common.MediationSettings;
import com.sabavision.common.SabaVisionReward;
import com.sabavision.common.logging.SabaVisionLog;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJVideoListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyLog;

import java.util.Hashtable;
import java.util.Map;

// Tested with Tapjoy SDK 11.8.2
public class TapjoyRewardedVideo extends CustomEventRewardedVideo {
    private static final String TAG = TapjoyRewardedVideo.class.getSimpleName();
    private static final String TJC_SABAVISION_NETWORK_CONSTANT = "sabavision";
    private static final String TJC_SABAVISION_ADAPTER_VERSION_NUMBER = "4.1.0";
    private static final String TAPJOY_AD_NETWORK_CONSTANT = "tapjoy_id";

    // Configuration keys
    public static final String SDK_KEY = "sdkKey";
    public static final String DEBUG_ENABLED = "debugEnabled";
    public static final String PLACEMENT_NAME = "name";

    private String sdkKey;
    private String placementName;
    private Hashtable<String, Object> connectFlags;
    private TJPlacement tjPlacement;
    private boolean isAutoConnect = false;
    private static TapjoyRewardedVideoListener sTapjoyListener = new TapjoyRewardedVideoListener();

    static {
        TapjoyLog.i(TAG, "Class initialized with network adapter version " + TJC_SABAVISION_ADAPTER_VERSION_NUMBER);
    }

    @Override
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return sTapjoyListener;
    }

    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected String getAdNetworkId() {
        return TAPJOY_AD_NETWORK_CONSTANT;
    }

    @Override
    protected void onInvalidate() {
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception {

        placementName = serverExtras.get(PLACEMENT_NAME);
        if (TextUtils.isEmpty(placementName)) {
            SabaVisionLog.d("Tapjoy rewarded video loaded with empty 'name' field. Request will fail.");
        }

        if (!Tapjoy.isConnected()) {
            if (checkAndInitMediationSettings()) {
                SabaVisionLog.d("Connecting to Tapjoy via SabaVision mediation settings...");
                connectToTapjoy(launcherActivity);

                isAutoConnect = true;
                return true;
            } else {
                boolean enableDebug = Boolean.valueOf(serverExtras.get(DEBUG_ENABLED));
                Tapjoy.setDebugEnabled(enableDebug);

                sdkKey = serverExtras.get(SDK_KEY);
                if (!TextUtils.isEmpty(sdkKey)) {
                    SabaVisionLog.d("Connecting to Tapjoy via SabaVision dashboard settings...");
                    connectToTapjoy(launcherActivity);

                    isAutoConnect = true;
                    return true;
                } else {
                    SabaVisionLog.d("Tapjoy rewarded video is initialized with empty 'sdkKey'. You must call Tapjoy.connect()");
                    isAutoConnect = false;
                }
            }
        }

        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception {
        SabaVisionLog.d("Requesting Tapjoy rewarded video");
        createPlacement(activity);
    }

    private void connectToTapjoy(final Activity launcherActivity) {
        Tapjoy.connect(launcherActivity, sdkKey, connectFlags, new TJConnectListener() {
            @Override
            public void onConnectSuccess() {
                SabaVisionLog.d("Tapjoy connected successfully");
                createPlacement(launcherActivity);
            }

            @Override
            public void onConnectFailure() {
                SabaVisionLog.d("Tapjoy connect failed");
            }
        });
    }

    private void createPlacement(Activity activity) {
        if (!TextUtils.isEmpty(placementName)) {
            if (isAutoConnect && !Tapjoy.isConnected()) {
                // If adapter is making the Tapjoy.connect() call on behalf of the pub, wait for it to
                // succeed before making a placement request.
                SabaVisionLog.d("Tapjoy is still connecting. Please wait for this to finish before making a placement request");
                return;
            }

            tjPlacement = new TJPlacement(activity, placementName, sTapjoyListener);
            tjPlacement.setMediationName(TJC_SABAVISION_NETWORK_CONSTANT);
            tjPlacement.setAdapterVersion(TJC_SABAVISION_ADAPTER_VERSION_NUMBER);
            tjPlacement.requestContent();
        } else {
            SabaVisionLog.d("Tapjoy placementName is empty. Unable to create TJPlacement.");
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return tjPlacement.isContentAvailable();
    }

    @Override
    protected void showVideo() {
        if (hasVideoAvailable()) {
            SabaVisionLog.d("Tapjoy rewarded video will be shown.");
            tjPlacement.showContent();
        } else {
            SabaVisionLog.d("Failed to show Tapjoy rewarded video.");
        }
    }

    private boolean checkAndInitMediationSettings() {
        final TapjoyMediationSettings globalMediationSettings =
                SabaVisionRewardedVideoManager.getGlobalMediationSettings(TapjoyMediationSettings.class);

        if (globalMediationSettings != null) {
            SabaVisionLog.d("Initializing Tapjoy mediation settings");

            if (!TextUtils.isEmpty(globalMediationSettings.getSdkKey())) {
                sdkKey = globalMediationSettings.getSdkKey();
            } else {
                SabaVisionLog.d("Cannot initialize Tapjoy -- 'sdkkey' is empty");
                return false;
            }

            if (globalMediationSettings.getConnectFlags() != null) {
                connectFlags = globalMediationSettings.getConnectFlags();
            }

            return true;
        } else {
            return false;
        }
    }

    private static class TapjoyRewardedVideoListener implements TJPlacementListener, CustomEventRewardedVideoListener, TJVideoListener {
        @Override
        public void onRequestSuccess(TJPlacement placement) {
            if (!placement.isContentAvailable()) {
                SabaVisionLog.d("No Tapjoy rewarded videos available");
                SabaVisionRewardedVideoManager.onRewardedVideoLoadFailure(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, SabaVisionErrorCode.NETWORK_NO_FILL);
            }
        }

        @Override
        public void onContentReady(TJPlacement placement) {
            SabaVisionLog.d("Tapjoy rewarded video content is ready");
            SabaVisionRewardedVideoManager.onRewardedVideoLoadSuccess(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
        }

        @Override
        public void onRequestFailure(TJPlacement placement, TJError error) {
            SabaVisionLog.d("Tapjoy rewarded video request failed");
            SabaVisionRewardedVideoManager.onRewardedVideoLoadFailure(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, SabaVisionErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onContentShow(TJPlacement placement) {
            Tapjoy.setVideoListener(this);
            SabaVisionLog.d("Tapjoy rewarded video content shown");
            SabaVisionRewardedVideoManager.onRewardedVideoStarted(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
        }

        @Override
        public void onContentDismiss(TJPlacement placement) {
            Tapjoy.setVideoListener(null);
            SabaVisionLog.d("Tapjoy rewarded video content dismissed");
            SabaVisionRewardedVideoManager.onRewardedVideoClosed(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
        }

        @Override
        public void onPurchaseRequest(TJPlacement placement, TJActionRequest request,
                String productId) {
        }

        @Override
        public void onRewardRequest(TJPlacement placement, TJActionRequest request, String itemId,
                int quantity) {
        }

        @Override
        public void onVideoStart() {

        }

        @Override
        public void onVideoError(int statusCode) {
        }

        @Override
        public void onVideoComplete() {
            SabaVisionLog.d("Tapjoy rewarded video completed");
            SabaVisionRewardedVideoManager.onRewardedVideoCompleted(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, SabaVisionReward.success(SabaVisionReward.NO_REWARD_LABEL, SabaVisionReward.NO_REWARD_AMOUNT));
        }
    }

    public static final class TapjoyMediationSettings implements MediationSettings {
        @Nullable
        private final String mSdkKey;
        @Nullable
        Hashtable<String, Object> mConnectFlags;

        public TapjoyMediationSettings(String sdkKey) {
            this.mSdkKey = sdkKey;
        }

        public TapjoyMediationSettings(String sdkKey, Hashtable<String, Object> connectFlags) {
            this.mSdkKey = sdkKey;
            this.mConnectFlags = connectFlags;
        }

        @NonNull
        public String getSdkKey() {
            return mSdkKey;
        }

        @NonNull
        public Hashtable<String, Object> getConnectFlags() {
            return mConnectFlags;
        }
    }

}
