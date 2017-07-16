package ua.tohateam.aGrep.model;

import android.graphics.drawable.Drawable;

import java.io.File;

public class SearchModel {
    private int line;
    private String group;
    private String name;
    private File path;
    private boolean select;
    private Drawable icon;
    private String encoding;
    private String delimiter;

    public SearchModel() {
        this(0, null, null, null, null, null);
    }

    public SearchModel(int line, String group, String text, File path, String encoding, String delimiter) {
        this.line = line;
        this.group = group;
        this.name = text;
        this.path = path;
        this.delimiter = delimiter;
        this.encoding = encoding;
    }

    public SearchModel(Drawable icon, String group, String text, File path, boolean select) {
        this.icon = icon;
        this.group = group;
        this.name = text;
        this.path = path;
        this.select = select;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isSelect() {
        return select;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String text) {
        this.name = text;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

}
