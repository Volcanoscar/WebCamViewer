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

package cz.yetanotherview.webcamviewer.app.model;

public class Category {

    private long id;
    private String category_name;
    private int count;

    // constructors
    public Category() {

    }

    public Category(String category_name) {
        this.category_name = category_name;
    }

    public Category(long id, String category_name) {
        this.id = id;
        this.category_name = category_name;
    }

    // setter
    public void setId(long id) {
        this.id = id;
    }

    public void setcategoryName(String category_name) {
        this.category_name = category_name;
    }

    public void setCount(int count) {
        this.count = count;
    }

    // getter
    public long getId() {
        return this.id;
    }

    public String getcategoryName() {
        return this.category_name;
    }

    public int getCount() {
        return this.count;
    }

    public String getCountAsString() {
        return String.valueOf(this.count);
    }
}
