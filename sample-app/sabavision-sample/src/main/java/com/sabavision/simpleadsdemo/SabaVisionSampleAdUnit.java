package com.sabavision.simpleadsdemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.Comparator;

class SabaVisionSampleAdUnit implements Comparable<SabaVisionSampleAdUnit> {

    public static final String AD_UNIT_ID = "adUnitId";
    public static final String DESCRIPTION = "description";
    public static final String AD_TYPE = "adType";
    public static final String IS_USER_DEFINED = "isCustom";
    public static final String ID = "id";

    // Note that entries are also sorted in this order
    enum AdType {
        BANNER("Banner", BannerDetailFragment.class),
        INTERSTITIAL("Interstitial", InterstitialDetailFragment.class),
        REWARDED_VIDEO("Rewarded Video", RewardedVideoDetailFragment.class);

        String getName() {
            return name;
        }

        private final String name;
        private final Class<? extends Fragment> fragmentClass;

        AdType(final String name, final Class<? extends Fragment> fragmentClass) {
            this.name = name;
            this.fragmentClass = fragmentClass;
        }

        private Class<? extends Fragment> getFragmentClass() {
            return fragmentClass;
        }

        static AdType fromFragmentClassName(final String fragmentClassName) {
            for (final AdType adType : values()) {
                if (adType.fragmentClass.getName().equals(fragmentClassName)) {
                    return adType;
                }
            }

            return null;
        }
    }

    static final Comparator<SabaVisionSampleAdUnit> COMPARATOR =
            new Comparator<SabaVisionSampleAdUnit>() {
                @Override
                public int compare(SabaVisionSampleAdUnit a, SabaVisionSampleAdUnit b) {
                    return a.compareTo(b);
                }
            };

    static class Builder {
        private final String mAdUnitId;
        private final AdType mAdType;

        private String mDescription;
        private boolean mIsUserDefined;
        private long mId;

        Builder(final String adUnitId, final AdType adType) {
            mAdUnitId = adUnitId;
            mAdType = adType;
            mId = -1;
        }

        Builder description(final String description) {
            mDescription = description;
            return this;
        }

        Builder isUserDefined(boolean userDefined) {
            mIsUserDefined = userDefined;
            return this;
        }

        Builder id(final long id) {
            mId = id;
            return this;
        }

        SabaVisionSampleAdUnit build() {
            return new SabaVisionSampleAdUnit(this);
        }
    }

    private final String mAdUnitId;
    private final AdType mAdType;
    private final String mDescription;
    private final boolean mIsUserDefined;
    private final long mId;

    private SabaVisionSampleAdUnit(final Builder builder) {
        mAdUnitId = builder.mAdUnitId;
        mAdType = builder.mAdType;
        mDescription = builder.mDescription;
        mIsUserDefined = builder.mIsUserDefined;
        mId = builder.mId;
    }

    Class<? extends Fragment> getFragmentClass() {
        return mAdType.getFragmentClass();
    }

    String getAdUnitId() {
        return mAdUnitId;
    }

    String getDescription() {
        return mDescription;
    }

    String getFragmentClassName() {
        return mAdType.getFragmentClass().getName();
    }

    String getHeaderName() {
        return mAdType.name;
    }

    long getId() {
        return mId;
    }

    boolean isUserDefined() {
        return mIsUserDefined;
    }

    Bundle toBundle() {
        final Bundle bundle = new Bundle();
        bundle.putLong(ID, mId);
        bundle.putString(AD_UNIT_ID, mAdUnitId);
        bundle.putString(DESCRIPTION, mDescription);
        bundle.putSerializable(AD_TYPE, mAdType);
        bundle.putBoolean(IS_USER_DEFINED, mIsUserDefined);

        return bundle;
    }

    static SabaVisionSampleAdUnit fromBundle(final Bundle bundle) {
        final Long id = bundle.getLong(ID, -1L);
        final String adUnitId = bundle.getString(AD_UNIT_ID);
        final AdType adType = (AdType) bundle.getSerializable(AD_TYPE);
        final String description = bundle.getString(DESCRIPTION);
        final boolean isUserDefined = bundle.getBoolean(IS_USER_DEFINED, false);
        final Builder builder = new SabaVisionSampleAdUnit.Builder(adUnitId, adType);
        builder.description(description);
        builder.id(id);
        builder.isUserDefined(isUserDefined);

        return builder.build();
    }

    @Override
    public int compareTo(@NonNull SabaVisionSampleAdUnit that) {
        if (mAdType != that.mAdType) {
            return mAdType.ordinal() - that.mAdType.ordinal();
        }

        return mDescription.compareTo(that.mDescription);
    }

    @Override
    public int hashCode() {
        int result = 11;
        result = 31 * result + mAdType.ordinal();
        result = 31 * result + (mIsUserDefined ? 1 : 0);
        result = 31 * result + mDescription.hashCode();
        result = 31 * result + mAdUnitId.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof SabaVisionSampleAdUnit)) {
            return false;
        }

        final SabaVisionSampleAdUnit that = (SabaVisionSampleAdUnit) o;

        return that.mAdType.equals(this.mAdType) &&
                that.mIsUserDefined == this.mIsUserDefined &&
                that.mDescription.equals(this.mDescription) &&
                that.mAdUnitId.equals(this.mAdUnitId);
    }
}
