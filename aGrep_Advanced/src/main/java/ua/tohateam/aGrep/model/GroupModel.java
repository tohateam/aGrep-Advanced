package ua.tohateam.aGrep.model;

import java.io.File;
import java.util.ArrayList;

public class GroupModel {
    private String Name;
    private ArrayList<ChildModel> Items;
    private File path;
    private boolean selected;
    private String encoding;
    private String delimiter;

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public ArrayList<ChildModel> getItems() {
        return Items;
    }

    public void setItems(ArrayList<ChildModel> Items) {
        this.Items = Items;
    }
}
