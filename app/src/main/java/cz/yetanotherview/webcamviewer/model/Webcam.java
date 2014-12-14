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

package cz.yetanotherview.webcamviewer.model;

public class Webcam {

    long id;
    String name;
    String url;
    int position;
    int status;
    String created_at;

    // constructors
    public Webcam() {
    }

    public Webcam(String name, String url, int position, int status) {
        this.name = name;
        this.url = url;
        this.position = position;
        this.status = status;
    }

    public Webcam(long id, String name, String url, int position, int status) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.position = position;
        this.status = status;
    }

    // setters
    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCreatedAt(String created_at){
        this.created_at = created_at;
    }

    // getters
    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    public int getPosition() {
        return this.position;
    }

    public int getStatus() {
        return this.status;
    }
}
