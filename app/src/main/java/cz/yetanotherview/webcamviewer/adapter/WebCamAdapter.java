package cz.yetanotherview.webcamviewer.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        //webcamViewHolder.vSurname.setText(webcam.surname);
        //webcamViewHolder.vEmail.setText(webcam.email);
        //webcamViewHolder.vTitle.setText(webcam.name + " " + ci.surname);
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

        public WebCamViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            vName = (TextView) itemLayoutView.findViewById(R.id.titleTextView);
        }
    }
}