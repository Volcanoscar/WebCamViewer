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
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.model.Category;

public class CategoryAdapter extends ArrayAdapter<Category> {

    public CategoryAdapter(Context context, ArrayList<Category> categories) {
        super(context, 0, categories);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Category category = getItem(position);
        DatabaseHelper db = new DatabaseHelper(getContext());

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.drawer_list_item, parent, false);
        }

        TextView categoryName = (TextView) convertView.findViewById(R.id.categoryName);
        TextView categoryCount = (TextView) convertView.findViewById(R.id.categoryCount);

        categoryName.setTypeface(null, Typeface.NORMAL);
        categoryCount.setTypeface(null, Typeface.NORMAL);
        if (((ListView)parent).isItemChecked(position)) {
            categoryName.setTypeface(null, Typeface.BOLD);
            categoryCount.setTypeface(null, Typeface.BOLD);
        }

        categoryName.setText(category.getcategoryName());
        categoryCount.setText("(" + category.getCountAsString() + ")");
        db.closeDB();

        return convertView;
    }
}
