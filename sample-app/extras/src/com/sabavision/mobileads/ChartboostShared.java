package com.sabavision.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostDelegate;
import com.chartboost.sdk.Model.CBError;
import com.sabavision.common.SabaVision;
import com.sabavision.common.SabaVisionReward;
import com.sabavision.common.Preconditions;
import com.sabavision.common.VisibleForTesting;
import com.sabavision.common.logging.SabaVisionLog;
import com.sabavision.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.sabavision.mobileads.SabaVisionErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.sabavision.mobileads.SabaVisionErrorCode.VIDEO_DOWNLOAD_ERROR;

/**
 * Shared infrastructure for initializing the Chartboost SDK when mediated by SabaVision
 *
 * Certified with Chartboost 6.4.1
 */
public class ChartboostShared {
    private static volatile ChartboostSingletonDelegate sDelegate = new ChartboostSingletonDelegate();

    /*
     * These keys are intended for SabaVision internal use. Do not modify.
     */
    public static final String APP_ID_KEY = "appId";
    public static final String APP_SIGNATURE_KEY = "appSignature";
    public static final String LOCATION_KEY = "location";
    public static final String LOCATION_DEFAULT = "Default";

    @Nullable private static String mAppId;
    @Nullable private static String mAppSignature;

    /**
     * Initialize the Chartboost SDK for the provided application id and app signature.
     */
    public static synchronized boolean initializeSdk(@NonNull Activity launcherActivity,
            @NonNull Map<String, String> serverExtras) {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(serverExtras);

        // Validate Chartboost args
        if (!serverExtras.containsKey(APP_ID_KEY)) {
            throw new IllegalStateException("Chartboost rewarded video initialization" +
                    " failed due to missing application ID.");
        }

        if (!serverExtras.containsKey(APP_SIGNATURE_KEY)) {
            throw new IllegalStateException("Chartboost rewarded video initialization" +
                    " failed due to missing application signature.");
        }

        final String appId = serverExtras.get(APP_ID_KEY);
        final String appSignature = serverExtras.get(APP_SIGNATURE_KEY);

        if (appId.equals(mAppId) && appSignature.equals(mAppSignature)) {
            // We don't need to reinitialize.
            return false;
        }

        mAppId = appId;
        mAppSignature = appSignature;

        // Perform all the common SDK initialization steps including startAppWithId
        Chartboost.startWithAppId(launcherActivity, mAppId, mAppSignature);
        Chartboost.setMediation(Chartboost.CBMediation.CBMediationSabaVision, SabaVision.SDK_VERSION);
        Chartboost.setDelegate(sDelegate);
        Chartboost.setShouldRequestInterstitialsInFirstSession(true);
        Chartboost.setAutoCacheAds(false);
        Chartboost.setShouldDisplayLoadingViewForMoreApps(false);

        // Callers of this method need to call onCreate & onStart themselves.
        return true;
    }

    @NonNull
    public static ChartboostSingletonDelegate getDelegate() {
        return sDelegate;
    }

