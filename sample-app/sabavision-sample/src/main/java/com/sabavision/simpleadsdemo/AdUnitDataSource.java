package com.sabavision.simpleadsdemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.sabavision.common.logging.SabaVisionLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.sabavision.simpleadsdemo.SabaVisionSQLiteHelper.COLUMN_AD_TYPE;
import static com.sabavision.simpleadsdemo.SabaVisionSQLiteHelper.COLUMN_AD_UNIT_ID;
import static com.sabavision.simpleadsdemo.SabaVisionSQLiteHelper.COLUMN_DESCRIPTION;
import static com.sabavision.simpleadsdemo.SabaVisionSQLiteHelper.COLUMN_ID;
import static com.sabavision.simpleadsdemo.SabaVisionSQLiteHelper.COLUMN_USER_GENERATED;
import static com.sabavision.simpleadsdemo.SabaVisionSQLiteHelper.TABLE_AD_CONFIGURATIONS;
import static com.sabavision.simpleadsdemo.SabaVisionSampleAdUnit.AdType;
import static com.sabavision.simpleadsdemo.SabaVisionSampleAdUnit.AdType.BANNER;
import static com.sabavision.simpleadsdemo.SabaVisionSampleAdUnit.AdType.INTERSTITIAL;
import static com.sabavision.simpleadsdemo.SabaVisionSampleAdUnit.AdType.REWARDED_VIDEO;

class AdUnitDataSource {
    private Context mContext;
    private SabaVisionSQLiteHelper mDatabaseHelper;
    private String[] mAllColumns = {
            COLUMN_ID,
            COLUMN_AD_UNIT_ID,
            COLUMN_DESCRIPTION,
            COLUMN_USER_GENERATED,
            COLUMN_AD_TYPE
    };

    AdUnitDataSource(final Context context) {
        mContext = context.getApplicationContext();
        mDatabaseHelper = new SabaVisionSQLiteHelper(context);
        populateDefaultSampleAdUnits();
    }

    SabaVisionSampleAdUnit createDefaultSampleAdUnit(final SabaVisionSampleAdUnit sampleAdUnit) {
        return createSampleAdUnit(sampleAdUnit, false);
    }

    SabaVisionSampleAdUnit createSampleAdUnit(final SabaVisionSampleAdUnit sampleAdUnit) {
        return createSampleAdUnit(sampleAdUnit, true);
    }

    private SabaVisionSampleAdUnit createSampleAdUnit(final SabaVisionSampleAdUnit sampleAdUnit,
                                                 final boolean isUserGenerated) {
        final ContentValues values = new ContentValues();
        final int userGenerated = isUserGenerated ? 1 : 0;
        values.put(COLUMN_AD_UNIT_ID, sampleAdUnit.getAdUnitId());
        values.put(COLUMN_DESCRIPTION, sampleAdUnit.getDescription());
        values.put(COLUMN_USER_GENERATED, userGenerated);
        values.put(COLUMN_AD_TYPE, sampleAdUnit.getFragmentClassName());

        final SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        final long insertId = database.insert(TABLE_AD_CONFIGURATIONS, null, values);
        final Cursor cursor = database.query(TABLE_AD_CONFIGURATIONS, mAllColumns,
                COLUMN_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();

        final SabaVisionSampleAdUnit newAdConfiguration = cursorToAdConfiguration(cursor);
        cursor.close();
        database.close();

        if (newAdConfiguration != null) {
            SabaVisionLog.d("Ad configuration added with id: " + newAdConfiguration.getId());
        }
        return newAdConfiguration;
    }

    void deleteSampleAdUnit(final SabaVisionSampleAdUnit adConfiguration) {
        final long id = adConfiguration.getId();
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        database.delete(TABLE_AD_CONFIGURATIONS, COLUMN_ID + " = " + id, null);
        SabaVisionLog.d("Ad Configuration deleted with id: " + id);
        database.close();
    }

    List<SabaVisionSampleAdUnit> getAllAdUnits() {
        final List<SabaVisionSampleAdUnit> adConfigurations = new ArrayList<>();
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        final Cursor cursor = database.query(TABLE_AD_CONFIGURATIONS,
                mAllColumns, null, null, null, null, null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            final SabaVisionSampleAdUnit adConfiguration = cursorToAdConfiguration(cursor);
            adConfigurations.add(adConfiguration);
            cursor.moveToNext();
        }

        cursor.close();
        database.close();
        return adConfigurations;
    }

    List<SabaVisionSampleAdUnit> getDefaultAdUnits() {
        final List<SabaVisionSampleAdUnit> adUnitList = new ArrayList<>();
        adUnitList.add(
                new SabaVisionSampleAdUnit
                        .Builder(mContext.getString(R.string.ad_unit_id_banner), BANNER)
                        .description("SabaVision Banner Sample")
                        .build());

        adUnitList.add(
                new SabaVisionSampleAdUnit
                        .Builder(mContext.getString(R.string.ad_unit_id_interstitial), INTERSTITIAL)
                        .description("SabaVision Interstitial Sample")
                        .build());
        adUnitList.add(
                new SabaVisionSampleAdUnit
                        .Builder(mContext.getString(R.string.ad_unit_id_rewarded_video),
                        REWARDED_VIDEO)
                        .description("SabaVision Rewarded Video Sample")
                        .build());

        return adUnitList;
    }

    private void populateDefaultSampleAdUnits() {
        final HashSet<SabaVisionSampleAdUnit> allAdUnits = new HashSet<>();
        for (final SabaVisionSampleAdUnit adUnit : getAllAdUnits()) {
            allAdUnits.add(adUnit);
        }

        for (final SabaVisionSampleAdUnit defaultAdUnit : getDefaultAdUnits()) {
            if (!allAdUnits.contains(defaultAdUnit)) {
                createDefaultSampleAdUnit(defaultAdUnit);
            }
        }
    }

    private SabaVisionSampleAdUnit cursorToAdConfiguration(final Cursor cursor) {
        final long id = cursor.getLong(0);
        final String adUnitId = cursor.getString(1);
        final String description = cursor.getString(2);
        final int userGenerated = cursor.getInt(3);
        final AdType adType = AdType.fromFragmentClassName(cursor.getString(4));

        if (adType == null) {
            return null;
        }

        return new SabaVisionSampleAdUnit.Builder(adUnitId, adType)
                .description(description)
                .isUserDefined(userGenerated == 1)
                .id(id)
                .build();
    }
}
