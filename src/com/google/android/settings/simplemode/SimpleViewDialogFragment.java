package com.google.android.settings.simplemode;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class SimpleViewDialogFragment extends DialogFragment {
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        super.onCreateDialog(bundle);
        final FragmentActivity activity = getActivity();
        int dialogMode = getArguments().getInt("dialog_mode");
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        View viewInflate =
                LayoutInflater.from(activity)
                        .inflate(R.layout.simple_view_dialog, (ViewGroup) null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(viewInflate);
        AlertDialog alertDialog = builder.create();
        TextView titleView = (TextView) viewInflate.findViewById(R.id.simple_view_dialog_title);
        TextView descriptionView =
                (TextView) viewInflate.findViewById(R.id.simple_view_dialog_description);
        Button positiveButton =
                (Button) viewInflate.findViewById(R.id.simple_view_dialog_positive_button);
        Button negativeButton =
                (Button) viewInflate.findViewById(R.id.simple_view_dialog_negative_button);
        final boolean isCallingFromSUWEntryPoint =
                SimpleModeUtils.isCallingFromSUWEntryPoint(activity);
        if (dialogMode == 0) {
            titleView.setText(R.string.simple_view_feature_enabled_dialog_title);
            descriptionView.setText(R.string.simple_view_feature_enabled_dialog_message);
            positiveButton.setOnClickListener(
                    view -> {
                        SimpleModeUtils.enableSimpleView(activity, isCallingFromSUWEntryPoint);
                        dismiss();
                    });
            negativeButton.setOnClickListener(view -> dismiss());
            return alertDialog;
        }
        if (dialogMode == 1) {
            titleView.setText(R.string.simple_view_feature_disabled_dialog_title);
            descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
            descriptionView.setText(R.string.simple_view_feature_disabled_dialog_message);
            positiveButton.setOnClickListener(
                    view -> {
                        SimpleModeUtils.disableSimpleView(activity, isCallingFromSUWEntryPoint);
                        dismiss();
                    });
            negativeButton.setOnClickListener(view -> dismiss());
        }
        return alertDialog;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        getParentFragmentManager().setFragmentResult("simple_view", Bundle.EMPTY);
    }
}