    /**
     * A {@link ChartboostDelegate} that can forward events for Chartboost interstitials
     * and rewarded videos to the appropriate listener based on the Chartboost location used.
     */
    public static class ChartboostSingletonDelegate extends ChartboostDelegate
            implements CustomEventRewardedVideo.CustomEventRewardedVideoListener {
        private static final CustomEventInterstitialListener NULL_LISTENER =
                new CustomEventInterstitialListener() {
                    @Override
                    public void onInterstitialLoaded() { }

                    @Override
                    public void onInterstitialFailed(SabaVisionErrorCode errorCode) { }

                    @Override
                    public void onInterstitialShown() { }

                    @Override
                    public void onInterstitialClicked() { }

                    @Override
                    public void onLeaveApplication() { }

                    @Override
                    public void onInterstitialDismissed() { }
                };

        //***************
        // Chartboost Location Management for interstitials and rewarded videos
        //***************

        private Map<String, CustomEventInterstitialListener> mInterstitialListenersForLocation
                = Collections.synchronizedMap(new TreeMap<String, CustomEventInterstitialListener>());

        private Set<String> mRewardedVideoLocationsToLoad = Collections.synchronizedSet(new TreeSet<String>());

        public void registerInterstitialListener(@NonNull String location,
                @NonNull CustomEventInterstitialListener interstitialListener) {
            Preconditions.checkNotNull(location);
            Preconditions.checkNotNull(interstitialListener);
            mInterstitialListenersForLocation.put(location, interstitialListener);
        }

        public void unregisterInterstitialListener(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mInterstitialListenersForLocation.remove(location);
        }

        public void registerRewardedVideoLocation(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mRewardedVideoLocationsToLoad.add(location);
        }

        public void unregisterRewardedVideoLocation(@NonNull String location) {
            Preconditions.checkNotNull(location);
            mRewardedVideoLocationsToLoad.remove(location);
        }

        @NonNull
        public CustomEventInterstitialListener getInterstitialListener(@NonNull String location) {
            final CustomEventInterstitialListener listener = mInterstitialListenersForLocation.get(location);
            return listener != null ? listener : NULL_LISTENER;
        }

        public boolean hasInterstitialLocation(@NonNull String location) {
            return mInterstitialListenersForLocation.containsKey(location);
        }

        //******************
        // Chartboost Delegate methods.
        //******************

        //******************
        // Interstitials
        //******************
        @Override
        public void didCacheInterstitial(String location) {
            SabaVisionLog.d("Chartboost interstitial loaded successfully.");
            getInterstitialListener(location).onInterstitialLoaded();
        }

        @Override
        public void didFailToLoadInterstitial(String location, CBError.CBImpressionError error) {
            String suffix = error != null ? "Error: " + error.name() : "";
            Log.d("SabaVision", "Chartboost interstitial ad failed to load." + suffix);
            getInterstitialListener(location).onInterstitialFailed(SabaVisionErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void didDismissInterstitial(String location) {
            // Note that this method is fired before didCloseInterstitial and didClickInterstitial.
            SabaVisionLog.d("Chartboost interstitial ad dismissed.");
            getInterstitialListener(location).onInterstitialDismissed();
        }

        @Override
        public void didCloseInterstitial(String location) {
            SabaVisionLog.d("Chartboost interstitial ad closed.");
        }

        @Override
        public void didClickInterstitial(String location) {
            SabaVisionLog.d("Chartboost interstitial ad clicked.");
            getInterstitialListener(location).onInterstitialClicked();
        }

        @Override
        public void didDisplayInterstitial(String location) {
            SabaVisionLog.d("Chartboost interstitial ad shown.");
            getInterstitialListener(location).onInterstitialShown();
        }

        //******************
        // Rewarded Videos
        //******************
        @Override
        public void didCacheRewardedVideo(String location) {
            super.didCacheRewardedVideo(location);

            if (mRewardedVideoLocationsToLoad.contains(location)) {
                SabaVisionLog.d("Chartboost rewarded video cached for location " + location + ".");
                SabaVisionRewardedVideoManager.onRewardedVideoLoadSuccess(ChartboostRewardedVideo.class, location);
                mRewardedVideoLocationsToLoad.remove(location);
            }
        }

        @Override
        public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error) {
            super.didFailToLoadRewardedVideo(location, error);
            String suffix = error != null ? " with error: " + error.name() : "";
            if (mRewardedVideoLocationsToLoad.contains(location)) {
                SabaVisionErrorCode errorCode = VIDEO_DOWNLOAD_ERROR;
                SabaVisionLog.d("Chartboost rewarded video cache failed for location " + location + suffix);
                if (CBError.CBImpressionError.INVALID_LOCATION.equals(error)) {
                    errorCode = ADAPTER_CONFIGURATION_ERROR;
                }
                SabaVisionRewardedVideoManager.onRewardedVideoLoadFailure(ChartboostRewardedVideo.class, location, errorCode);
                mRewardedVideoLocationsToLoad.remove(location);
            }
        }

        @Override
        public void didDismissRewardedVideo(String location) {
            // This is called before didCloseRewardedVideo and didClickRewardedVideo
            super.didDismissRewardedVideo(location);
            SabaVisionRewardedVideoManager.onRewardedVideoClosed(ChartboostRewardedVideo.class, location);
            SabaVisionLog.d("Chartboost rewarded video dismissed for location " + location + ".");
        }

        @Override
        public void didCloseRewardedVideo(String location) {
            super.didCloseRewardedVideo(location);
            SabaVisionLog.d("Chartboost rewarded video closed for location " + location + ".");
        }

        @Override
        public void didClickRewardedVideo(String location) {
            super.didClickRewardedVideo(location);
            SabaVisionRewardedVideoManager.onRewardedVideoClicked(ChartboostRewardedVideo.class, location);
            SabaVisionLog.d("Chartboost rewarded video clicked for location " + location + ".");
        }

        @Override
        public void didCompleteRewardedVideo(String location, int reward) {
            super.didCompleteRewardedVideo(location, reward);
            SabaVisionLog.d("Chartboost rewarded video completed for location " + location + " with "
                    + "reward amount " + reward);
            SabaVisionRewardedVideoManager.onRewardedVideoCompleted(
                    ChartboostRewardedVideo.class,
                    location,
                    SabaVisionReward.success(SabaVisionReward.NO_REWARD_LABEL, reward));
        }

        @Override
        public void didDisplayRewardedVideo(String location) {
            super.didDisplayRewardedVideo(location);
            SabaVisionLog.d("Chartboost rewarded video displayed for location " + location + ".");
            SabaVisionRewardedVideoManager.onRewardedVideoStarted(ChartboostRewardedVideo.class, location);
        }

        //******************
        // More Apps
        //******************
        @Override
        public boolean shouldRequestMoreApps(String location) {
            return false;
        }

        @Override
        public boolean shouldDisplayMoreApps(final String location) {
            return false;
        }
    }


    @VisibleForTesting
    @Deprecated
    static void reset() {
        // Clears all the locations to load and other state.
        sDelegate = new ChartboostSingletonDelegate();
        mAppId = null;
        mAppSignature = null;
    }
}
