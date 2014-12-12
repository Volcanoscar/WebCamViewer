/*
* ******************************************************************************
* Copyright (c) 2013-2014 Tomas Valenta.
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

package cz.yetanotherview.webcamviewer.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import cz.yetanotherview.webcamviewer.R;
import cz.yetanotherview.webcamviewer.model.Webcam;

public class WebCamAdapter extends RecyclerView.Adapter<WebCamAdapter.WebCamViewHolder> {

    private List<Webcam> webcamItems;

    public WebCamAdapter(List<Webcam> webcamItems) {
        this.webcamItems = webcamItems;
    }

    @Override
    public int getItemCount() {
        return webcamItems.size();
    }

    public Object getItem(int location) {
        return webcamItems.get(location);
    }

    @Override
    public void onBindViewHolder(WebCamViewHolder webcamViewHolder, int i) {
        Webcam webcam = webcamItems.get(i);
        webcamViewHolder.vName.setText(webcam.getName());

        Picasso.with(webcamViewHolder.itemView.getContext())
                .load(webcam.getUrl())
                .fit()
                .placeholder(R.drawable.animation)
                .error(R.drawable.placeholder_error)
                .into(webcamViewHolder.vImage);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public WebCamViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // create a new view
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.webcam_layout, viewGroup, false);
        // set the view's size, margins, paddings and layout parameters
        return new WebCamViewHolder(itemView);
    }

    public static class WebCamViewHolder extends RecyclerView.ViewHolder {

        protected TextView vName;
        protected ImageView vImage;

        public WebCamViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            vName = (TextView) itemLayoutView.findViewById(R.id.titleTextView);
            vImage = (ImageView) itemLayoutView.findViewById(R.id.imageView);
        }
    }
}