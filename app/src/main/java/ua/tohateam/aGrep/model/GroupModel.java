package ua.tohateam.aGrep.model;

import java.io.*;
import java.util.*;

public class GroupModel
{
	private String Name;
    private ArrayList<ChildModel> Items;
	private File path;
	private boolean selected;

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setPath(File path) {
		this.path = path;
	}

	public File getPath() {
		return path;
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
