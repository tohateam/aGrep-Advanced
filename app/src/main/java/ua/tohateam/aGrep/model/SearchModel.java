package ua.tohateam.aGrep.model;

import java.io.*;

public class SearchModel
{
	private int line;
	private String group;
	private String text;
	private File path;

	public SearchModel() {
		this(0, null , null , null);
	}
	
	public SearchModel(int line, String group, String text, File path) {
		this.line = line;
		this.group = group;
		this.text = text;
		this.path = path;
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

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setPath(File path) {
		this.path = path;
	}

	public File getPath() {
		return path;
	}

}
