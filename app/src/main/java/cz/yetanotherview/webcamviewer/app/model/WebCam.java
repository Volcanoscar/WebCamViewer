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

package cz.yetanotherview.webcamviewer.app.model;

import java.util.List;

public class WebCam {

    private long id;
    private String webcamName;
    private String webcamUrl;
    private int position;
    private int status;
    private double latitude;
    private double longitude;
    private String created_at;

    private List tags;

    // constructors
    public WebCam() {
    }

    public WebCam(String webcamName, String webcamUrl, int position, int status) {
        this.webcamName = webcamName;
        this.webcamUrl = webcamUrl;
        this.position = position;
        this.status = status;
    }

    public WebCam(String webcamName, String webcamUrl, int position, int status, double latitude, double longitude) {
        this.webcamName = webcamName;
        this.webcamUrl = webcamUrl;
        this.position = position;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public WebCam(long id, String webcamName, String webcamUrl, int position, int status) {
        this.id = id;
        this.webcamName = webcamName;
        this.webcamUrl = webcamUrl;
        this.position = position;
        this.status = status;
    }

    // setters
    public void setId(long id) {
        this.id = id;
    }

    public void setName(String webcamName) {
        this.webcamName = webcamName;
    }

    public void setUrl(String webcamUrl) {
        this.webcamUrl = webcamUrl;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setCreatedAt(String created_at){
        this.created_at = created_at;
    }

    public void setTags(List tags) {
        this.tags = tags;
    }

    // getters
    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.webcamName;
    }

    public String getUrl() {
        return this.webcamUrl;
    }

    public int getPosition() {
        return this.position;
    }

    public int getStatus() {
        return this.status;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public List getTags() {
        return tags;
    }

}
