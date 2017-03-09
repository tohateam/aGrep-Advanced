package ua.tohateam.aGrep;

import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.regex.*;
import ua.tohateam.aGrep.model.*;
import ua.tohateam.aGrep.utils.*;

public class SearchAdapter extends BaseExpandableListAdapter
{
	private Context context;
    private ArrayList<GroupModel> groups;
	private MyUtils mUtils;

	private Pattern mPattern;
	private int mFgColor;
	private int mBgColor;
	private int mFontSize;


	public SearchAdapter(Context context, ArrayList<GroupModel> groups) {
        this.context = context;
        this.groups = groups;
		this.mUtils = new MyUtils();
	}

	class GroupHolder
	{
		TextView groupName;
  		TextView groupPath;
		CheckBox groupButton;
	}

	class ChildHolder
	{		
		TextView searchLine;
		TextView searchText;
		boolean fileSelect;
    }

	@Override
    public Object getChild(int groupPosition, int childPosition) {
        ArrayList<ChildModel> chList = groups.get(groupPosition).getItems();
        return chList.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

	@Override
    public int getChildrenCount(int groupPosition) {
        ArrayList<ChildModel> chList = groups.get(groupPosition).getItems();
        return chList.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groups.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

	@Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

	// View group
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		final GroupHolder groupHolder;
		final GroupModel group = (GroupModel) getGroup(groupPosition);

        if (convertView == null) {
			LayoutInflater inf = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            convertView = inf.inflate(R.layout.group_item_row, null);
			groupHolder = new GroupHolder();
			groupHolder.groupName = (TextView) convertView.findViewById(R.id.group_name);
			groupHolder.groupPath = (TextView) convertView.findViewById(R.id.group_path);
			groupHolder.groupButton = (CheckBox) convertView.findViewById(R.id.cb_group_select);
			groupHolder.groupName.setTextSize(mFontSize);
			groupHolder.groupPath.setTextSize(mFontSize);
			groupHolder.groupButton.setTextSize(mFontSize);

			convertView.setTag(groupHolder);
		} else {
            groupHolder = (GroupHolder) convertView.getTag();
        }

		groupHolder.groupName.setText(group.getName().toUpperCase());
		groupHolder.groupPath.setText(group.getPath().getAbsolutePath());
		final String count = Integer.toString(getChildrenCount(groupPosition));
		groupHolder.groupButton.setText(count);
		
		groupHolder.groupButton.setOnCheckedChangeListener(
			new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton p1, boolean p2) {
					if (groupHolder.groupButton.isChecked()) {
						group.setSelected(true);
					} else {
						group.setSelected(false);
					}
					notifyDataSetChanged();
				}
			});
		groupHolder.groupButton.setChecked(group.isSelected());
		

        return convertView;
	}

	@Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		final ChildHolder childHolder;
        final ChildModel child = (ChildModel) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.child_item_row, null);
			childHolder = new ChildHolder();

			childHolder.searchLine = (TextView) convertView.findViewById(R.id.child_line);
			childHolder.searchText = (TextView) convertView.findViewById(R.id.child_text);
			childHolder.searchLine.setTextSize(mFontSize);
			childHolder.searchText.setTextSize(mFontSize);
            convertView.setTag(childHolder);
        } else {
            childHolder = (ChildHolder) convertView.getTag();
        }

        childHolder.searchLine.setText(Integer.toString(child.getLine()));
		childHolder.searchText.setText(mUtils.highlightKeyword(child.getText(), mPattern, mFgColor , mBgColor));
        return convertView;
	}

	@Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

	private int countSelected(int groupPosition) {
		int count = 0;
		ArrayList<ChildModel> chList = groups.get(groupPosition).getItems();
		for (int i=0; i < chList.size(); i++) {
			if (chList.get(i).isSelected()) {
				count++;
			}
		}
		return count;
	}

	public void setFormat(Pattern pattern, int fgcolor, int bgcolor, int size) {
		mPattern = pattern;
		mFgColor = fgcolor;
		mBgColor = bgcolor;
		mFontSize = size;
	}

}
