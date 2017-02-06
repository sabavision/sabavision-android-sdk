package com.sabavision.simpleadsdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.sabavision.mobileads.SabaVisionErrorCode;
import com.sabavision.mobileads.SabaVisionInterstitial;

import static com.sabavision.mobileads.SabaVisionInterstitial.InterstitialAdListener;
import static com.sabavision.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.sabavision.simpleadsdemo.Utils.logToast;

public class InterstitialDetailFragment extends Fragment implements InterstitialAdListener {
    private SabaVisionInterstitial mSabaVisionInterstitial;
    private Button mShowButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final SabaVisionSampleAdUnit adConfiguration =
                SabaVisionSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.interstitial_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        hideSoftKeyboard(views.mKeywordsField);

        final String adUnitId = adConfiguration.getAdUnitId();
        views.mDescriptionView.setText(adConfiguration.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShowButton.setEnabled(false);
                if (mSabaVisionInterstitial == null) {
                    mSabaVisionInterstitial = new SabaVisionInterstitial(getActivity(), adUnitId);
                    mSabaVisionInterstitial.setInterstitialAdListener(InterstitialDetailFragment.this);
                }
                final String keywords = views.mKeywordsField.getText().toString();
                mSabaVisionInterstitial.setKeywords(keywords);
                mSabaVisionInterstitial.load();
            }
        });
        mShowButton = (Button) view.findViewById(R.id.interstitial_show_button);
        mShowButton.setEnabled(false);
        mShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSabaVisionInterstitial.show();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mSabaVisionInterstitial != null) {
            mSabaVisionInterstitial.destroy();
            mSabaVisionInterstitial = null;
        }
    }

    // InterstitialAdListener implementation
    @Override
    public void onInterstitialLoaded(SabaVisionInterstitial interstitial) {
        mShowButton.setEnabled(true);
        logToast(getActivity(), "Interstitial loaded.");
    }

    @Override
    public void onInterstitialFailed(SabaVisionInterstitial interstitial, SabaVisionErrorCode errorCode) {
        final String errorMessage = (errorCode != null) ? errorCode.toString() : "";
        logToast(getActivity(), "Interstitial failed to load: " + errorMessage);
    }

    @Override
    public void onInterstitialShown(SabaVisionInterstitial interstitial) {
        mShowButton.setEnabled(false);
        logToast(getActivity(), "Interstitial shown.");
    }

    @Override
    public void onInterstitialClicked(SabaVisionInterstitial interstitial) {
        logToast(getActivity(), "Interstitial clicked.");
    }

    @Override
    public void onInterstitialDismissed(SabaVisionInterstitial interstitial) {
        logToast(getActivity(), "Interstitial dismissed.");
    }
}
