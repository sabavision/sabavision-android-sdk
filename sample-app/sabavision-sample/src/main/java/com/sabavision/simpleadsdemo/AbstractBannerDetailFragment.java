package com.sabavision.simpleadsdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sabavision.mobileads.SabaVisionErrorCode;
import com.sabavision.mobileads.SabaVisionView;

import static com.sabavision.mobileads.SabaVisionView.BannerAdListener;
import static com.sabavision.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.sabavision.simpleadsdemo.Utils.logToast;

/**
 * A base class for creating banner style ads with various height and width dimensions.
 * <p>
 * A subclass simply needs to specify the height and width of the ad in pixels, and this class will
 * inflate a layout containing a programmatically rescaled {@link SabaVisionView} that will be used to
 * display the ad.
 */
public abstract class AbstractBannerDetailFragment extends Fragment implements BannerAdListener {
    private SabaVisionView mSabaVisionView;
    private SabaVisionSampleAdUnit mSabaVisionSampleAdUnit;

    public abstract int getWidth();

    public abstract int getHeight();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.banner_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);

        mSabaVisionSampleAdUnit = SabaVisionSampleAdUnit.fromBundle(getArguments());
        mSabaVisionView = (SabaVisionView) view.findViewById(R.id.banner_sabavisionview);
        LinearLayout.LayoutParams layoutParams =
                (LinearLayout.LayoutParams) mSabaVisionView.getLayoutParams();
        layoutParams.width = getWidth();
        layoutParams.height = getHeight();
        mSabaVisionView.setLayoutParams(layoutParams);

        hideSoftKeyboard(views.mKeywordsField);

        final String adUnitId = mSabaVisionSampleAdUnit.getAdUnitId();
        views.mDescriptionView.setText(mSabaVisionSampleAdUnit.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String keywords = views.mKeywordsField.getText().toString();
                loadSabaVisionView(adUnitId, keywords);
            }
        });
        mSabaVisionView.setBannerAdListener(this);
        loadSabaVisionView(adUnitId, null);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mSabaVisionView != null) {
            mSabaVisionView.destroy();
            mSabaVisionView = null;
        }
    }

    private void loadSabaVisionView(final String adUnitId, final String keywords) {
        mSabaVisionView.setAdUnitId(adUnitId);
        mSabaVisionView.setKeywords(keywords);
        mSabaVisionView.loadAd();
    }

    private String getName() {
        if (mSabaVisionSampleAdUnit == null) {
            return SabaVisionSampleAdUnit.AdType.BANNER.getName();
        }
        return mSabaVisionSampleAdUnit.getHeaderName();
    }

    // BannerAdListener
    @Override
    public void onBannerLoaded(SabaVisionView banner) {
        logToast(getActivity(), getName() + " loaded.");
    }

    @Override
    public void onBannerFailed(SabaVisionView banner, SabaVisionErrorCode errorCode) {
        final String errorMessage = (errorCode != null) ? errorCode.toString() : "";
        logToast(getActivity(), getName() + " failed to load: " + errorMessage);
    }

    @Override
    public void onBannerClicked(SabaVisionView banner) {
        logToast(getActivity(), getName() + " clicked.");
    }

    @Override
    public void onBannerExpanded(SabaVisionView banner) {
        logToast(getActivity(), getName() + " expanded.");
    }

    @Override
    public void onBannerCollapsed(SabaVisionView banner) {
        logToast(getActivity(), getName() + " collapsed.");
    }
}
