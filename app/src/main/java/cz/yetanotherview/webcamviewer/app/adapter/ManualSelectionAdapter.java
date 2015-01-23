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

package cz.yetanotherview.webcamviewer.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class ManualSelectionAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private List<WebCam> webCamList;
    private List<WebCam> origList;

    public ManualSelectionAdapter(Context context, List<WebCam> webCamList) {
        super();
        this.context = context;
        this.webCamList = webCamList;
    }


    public class ViewHolder
    {
        CheckBox selCheckBox;
    }

    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults oReturn = new FilterResults();
                final List<WebCam> results = new ArrayList<>();
                if (origList == null)
                    origList = webCamList;
                if (constraint != null) {
                    if (origList != null && origList.size() > 0) {
                        for (final WebCam g : origList) {
                            String strippedName = Utils.getNameStrippedAccents(g.getName() + " " + g.getCountry());
                            if (strippedName.toLowerCase()
                                    .contains(constraint.toString()))
                                results.add(g);
                        }
                    }
                    oReturn.values = results;
                }
                return oReturn;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                webCamList = (List<WebCam>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return webCamList.size();
    }

    @Override
    public Object getItem(int position) {
        return webCamList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView==null)
        {
            convertView = LayoutInflater.from(context).inflate(R.layout.filtered_list_view_item, parent, false);
            holder = new ViewHolder();
            holder.selCheckBox = (CheckBox) convertView.findViewById(R.id.sel_checkbox);
            convertView.setTag(holder);

            holder.selCheckBox.setOnClickListener( new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    CheckBox cb = (CheckBox) v;
                    WebCam webCam = (WebCam) cb.getTag();

                    webCam.setSelected(cb.isChecked());
                }
            });
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }

        WebCam webCam = webCamList.get(position);

        holder.selCheckBox.setText(webCamList.get(position).getName()
                + " (" + webCamList.get(position).getCountry() + ")");
        holder.selCheckBox.setChecked(webCam.isSelected());
        holder.selCheckBox.setTag(webCam);

        return convertView;
    }
}
