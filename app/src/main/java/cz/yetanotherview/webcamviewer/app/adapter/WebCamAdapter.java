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

package cz.yetanotherview.webcamviewer.app.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class WebCamAdapter extends RecyclerView.Adapter<WebCamAdapter.WebCamViewHolder> {

    private List<WebCam> webCamItems;
    private ClickListener clickListener;

    public WebCamAdapter(List<WebCam> webCamItems) {
        this.webCamItems = webCamItems;
    }

    public void swapData(List<WebCam> webCamItems) {
        this.webCamItems = webCamItems;
        notifyDataSetChanged();
    }

    @Override
    public WebCamViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.webcam_layout, viewGroup, false);
        return new WebCamViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(WebCamViewHolder webcamViewHolder, int i) {
        WebCam webCam = webCamItems.get(i);
        webcamViewHolder.vName.setText(webCam.getName());

        //Picasso.with(webcamViewHolder.itemView.getContext()).setIndicatorsEnabled(true);
        Picasso.with(webcamViewHolder.itemView.getContext())
                .load(webCam.getUrl())
                .skipMemoryCache()
                .fit()
                .transform(new SizeAndRoundTransform(6, 0))
                .placeholder(R.drawable.animation)
                .error(R.drawable.placeholder_error)
                .into(webcamViewHolder.vImage);
    }

    public class WebCamViewHolder extends RecyclerView.ViewHolder implements OnClickListener {

        protected TextView vName;
        protected ImageView vImage;
        protected ImageButton vButton;

        public WebCamViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            vName = (TextView) itemLayoutView.findViewById(R.id.titleTextView);
            vImage = (ImageView) itemLayoutView.findViewById(R.id.imageView);
            vButton = (ImageButton) itemLayoutView.findViewById(R.id.action_edit);

            vImage.setOnClickListener(this);
            vButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v instanceof ImageButton){
                clickListener.onClick(v, getPosition(), true);
            } else {
                clickListener.onClick(v, getPosition(), false);
            }
        }
    }

    public interface ClickListener {

        public void onClick(View v, int position, boolean isEditClick);
    }

    /* Setter for listener. */
    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public int getItemCount() {
        return webCamItems.size();
    }

    public Object getItem(int location) {
        return webCamItems.get(location);
    }

    public WebCam getItemAt(int position) {
        if (position < webCamItems.size())
            return webCamItems.get(position);
        return null;
    }

    public void addItem(int position, WebCam webCam) {
        webCamItems.add(position, webCam);
        notifyItemInserted(position);
    }

    public void modifyItem(int position, WebCam webCam) {
        webCamItems.set(position, webCam);
        notifyItemChanged(position);
    }

    public void removeItem(WebCam item) {
        int position = webCamItems.indexOf(item);
        webCamItems.remove(position);
        notifyItemRemoved(position);
    }
}