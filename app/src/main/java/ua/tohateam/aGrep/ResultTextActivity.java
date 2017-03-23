package ua.tohateam.aGrep;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import ua.tohateam.aGrep.model.*;
import ua.tohateam.aGrep.utils.*;

import android.app.AlertDialog;
import android.support.v7.widget.Toolbar;

public class ResultTextActivity extends AppCompatActivity 
implements SearchAdapter.AdapterCallback,
SearchText.SearchTextCallback
{	
	private static final int RESULT_SEARCH = 0;
	private static final int RESULT_REPLACE = 1;
	private static final int RESULT_CANCELED = 2;

	private Toolbar toolbar;
	private ActionMode mActionMode;

	private MyUtils mUtils;
    private Prefs mPrefs;
	private ExpandableListView mResultList;
	private SearchAdapter mAdapter;
	private Context mContext;

	private String mQuery;
	private String mReplaceQuery;

	private ArrayList<GroupModel> mGroupModel;
	private ArrayAdapter<String> mRecentAdapter;
	private int mCurentGroup;
	private int mCurentChild;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

		mContext = this;
		mUtils = new MyUtils();
		mPrefs = Prefs.loadPrefes(this);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle(getString(R.string.app_search_result));
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setHomeButtonEnabled(true);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}

        mRecentAdapter = new ArrayAdapter <String>(mContext, 
												   R.layout.my_spinner_item, 
												   new ArrayList <String>());

		mResultList = (ExpandableListView) findViewById(R.id.search_result_list);

        Intent it = getIntent();
		if (it != null && Intent.ACTION_SEARCH.equals(it.getAction())) {
            Bundle extras = it.getExtras();
            mQuery = extras.getString(SearchManager.QUERY);
			getSupportActionBar().setTitle(getString(R.string.app_search_result) + " : " + mQuery);

            if (mQuery != null && mQuery.length() > 0) {
				SearchText searchText = new SearchText(this);
				searchText.startSearchText(mQuery);
            } else {
                finish();
            }
		}
	}

	@Override
	public void onMethodCallback() {
		invalidateOptionsMenu();
		int selected = mAdapter.countGroupSelected();
		if(selected>0) {
			toolbar.setSubtitleTextColor(getResources().getColor(R.color.colorAccent));
			getSupportActionBar().setSubtitle(getString(R.string.title_actionbar, selected));
		} else {
			getSupportActionBar().setSubtitle("");
		}
		if (mActionMode != null)
			setTitleActionBar();
	}
	
	// Получаем результаты поиска
	@Override
	public void onMethodCallback(ArrayList<SearchModel> data, int idResult, boolean result) {
		if (idResult == RESULT_CANCELED) {
			dialogNoSearch(mContext.getString(R.string.msg_search_canceled));
		} else if (idResult == RESULT_SEARCH && data.size() > 0) {
			showResult(data);
		} else if (idResult == RESULT_SEARCH && data.size() == 0) {
			dialogNoSearch(mContext.getString(R.string.msg_search_no, mQuery));
		} else if (idResult == RESULT_REPLACE) {
			String msg = result ? mContext.getString(R.string.msg_replace_ok, mQuery, mReplaceQuery)
				: mContext.getString(R.string.msg_search_canceled, mQuery, mReplaceQuery);
			dialogReplaceResult(msg);
		}
	}

	// Поиск - не найдено
	private void dialogNoSearch(String msg) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(R.drawable.ic_message_alert);
		alertDialog.setTitle(R.string.app_search_result);
		alertDialog.setMessage(msg);

		alertDialog.setPositiveButton(R.string.action_ok,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});

		alertDialog.show();
	}

	/*
	 * Результаты замены
	 * msg_result - текст сообщения
	 */
	private void dialogReplaceResult(String msg_result) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(R.drawable.ic_message_draw);
		alertDialog.setTitle(R.string.title_replace);
		alertDialog.setMessage(msg_result);

		alertDialog.setPositiveButton(R.string.action_ok,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					//startTaskSearch();
					finish();
				}
			});

		alertDialog.show();
	}

	private void showResult(ArrayList<SearchModel> data) {
        mGroupModel = setListGroups(data);
        mAdapter = new SearchAdapter(this, mGroupModel);
		mAdapter.setFormat(mUtils.getPattern(mContext, mQuery) , mPrefs.mHighlightFg , mPrefs.mHighlightBg , mPrefs.mFontSize);
        mResultList.setAdapter(mAdapter);

        mResultList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
					String patch = mGroupModel.get(groupPosition).getPath().getPath();
					Intent it = new Intent(ResultTextActivity.this, TextViewerActivity.class);
					it.setAction(Intent.ACTION_SEARCH);
					it.putExtra(SearchManager.QUERY, mQuery);
					it.putExtra("path", patch);
					startActivity(it);
					if (mActionMode != null) {
						mActionMode = null;
						for (int i=0; i < mGroupModel.size(); i++) {
							mGroupModel.get(i).setSelected(false);
						}
						mGroupModel.get(mCurentGroup).getItems().get(mCurentChild).setSelected(false);
						mAdapter.notifyDataSetChanged();
					}
					return false;
				}
			});

        mResultList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
						mCurentGroup = ExpandableListView.getPackedPositionGroup(id);
						mCurentChild = ExpandableListView.getPackedPositionChild(id);
						// выделяем текущую группу
						mGroupModel.get(mCurentGroup).setSelected(true);
						// сброс всех выделений
						for (int i= 0; i < mGroupModel.size(); i++) {
							for (int j=0; j < mGroupModel.get(i).getItems().size(); j++) {
								mGroupModel.get(i).getItems().get(j).setSelected(false);
							}
						}
						// выделяем текущий элемент
						mGroupModel.get(mCurentGroup).getItems().get(mCurentChild).setSelected(true);
						mAdapter.notifyDataSetChanged();
						// активируем CAB
						if (mActionMode == null) {
							mActionMode = ResultTextActivity.this.startActionMode(mActionModeCallback);
						}
						setTitleActionBar();
						return true;
					}
					return false;
				}
			});
	}


	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.search_context_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if nothing is done
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
				case R.id.item_select_all:
					for (int i=0; i < mGroupModel.size(); i++) {
						mGroupModel.get(i).setSelected(true);
					}
					mAdapter.notifyDataSetChanged();
					setTitleActionBar();
					return true;
				case R.id.item_unselect_all:
					for (int i=0; i < mGroupModel.size(); i++) {
						mGroupModel.get(i).setSelected(false);
					}
					mAdapter.notifyDataSetChanged();
					setTitleActionBar();
					return true;
				case R.id.item_select_invert:
					for (int i=0; i < mGroupModel.size(); i++) {
						mGroupModel.get(i).setSelected(!mGroupModel.get(i).isSelected());
					}
					mAdapter.notifyDataSetChanged();
					setTitleActionBar();
					return true;
				case R.id.item_replace_group:
					showReplaceDialog();
					mode.finish();
					return true;
				case R.id.item_send:
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);

					File path = mGroupModel.get(mCurentGroup).getPath();
					int position = mGroupModel.get(mCurentGroup).getItems().get(mCurentChild).getLine();

					if (mPrefs.addLineNumber) {
						intent.setDataAndType(Uri.parse("file://" + path + "?line=" + (position)), "text/plain");
					} else {
						intent.setDataAndType(Uri.parse("file://" + path), "text/plain");
					}
					mGroupModel.get(mCurentGroup).getItems().get(mCurentChild).setSelected(false);
					startActivity(intent);
					mode.finish();
					return true;
				default:
					return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			mGroupModel.get(mCurentGroup).getItems().get(mCurentChild).setSelected(false);
			// сброс всех выделений 
			for (int i= 0; i < mGroupModel.size(); i++) {
				for (int j=0; j < mGroupModel.get(i).getItems().size(); j++) {
					mGroupModel.get(i).getItems().get(j).setSelected(false);
				}
			}
			mAdapter.notifyDataSetChanged();
		}
	};

	// Устанавливаем заголовок ActionBar - кол-во выделенных элементов
	private void setTitleActionBar() {
		mActionMode.setTitle(getString(R.string.title_actionbar, Integer.toString(mAdapter.countGroupSelected())));
	}


	// Заполняем список результатов поиска
    public ArrayList<GroupModel> setListGroups(ArrayList<SearchModel> mData) {
        ArrayList<GroupModel> group_list = new ArrayList<GroupModel>();
        ArrayList<ChildModel> child_list = null;
		String oldGroup = null;

        if (mData != null) {
			// Сортируем по по группе
			Collections.sort(mData, new Comparator<SearchModel>() {
					@Override
					public int compare(SearchModel p1, SearchModel p2) {
						return p1.getGroup().compareToIgnoreCase(p2.getGroup());
					}
				});

			// Сортируем по пути
			Collections.sort(mData, new Comparator<SearchModel>() {
					@Override
					public int compare(SearchModel p1, SearchModel p2) {

						return p1.getPath().getParent().compareToIgnoreCase(p2.getPath().getParent());
					}
				});


			for (int i=0; i < mData.size();i++) {
				GroupModel group = new GroupModel();
				String mGroup = mData.get(i).getGroup();
				if (!mGroup.equals(oldGroup)) {
					group.setName(mGroup);
					group.setPath(mData.get(i).getPath());
					group.setSelected(false);
					group_list.add(group);
					oldGroup = mGroup;
				}
			}

			// Сортируем по номеру строки
			Collections.sort(mData, new Comparator<SearchModel>() {
					@Override
					public int compare(SearchModel p1, SearchModel p2) {
						Integer line1 = p1.getLine();
						Integer line2 = p2.getLine();
						return line1.compareTo(line2);
					}
				});

			for (int i=0; i < group_list.size();i++) {
				String mGroup = group_list.get(i).getName();
				child_list = new ArrayList<ChildModel>();
				for (int j=0; j < mData.size(); j++) {
					ChildModel child = new ChildModel();
					String group = mData.get(j).getGroup();
					if (group.equals(mGroup)) {
						child.setLine(mData.get(j).getLine());
						child.setText(mData.get(j).getName());
						child.setSelected(false);
						child_list.add(child);
					}
				}
				group_list.get(i).setItems(child_list);
			}
		}
		// Сортируем группу по имени
		Collections.sort(group_list, new Comparator<GroupModel>() {
				@Override
				public int compare(GroupModel p1, GroupModel p2) {
					return p1.getName().compareToIgnoreCase(p2.getName());
				}
			});

		return group_list;
    }

	// Основное меню
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				super. onBackPressed();
				return true;
			case R.id.item_expand_all:
				expandAll();
				return true;
			case R.id.item_collapse_all:
				collapseAll();
				return true;
			case R.id.item_replace:
				showReplaceDialog();
				break;
        }
        return super.onOptionsItemSelected(item);
    }
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	// Развернуть всё
	private void expandAll() {
		int count = mAdapter.getGroupCount();
		for (int i = 0; i < count; i++) {
			mResultList.expandGroup(i);
		}
	}

	// свернуть всё
	private void collapseAll() {
		int count = mAdapter.getGroupCount();
		for (int i = 0; i < count; i++) {
			mResultList.collapseGroup(i);
		}
	}

	// Заменить текст
	private void showReplaceDialog() {
		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.dialog_layout_replace, null);

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(R.drawable.ic_rename_box);
		alertDialog.setTitle(getString(R.string.title_replace_options));
		alertDialog.setMessage(getString(R.string.msg_replace, mQuery));
		alertDialog.setView(dialoglayout);

		final AutoCompleteTextView edittext = (AutoCompleteTextView) dialoglayout.findViewById(R.id.replace_query_input);
        edittext.setAdapter(mRecentAdapter);

        ImageButton clrBtn = (ImageButton) dialoglayout.findViewById(R.id.btn_clear_replace);
        clrBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					edittext.setText("");
					edittext.requestFocus();
				}
			});
        ImageButton historyBtn = (ImageButton) dialoglayout.findViewById(R.id.btn_history_replace);
        historyBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					edittext.showDropDown();
				}
			});

		alertDialog.setPositiveButton(R.string.action_ok,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String replace = edittext.getText().toString();
					if (replace.equals("")) {
						edittext.setError(getString(R.string.msg_is_empty));
						showReplaceDialog();
					} else {
						edittext.setError("");
						mReplaceQuery = edittext.getText().toString();
						confirmReplace();
					}
				}
			});

		alertDialog.setNegativeButton(R.string.action_cancel,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mGroupModel.get(mCurentGroup).setSelected(false);
					dialog.cancel();
				}
			});

		alertDialog.show();
	}

	// проверка введенных данных и замена
	private void confirmReplace() {
		String query = mReplaceQuery.trim();
		if (query == null || query.equals("")) {
			Toast.makeText(getApplicationContext(), R.string.msg_no_replace_query, Toast.LENGTH_LONG).show();
			showReplaceDialog();
		} else {
			// Добавляем в историю
			mPrefs.addRecent(ResultTextActivity.this, mReplaceQuery);

			boolean replace = false;
			if (mAdapter.countGroupSelected() == 1) {
				replace = false;
			} else if (mAdapter.countGroupSelected() == mAdapter.getGroupCount()) {
				replace = false;
			} else {
				replace = true;
			}

			SearchText searchText = new SearchText(ResultTextActivity.this);
			searchText.startReplaceText(mQuery, mReplaceQuery, replace, mGroupModel);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		invalidateOptionsMenu();
		final List<String> recent = mPrefs.getRecent(this);
		mRecentAdapter.clear();
		mRecentAdapter.addAll(recent);
		mRecentAdapter.notifyDataSetChanged();
	}

}
