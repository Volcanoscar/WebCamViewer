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

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;

public class SizeTransform implements com.squareup.picasso.Transformation {

    private final int layoutId;

    public SizeTransform(final int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
    public Bitmap transform(final Bitmap source) {

        int targetWidth;
        int targetHeight;

        if (layoutId == 1) {
            targetWidth = source.getWidth();
            targetHeight = (int) (targetWidth * 0.67);
        }
        else {
            targetWidth = source.getWidth();
            targetHeight = source.getHeight();
        }

        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        Bitmap output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawRect(new Rect(0, 0, targetWidth, targetHeight), paint);
        if (source != output) {
            source.recycle();
        }
        return output;
    }

    @Override
    public String key() {
        return "";
    }
}
