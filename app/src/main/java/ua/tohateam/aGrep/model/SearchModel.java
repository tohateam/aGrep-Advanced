package ua.tohateam.aGrep.model;

import java.io.*;

public class SearchModel
{
	private int line;
	private String group;
	private String name;
	private File path;
	private boolean select;

	public SearchModel() {
		this(0, null , null , null);
	}
	
	public SearchModel(int line, String group, String text, File path) {
		this.line = line;
		this.group = group;
		this.name = text;
		this.path = path;
	}

	public SearchModel(int line, String group, String text, File path, boolean select) {
		this.line = line;
		this.group = group;
		this.name = text;
		this.path = path;
		this.select = select;
	}
	
	public void setSelect(boolean select) {
		this.select = select;
	}

	public boolean isSelect() {
		return select;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public int getLine() {
		return line;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	public void setName(String text) {
		this.name = text;
	}

	public String getName() {
		return name;
	}

	public void setPath(File path) {
		this.path = path;
	}

	public File getPath() {
		return path;
	}

}
