/*
* ******************************************************************************
* Copyright (c) 2013-2015 Tomas Valenta.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* *****************************************************************************
*/

package cz.yetanotherview.webcamviewer.app.maps;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;

import cz.yetanotherview.webcamviewer.app.R;

public class MapsFragment extends Fragment {

    MapView mMapView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        View view = inflater.inflate(R.layout.maps_layout, container,
                false);

        Intent intent = getActivity().getIntent();
        String name = intent.getExtras().getString("name");
        double latitude = intent.getExtras().getDouble("latitude");
        double longitude = intent.getExtras().getDouble("longitude");

        LatLng latLng = new LatLng(latitude, longitude);
        Marker marker = new Marker(mMapView, name, String.valueOf(latitude) +
                " - " + String.valueOf(longitude), latLng);
        marker.setMarker(getResources().getDrawable(R.drawable.marker));

        mMapView = (MapView) view.findViewById(R.id.mapView);
        mMapView.setCenter(latLng);
        mMapView.setZoom(14);
        mMapView.addMarker(marker);
        mMapView.selectMarker(marker);

        mMapView.setDiskCacheEnabled(false);

        return view;
    }
}
