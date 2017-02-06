package com.sabavision.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.sabavision.common.BaseLifecycleListener;
import com.sabavision.common.DataKeys;
import com.sabavision.common.LifecycleListener;
import com.sabavision.common.MediationSettings;
import com.sabavision.common.SabaVisionReward;
import com.sabavision.common.VisibleForTesting;
import com.sabavision.common.logging.SabaVisionLog;
import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.UnityAds;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom event for showing Unity rewarded videos.
 *
 * Certified with Unity 1.4.7
 */
public class UnityRewardedVideo extends CustomEventRewardedVideo {
    private static final String DEFAULT_ZONE_ID = "";
    private static final String GAME_ID_KEY = "gameId";
    private static final String ZONE_ID_KEY = "zoneId";
    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final UnityAdsListener sUnityAdsListener = new UnityAdsListener();

    private static boolean sInitialized = false;
    @NonNull private static String sZoneId = DEFAULT_ZONE_ID;

    @Nullable private UnityMediationSettings mMediationSettings;

    @Override
    @NonNull
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return sUnityAdsListener;
    }

    @Override
    @NonNull
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @Override
    @NonNull
    public String getAdNetworkId() {
        return sZoneId;
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        if (sInitialized) {
            return false;
        }

        String gameId;
        if (serverExtras.containsKey(GAME_ID_KEY)) {
            gameId = serverExtras.get(GAME_ID_KEY);
            if (TextUtils.isEmpty(gameId)) {
                throw new IllegalStateException("Unity rewarded video initialization failed due " +
                        "to empty " + GAME_ID_KEY);
            }
        } else {
            throw new IllegalStateException("Unity rewarded video initialization failed due to " +
                    "missing " + GAME_ID_KEY);
        }

        UnityAds.init(launcherActivity, gameId, sUnityAdsListener);
        sInitialized = true;

        return true;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras)
            throws Exception {

        if (serverExtras.containsKey(ZONE_ID_KEY)) {
            String zoneId = serverExtras.get(ZONE_ID_KEY);
            sZoneId = TextUtils.isEmpty(zoneId) ? sZoneId : zoneId;
        }

        try {
            setUpMediationSettingsForRequest((String) localExtras.get(DataKeys.AD_UNIT_ID_KEY));
        } catch (ClassCastException e) {
            SabaVisionLog.e("Failed to set up Unity mediation settings due to invalid ad unit id", e);
        }

        loadRewardedVideo();
    }

    @Override
    public boolean hasVideoAvailable() {
        return UnityAds.canShow();
    }

    @Override
    public void showVideo() {
        if (hasVideoAvailable()) {
            UnityAds.show(getUnityProperties());
        } else {
            SabaVisionLog.d("Attempted to show Unity rewarded video before it was available.");
        }
    }

    @Override
    protected void onInvalidate() {
        UnityAds.setListener(null);
    }


    private void setUpMediationSettingsForRequest(@Nullable final String sabavisionId) {
        mMediationSettings =
                SabaVisionRewardedVideoManager.getGlobalMediationSettings(UnityMediationSettings.class);

        // Instance settings override global settings.
        if (sabavisionId != null) {
            final UnityMediationSettings instanceSettings = SabaVisionRewardedVideoManager
                    .getInstanceMediationSettings(UnityMediationSettings.class, sabavisionId);
            if (instanceSettings != null) {
                mMediationSettings = instanceSettings;
            }
        }

    }

    private static final class UnityLifecycleListener extends BaseLifecycleListener {
        @Override
        public void onCreate(@NonNull final Activity activity) {
            super.onCreate(activity);
            UnityAds.changeActivity(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
            UnityAds.changeActivity(activity);
        }

    }

    @NonNull
    private Map<String, Object> getUnityProperties() {
        if (mMediationSettings == null) {
            return Collections.emptyMap();
        }
        return mMediationSettings.getPropertiesMap();
    }


    private static class UnityAdsListener implements IUnityAdsListener,
            CustomEventRewardedVideoListener {
        @Override
        public void onFetchCompleted() {
            SabaVisionLog.d("Unity rewarded video cached for zone " + UnityAds.getZone() + ".");
            loadRewardedVideo();
        }

        @Override
        public void onFetchFailed() {
            SabaVisionLog.d("Unity rewarded video cache failed for zone " + UnityAds.getZone() + ".");
            SabaVisionRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class,
                    UnityAds.getZone(), SabaVisionErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onShow() {
            SabaVisionLog.d("Unity rewarded video displayed for zone " + UnityAds.getZone() + ".");
        }

        @Override
        public void onHide() {
            SabaVisionRewardedVideoManager.onRewardedVideoClosed(UnityRewardedVideo.class, UnityAds.getZone());
            SabaVisionLog.d("Unity rewarded video dismissed for zone " + UnityAds.getZone() + ".");
        }

        @Override
        public void onVideoStarted() {
            SabaVisionRewardedVideoManager.onRewardedVideoStarted(UnityRewardedVideo.class, UnityAds.getZone());
            SabaVisionLog.d("Unity rewarded video started for zone " + UnityAds.getZone() + ".");
        }

        @Override
        public void onVideoCompleted(final String itemKey, final boolean skipped) {
            if (!skipped) {
                SabaVisionRewardedVideoManager.onRewardedVideoCompleted(
                        UnityRewardedVideo.class,
                        UnityAds.getZone(),
                        SabaVisionReward.success(itemKey, SabaVisionReward.NO_REWARD_AMOUNT));
                SabaVisionLog.d("Unity rewarded video completed for zone " + UnityAds.getZone()
                        + " with reward item key " + itemKey);
            } else {
                SabaVisionRewardedVideoManager.onRewardedVideoCompleted(
                        UnityRewardedVideo.class,
                        UnityAds.getZone(),
                        SabaVisionReward.failure());
                SabaVisionLog.d("Unity rewarded video skipped for zone " + UnityAds.getZone() + " with "
                        + "reward item key " + itemKey);
            }
        }
    }

    private static void loadRewardedVideo() {
        UnityAds.setZone(sZoneId);
        SabaVisionRewardedVideoManager.onRewardedVideoLoadSuccess(UnityRewardedVideo.class, UnityAds.getZone());
    }

    public static final class UnityMediationSettings implements MediationSettings {
        @NonNull private final HashMap<String, Object> mProperties;

        public UnityMediationSettings(@NonNull final String gamerId) {
            mProperties = new HashMap<String, Object>();
            mProperties.put(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY, gamerId);
        }

        @NonNull
        public Map<String, Object> getPropertiesMap() {
            return mProperties;
        }
    }

    @VisibleForTesting
    void reset() {
        sInitialized = false;
        sZoneId = DEFAULT_ZONE_ID;
    }
}
