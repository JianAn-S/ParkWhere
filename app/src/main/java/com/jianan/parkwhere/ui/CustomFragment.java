package com.jianan.parkwhere.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.jianan.parkwhere.R;

public abstract class CustomFragment extends Fragment {
    private Toolbar customToolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflateFragmentLayout(inflater, container, savedInstanceState);

        if (rootView instanceof ViewGroup) {
            setupCustomActionBar((ViewGroup) rootView);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupActionBarTitle();
    }

    private void setupCustomActionBar(ViewGroup rootView) {
        // Inflate the custom tool bar
        View customActionBarView = LayoutInflater.from(getContext()).inflate(R.layout.custom_tool_bar, rootView, false);
        customToolbar = (Toolbar) customActionBarView;

        // Add the custom tool bar to the top of the root view
        rootView.addView(customActionBarView, 0);

        // Set tool bar as action bar
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(customToolbar);
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }
    }

    private void setupActionBarTitle() {
        if (customToolbar != null) {
            TextView titleView = customToolbar.findViewById(R.id.toolbar_title);
            titleView.setText(getActionBarTitle());
        }
    }

    // Abstract methods for subclasses to implement
    protected abstract View inflateFragmentLayout(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);
    protected abstract String getActionBarTitle();
}
