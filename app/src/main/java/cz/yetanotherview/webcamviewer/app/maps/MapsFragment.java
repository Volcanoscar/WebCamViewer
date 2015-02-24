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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import cz.yetanotherview.webcamviewer.app.R;

public class MapsFragment extends Fragment {

    private WebView viewContentWebView;
    private ProgressBar viewContentProgress;

    private String url;

    private boolean resetHistory = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.maps_layout, container, false);

        Intent intent = getActivity().getIntent();
        String coordinates = intent.getExtras().getString("coordinates");
        String baseUrl = "http://maps.google.com?q=";
        url = baseUrl + coordinates;

        viewContentProgress = (ProgressBar) view.findViewById(R.id.progress);
        viewContentWebView = (WebView) view.findViewById(R.id.webView);
        viewContentWebView.getSettings().setJavaScriptEnabled(true);
        viewContentWebView.getSettings().setSaveFormData(false);
        viewContentWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
        viewContentWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                viewContentProgress.setProgress(newProgress);
                viewContentProgress.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);

                if (newProgress == 100 && resetHistory) {
                    viewContentWebView.clearHistory();
                    resetHistory = false;
                }
            }
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reload();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden)
            viewContentWebView.stopLoading();
        else
            reload();
    }

    public void reload() {
        if (TextUtils.isEmpty(url))
            return;

        viewContentWebView.loadUrl(url);
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        viewContentWebView.clearCache(true);
        viewContentWebView.clearHistory();
        Log.d("Clean", "?!?");
    }
}
