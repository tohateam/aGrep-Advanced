package ua.tohateam.aGrep.model;

import java.util.*;

public class GroupFilesModel
{
	private String name;
    private ArrayList<ChildFilesModel> items;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public void setItems(ArrayList<ChildFilesModel> items) {
		this.items = items;
	}

	public ArrayList<ChildFilesModel> getItems() {
		return items;
	}
}
