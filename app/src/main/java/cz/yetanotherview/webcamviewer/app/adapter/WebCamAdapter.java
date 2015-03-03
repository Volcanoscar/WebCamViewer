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

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.signature.StringSignature;

import java.util.List;
import java.util.Random;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class WebCamAdapter extends RecyclerView.Adapter<WebCamAdapter.WebCamViewHolder> {

    private final int mLayoutId;
    private final int mOrientation;
    private int minHeight;
    private boolean mImageOn;

    private StringSignature mStringSignature;

    private final Context mContext;
    private List<WebCam> webCamItems;
    private ClickListener clickListener;

    public WebCamAdapter(Context context, List<WebCam> webCamItems, int orientation, int layoutId, StringSignature stringSignature, boolean imageOn) {
        this.webCamItems = webCamItems;
        mContext = context;
        mLayoutId = layoutId;
        mOrientation = orientation;
        mStringSignature = stringSignature;
        mImageOn = imageOn;
    }

    public void swapData(List<WebCam> webCamItems) {
        this.webCamItems = webCamItems;
        notifyDataSetChanged();
    }

    @Override
    public WebCamViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (mLayoutId == 1) {
            if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
                return new WebCamViewHolder(LayoutInflater.from(mContext).inflate(R.layout.webcam_layout_list_pre_lollipop, viewGroup, false));
            }
            else return new WebCamViewHolder(LayoutInflater.from(mContext).inflate(R.layout.webcam_layout_list, viewGroup, false));
        }
        else return new WebCamViewHolder(LayoutInflater.from(mContext).inflate(R.layout.webcam_layout_grid, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(WebCamViewHolder webcamViewHolder, int position) {
        WebCam webCam = webCamItems.get(position);
        webcamViewHolder.vName.setText(webCam.getName());
        webcamViewHolder.vProgress.setVisibility(View.VISIBLE);

        if (webcamViewHolder.vImage.getWidth() == 0) {

            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int mWidth = size.x;

            if (mLayoutId == 1) {
                minHeight = (int) (mWidth * 0.67);
            }
            else if (mLayoutId == 2) {
                minHeight = (int) ((mWidth * 0.67) / 2);
            }
            else if (mLayoutId == 3) {
                minHeight = (int) ((mWidth * 0.67) / 3);
            }
        }
        webcamViewHolder.vImage.setMinimumHeight(minHeight);

        if (mImageOn) {
            loadImages(webcamViewHolder, webCam.getUrl());
        }
        else {
            webcamViewHolder.vProgress.setVisibility(View.GONE);

            Random r = new Random();
            int[] res = {R.drawable.no_image_0, R.drawable.no_image_1, R.drawable.no_image_2,
                    R.drawable.no_image_3, R.drawable.no_image_4};

            int rndInt = r.nextInt(res .length);
            webcamViewHolder.vImage.setImageDrawable(mContext.getResources().getDrawable(res[rndInt]));
        }
    }

    private void loadImages(final WebCamViewHolder webcamViewHolder, String source) {

        if (mLayoutId == 1) {
            Glide.with(mContext)
                    .load(source)
                    .centerCrop()
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .signature(mStringSignature)
                    .into(new GlideDrawableImageViewTarget(webcamViewHolder.vImage) {
                        @Override
                        public void onResourceReady(GlideDrawable drawable, GlideAnimation anim) {
                            super.onResourceReady(drawable, anim);
                            webcamViewHolder.vProgress.setVisibility(View.GONE);
                        }
                    });
        }
        else {
            Glide.with(mContext)
                    .load(source)
                    .dontTransform()
                    .crossFade()
                    .signature(mStringSignature)
                    .into(new GlideDrawableImageViewTarget(webcamViewHolder.vImage) {
                        @Override
                        public void onResourceReady(GlideDrawable drawable, GlideAnimation anim) {
                            super.onResourceReady(drawable, anim);
                            webcamViewHolder.vProgress.setVisibility(View.GONE);
                        }
                    });
        }
    }

    public class WebCamViewHolder extends RecyclerView.ViewHolder implements OnClickListener {

        protected TextView vName;
        protected ImageView vImage;
        protected ImageButton vButton;

        protected ProgressBar vProgress;

        public WebCamViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            vName = (TextView) itemLayoutView.findViewById(R.id.titleTextView);
            vImage = (ImageView) itemLayoutView.findViewById(R.id.imageView);
            vButton = (ImageButton) itemLayoutView.findViewById(R.id.action_edit);

            vProgress = (ProgressBar) itemLayoutView.findViewById(R.id.loadingProgressBar);

            vImage.setOnClickListener(this);
            vButton.setOnClickListener(this);

            final int small = mContext.getResources().getDimensionPixelSize(R.dimen.small_padding);
            final int middle = mContext.getResources().getDimensionPixelSize(R.dimen.middle_padding);
            final int big = mContext.getResources().getDimensionPixelSize(R.dimen.big_padding);

            if (mLayoutId == 1) {
                vName.setTextSize(28);
                vName.setPadding(big,0,0,middle);
            }
            else if (mLayoutId == 2 && mOrientation == 1) {
                vName.setTextSize(16);
                vName.setPadding(middle,0,0,small);
                vButton.setMaxHeight(120);
            }
            else if (mLayoutId == 2 && mOrientation == 2) {
                vName.setTextSize(26);
                vName.setPadding(middle,0,0,small);
            }
            else if (mLayoutId == 3) {
                vName.setTextSize(19);
                vName.setPadding(small,0,0,0);
                vButton.setMaxHeight(120);
            }
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

    public void refreshViewImages(StringSignature stringSignature) {
        mStringSignature = stringSignature;
        notifyDataSetChanged();
    }
}
