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

package cz.yetanotherview.webcamviewer.actions;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.util.Random;

import cz.yetanotherview.webcamviewer.MainActivity;
import cz.yetanotherview.webcamviewer.R;
import cz.yetanotherview.webcamviewer.db.DatabaseHelper;
import cz.yetanotherview.webcamviewer.model.Category;
import cz.yetanotherview.webcamviewer.model.Webcam;

public class AddWebcam extends Activity implements OnClickListener {

    private Button addWebcamBtn;
    private EditText subjectEditText;
    private EditText descEditText;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Add Record");

        setContentView(R.layout.add_webcam);

        subjectEditText = (EditText) findViewById(R.id.subject_edittext);
        descEditText = (EditText) findViewById(R.id.description_edittext);

        addWebcamBtn = (Button) findViewById(R.id.add_record);

        db = new DatabaseHelper(getApplicationContext());
        addWebcamBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_record:

                String name = subjectEditText.getText().toString();
                String url = descEditText.getText().toString();

                //ToDo: nenechat takto, i když pozicování ještě nebude hotové
                Random r = new Random();
                int pos = r.nextInt(10000);


                //db.insert(name, desc);
                db.createWebCam(new Webcam(name, url, pos, 0), new long[]{db.createCategory(new Category("Test"))});
                db.closeDB();

                Intent main = new Intent(AddWebcam.this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(main);
                break;
        }
    }

}