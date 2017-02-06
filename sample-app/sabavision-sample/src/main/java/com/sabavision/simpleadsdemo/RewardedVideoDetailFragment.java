package com.sabavision.simpleadsdemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.sabavision.common.SabaVision;
import com.sabavision.common.SabaVisionReward;
import com.sabavision.mobileads.SabaVisionErrorCode;
import com.sabavision.mobileads.SabaVisionRewardedVideoListener;
import com.sabavision.mobileads.SabaVisionRewardedVideoManager.RequestParameters;

import java.util.Locale;
import java.util.Set;

import static com.sabavision.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.sabavision.simpleadsdemo.Utils.logToast;

public class RewardedVideoDetailFragment extends Fragment implements SabaVisionRewardedVideoListener {

    private static boolean sRewardedVideoInitialized;

    @Nullable private Button mShowButton;
    @Nullable private String mAdUnitId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final SabaVisionSampleAdUnit adConfiguration =
                SabaVisionSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.interstitial_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        hideSoftKeyboard(views.mKeywordsField);

        if (!sRewardedVideoInitialized) {
            SabaVision.initializeRewardedVideo(getActivity());
            sRewardedVideoInitialized = true;
        }
        SabaVision.setRewardedVideoListener(this);

        mAdUnitId = adConfiguration.getAdUnitId();
        views.mDescriptionView.setText(adConfiguration.getDescription());
        views.mAdUnitIdView.setText(mAdUnitId);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdUnitId == null) {
                    return;
                }
                SabaVision.loadRewardedVideo(mAdUnitId,
                        new RequestParameters(views.mKeywordsField.getText().toString(), null,
                                "sample_app_customer_id"));
                if (mShowButton != null) {
                    mShowButton.setEnabled(false);
                }
            }
        });
        mShowButton = (Button) view.findViewById(R.id.interstitial_show_button);
        mShowButton.setEnabled(false);
        mShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdUnitId == null) {
                    return;
                }
                SabaVision.showRewardedVideo(mAdUnitId);
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // SabaVisionRewardedVideoListener implementation
    @Override
    public void onRewardedVideoLoadSuccess(@NonNull final String adUnitId) {
        if (adUnitId.equals(mAdUnitId)) {
            if (mShowButton != null) {
                mShowButton.setEnabled(true);
            }
            logToast(getActivity(), "Rewarded video loaded.");
        }
    }

    @Override
    public void onRewardedVideoLoadFailure(@NonNull final String adUnitId, @NonNull final SabaVisionErrorCode errorCode) {
        if (adUnitId.equals(mAdUnitId)) {
            if (mShowButton != null) {
                mShowButton.setEnabled(false);
            }
            logToast(getActivity(), String.format(Locale.US, "Rewarded video failed to load: %s",
                    errorCode.toString()));
        }
    }

    @Override
    public void onRewardedVideoStarted(@NonNull final String adUnitId) {
        if (adUnitId.equals(mAdUnitId)) {
            logToast(getActivity(), "Rewarded video started.");
            if (mShowButton != null) {
                mShowButton.setEnabled(false);
            }
        }
    }

    @Override
    public void onRewardedVideoPlaybackError(@NonNull final String adUnitId, @NonNull final SabaVisionErrorCode errorCode) {
        if (adUnitId.equals(mAdUnitId)) {
            logToast(getActivity(), String.format(Locale.US, "Rewarded video playback error: %s",
                    errorCode.toString()));
            if (mShowButton != null) {
                mShowButton.setEnabled(false);
            }
        }
    }

    @Override
    public void onRewardedVideoClosed(@NonNull final String adUnitId) {
        if (adUnitId.equals(mAdUnitId)) {
            logToast(getActivity(), "Rewarded video closed.");
            if (mShowButton != null) {
                mShowButton.setEnabled(false);
            }
        }
    }

    @Override
    public void onRewardedVideoCompleted(@NonNull final Set<String> adUnitIds,
            @NonNull final SabaVisionReward reward) {
        if (adUnitIds.contains(mAdUnitId)) {
            logToast(getActivity(),
                    String.format(Locale.US,
                            "Rewarded video completed with reward  \"%d %s\"",
                            reward.getAmount(),
                            reward.getLabel()));
        }
    }
}
