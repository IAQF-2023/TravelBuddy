package com.example.travelbuddy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mView;
    private final LayoutInflater layoutInflater;
    private final Context context;

    public CustomInfoWindowAdapter(Context context) {
        mView = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null; // Use the default InfoWindow frame
    }

    @Nullable
    @Override
    public View getInfoContents(Marker marker) {
        View view = layoutInflater.inflate(R.layout.custom_info_window, null);

        TextView titleView = view.findViewById(R.id.title);
        TextView snippetView = view.findViewById(R.id.snippet);

        titleView.setText(marker.getTitle());
        snippetView.setText(marker.getSnippet());

        return view;
    }
}


