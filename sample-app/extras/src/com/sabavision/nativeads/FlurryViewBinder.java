package com.sabavision.nativeads;

import android.support.annotation.NonNull;

/**
 * Tested with Flurry 6.5.0
 */
public class FlurryViewBinder {
    ViewBinder staticViewBinder;
    int videoViewId;

    private FlurryViewBinder(@NonNull Builder builder) {
        this.staticViewBinder = builder.staticViewBinder;
        this.videoViewId = builder.videoViewId;
    }

    public final static class Builder {
        ViewBinder staticViewBinder;
        int videoViewId;

        public Builder(final ViewBinder staticViewBinder) {
            this.staticViewBinder = staticViewBinder;
        }

        @NonNull
        public final Builder videoViewId(final int videoViewId) {
            this.videoViewId = videoViewId;
            return this;
        }

        @NonNull
        public final FlurryViewBinder build() {
            return new FlurryViewBinder(this);
        }
    }
}
