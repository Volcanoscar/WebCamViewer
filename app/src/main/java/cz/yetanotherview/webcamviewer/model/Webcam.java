package cz.yetanotherview.webcamviewer.model;

public class Webcam {

    int id;
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

    public Webcam(int id, String name, String url, int position, int status) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.position = position;
        this.status = status;
    }

    // setters
    public void setId(int id) {
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
