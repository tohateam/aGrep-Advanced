package ua.tohateam.aGrep.model;

public class ChildModel
{
	private int line;
	private String text;
	private boolean selected;

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public int getLine() {
		return line;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
