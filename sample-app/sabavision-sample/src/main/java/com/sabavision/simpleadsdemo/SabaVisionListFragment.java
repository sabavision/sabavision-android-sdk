package com.sabavision.simpleadsdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sabavision.common.SabaVision;
import com.sabavision.common.logging.SabaVisionLog;

import java.util.ArrayList;
import java.util.List;

import static com.sabavision.simpleadsdemo.SabaVisionSampleAdUnit.AdType;


interface TrashCanClickListener {
    void onTrashCanClicked(SabaVisionSampleAdUnit adUnit);
}

public class SabaVisionListFragment extends ListFragment implements TrashCanClickListener {
    private SabaVisionSampleListAdapter mAdapter;
    private AdUnitDataSource mAdUnitDataSource;

    private static final AdType[] adTypes = AdType.values();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeAdapter();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.ad_unit_list_fragment, container, false);
        final Button button = (Button) view.findViewById(R.id.add_ad_unit_button);
        final TextView versionCodeView = (TextView) view.findViewById(R.id.version_code);
        versionCodeView.setText("SDK Version " + SabaVision.SDK_VERSION);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onAddClicked(view);
            }
        });

        return view;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        final SabaVisionSampleAdUnit adConfiguration = mAdapter.getItem(position);

        final FragmentTransaction fragmentTransaction =
                getActivity().getSupportFragmentManager().beginTransaction();

        final Class<? extends Fragment> fragmentClass = adConfiguration.getFragmentClass();
        final Fragment fragment;

        try {
            fragment = fragmentClass.newInstance();
        } catch (java.lang.InstantiationException e) {
            SabaVisionLog.e("Error creating fragment for class " + fragmentClass, e);
            return;
        } catch (IllegalAccessException e) {
            SabaVisionLog.e("Error creating fragment for class " + fragmentClass, e);
            return;
        }

        fragment.setArguments(adConfiguration.toBundle());

        fragmentTransaction
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onTrashCanClicked(final SabaVisionSampleAdUnit adUnit) {
        final DialogFragment deleteConfirmation = DeleteDialogFragment.newInstance(adUnit);
        deleteConfirmation.setTargetFragment(this, 0);
        deleteConfirmation.show(getActivity().getSupportFragmentManager(), "delete");
    }

    public void onAddClicked(final View view) {
        final AddDialogFragment dialogFragment = AddDialogFragment.newInstance();
        dialogFragment.setTargetFragment(this, 0);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "add");
    }

    @Override
    public void onResume() {
        super.onResume();
        Utils.hideSoftKeyboard(getListView());
    }

    private void initializeAdapter() {
        mAdapter = new SabaVisionSampleListAdapter(getActivity(), this);

        mAdUnitDataSource = new AdUnitDataSource(getActivity());

        // If you have a large amount of data, this loading work should be done in the background.
        final List<SabaVisionSampleAdUnit> adUnits = mAdUnitDataSource.getAllAdUnits();
        for (final SabaVisionSampleAdUnit adUnit : adUnits) {
            mAdapter.add(adUnit);
        }

        mAdapter.sort(SabaVisionSampleAdUnit.COMPARATOR);
        setListAdapter(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    void addAdUnit(final SabaVisionSampleAdUnit sabavisionSampleAdUnit) {
        SabaVisionSampleAdUnit createdAdUnit = mAdUnitDataSource.createSampleAdUnit(sabavisionSampleAdUnit);
        mAdapter.add(createdAdUnit);
        mAdapter.sort(SabaVisionSampleAdUnit.COMPARATOR);
    }

    void deleteAdUnit(final SabaVisionSampleAdUnit sabavisionSampleAdUnit) {
        mAdUnitDataSource.deleteSampleAdUnit(sabavisionSampleAdUnit);
        mAdapter.remove(sabavisionSampleAdUnit);
        mAdapter.sort(SabaVisionSampleAdUnit.COMPARATOR);
    }

    public static class DeleteDialogFragment extends DialogFragment {
        public static DeleteDialogFragment newInstance(SabaVisionSampleAdUnit adUnit) {
            final DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
            Bundle args = adUnit.toBundle();
            deleteDialogFragment.setArguments(args);
            return deleteDialogFragment;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Bundle args = getArguments();

            return new AlertDialog.Builder(getActivity())
                    .setTitle("Delete Ad Unit " + args.getString(SabaVisionSampleAdUnit.DESCRIPTION) + "?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            final SabaVisionListFragment listFragment = (SabaVisionListFragment) getTargetFragment();
                            listFragment.deleteAdUnit(SabaVisionSampleAdUnit.fromBundle(args));
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            dismiss();
                        }
                    })
                    .setCancelable(true)
                    .create();
        }
    }

    public static class AddDialogFragment extends DialogFragment {
        public static AddDialogFragment newInstance() {
            return new AddDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Add a custom Ad Unit")
                    .setPositiveButton("Save ad unit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            AlertDialog dialog = (AlertDialog) dialogInterface;
                            final EditText adUnitIdField =
                                    (EditText) dialog.findViewById(R.id.add_ad_unit_id);
                            final Spinner adTypeSpinner =
                                    (Spinner) dialog.findViewById(R.id.add_ad_unit_type);
                            final EditText descriptionField =
                                    (EditText) dialog.findViewById(R.id.add_ad_unit_description);

                            // Verify data:
                            try {
                                Utils.validateAdUnitId(adUnitIdField.getText().toString());
                            } catch (IllegalArgumentException e) {
                                // Input is not valid.
                                Toast toast = Toast.makeText(getActivity(), "Ad Unit ID invalid",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                return;
                            }

                            // Create ad unit and save it in the database:
                            final String adUnitId = adUnitIdField.getText().toString();
                            final AdType adType = adTypes[adTypeSpinner.getSelectedItemPosition()];
                            final String description = descriptionField.getText().toString();
                            final SabaVisionSampleAdUnit sampleAdUnit =
                                    new SabaVisionSampleAdUnit.Builder(adUnitId, adType)
                                            .description(description)
                                            .isUserDefined(true)
                                            .build();
                            ((SabaVisionListFragment) getTargetFragment()).addAdUnit(sampleAdUnit);
                            dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            dismiss();
                        }
                    })
                    .setCancelable(true)
                    .create();

            // Inflate and add our custom layout to the dialog.
            final View view = dialog.getLayoutInflater()
                    .inflate(R.layout.ad_config_dialog, null);
            final Spinner spinner = (Spinner) view.findViewById(R.id.add_ad_unit_type);
            final List<String> adTypeStrings = new ArrayList<String>(adTypes.length);

            for (final AdType adType : adTypes) {
                adTypeStrings.add(adType.getName());
            }

            spinner.setAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, adTypeStrings));
            dialog.setView(view);
            return dialog;
        }
    }
}

