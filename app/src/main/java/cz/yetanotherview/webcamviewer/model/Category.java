package cz.yetanotherview.webcamviewer.model;

public class Category {

    int id;
    String category_name;

    // constructors
    public Category() {

    }

    public Category(String category_name) {
        this.category_name = category_name;
    }

    public Category(int id, String category_name) {
        this.id = id;
        this.category_name = category_name;
    }

    // setter
    public void setId(int id) {
        this.id = id;
    }

    public void setcategoryName(String category_name) {
        this.category_name = category_name;
    }

    // getter
    public int getId() {
        return this.id;
    }

    public String getcategoryName() {
        return this.category_name;
    }
}
