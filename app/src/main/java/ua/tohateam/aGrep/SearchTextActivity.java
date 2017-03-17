package ua.tohateam.aGrep;

import android.app.*;
import android.content.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.ExpandableListView.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import ua.tohateam.aGrep.model.*;
import ua.tohateam.aGrep.utils.*;

import android.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View.OnClickListener;

public class SearchTextActivity extends AppCompatActivity 
implements 
//AsyncResponse, 
SearchAdapter.AdapterCallback,
SearchText.SearchTextCallback
{	
	private static final int RESULT_SEARCH = 0;
	private static final int RESULT_REPLACE = 1;

	private Toolbar toolbar;
	private ActionMode mActionMode;

	private MyUtils mUtils;
    private Prefs mPrefs;
	private ExpandableListView mResultList;
	private SearchAdapter mAdapter;
	private Pattern mPattern;
//	private GrepTask mTask;
//	private ReplaceTask mReplaceTask;
	private Context mContext;

	private String mQuery;
	private String mReplaceQuery;

	private ArrayList<SearchModel> mData;
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

		// Если не указаны директории поиска то выходим
		if (mPrefs.mDirList.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.msg_no_target_dir, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

		mResultList = (ExpandableListView) findViewById(R.id.search_result_list);
		mData = new ArrayList<SearchModel>();

        Intent it = getIntent();
		if (it != null && Intent.ACTION_SEARCH.equals(it.getAction())) {
            Bundle extras = it.getExtras();
            mQuery = extras.getString(SearchManager.QUERY);
			getSupportActionBar().setTitle(getString(R.string.app_search_result) + " : " + mQuery);

            if (mQuery != null && mQuery.length() > 0) {
//                mPrefs.addRecent(this, mQuery);
                //String patternText = mQuery;
				SearchText searchText = new SearchText(this);
				searchText.startSearchText(mQuery);
            } else {
                finish();
            }
		}
	}

	// Получаем результаты поиска
	@Override
	public void onMethodCallback(ArrayList<SearchModel> data, int idResult, boolean result) {
		// TODO: Implement this method
		if (idResult == 0 && data.size() > 0) {
			mData = data;
			// Сортируем по пути
			Collections.sort(mData, new Comparator<SearchModel>() {
					@Override
					public int compare(SearchModel p1, SearchModel p2) {
						return p1.getPath().getParent().compareToIgnoreCase(p2.getPath().getParent());
					}
				});
			
			showResult();
		} else if (idResult == 0 && data.size() == 0) {
			dialogNoSearch();
		} else if (idResult == 1) {
			String msg = result ? mContext.getString(R.string.msg_replace_ok, mQuery, mReplaceQuery)
				: mContext.getString(R.string.msg_replace_canceled, mQuery, mReplaceQuery);
			dialogReplaceResult(msg);
		}
	}

	// Поиск - не найдено
	private void dialogNoSearch() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(R.drawable.ic_message_alert);
		alertDialog.setTitle(R.string.title_search);
		alertDialog.setMessage(getString(R.string.msg_replace_no, mQuery));

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

//		alertDialog.setNegativeButton(R.string.action_search_close,
//			new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int which) {
//					mGroupModel.get(mCurentGroup).setSelected(false);
//					finish();
//				}
//			});

		alertDialog.show();
	}
	private void showResult() {
        mGroupModel = setListGroups();
        mAdapter = new SearchAdapter(this, mGroupModel);
		mAdapter.setFormat(mPattern , mPrefs.mHighlightFg , mPrefs.mHighlightBg , mPrefs.mFontSize);
        mResultList.setAdapter(mAdapter);

        mResultList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
					String patch = mGroupModel.get(groupPosition).getPath().getPath();
					Intent it = new Intent(SearchTextActivity.this, TextViewerActivity.class);
					it.setAction(Intent.ACTION_SEARCH);
					it.putExtra(SearchManager.QUERY, mQuery);
					it.putExtra("path", patch);
					startActivity(it);
					return false;
				}
			});

        mResultList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
						mCurentGroup = ExpandableListView.getPackedPositionGroup(id);
						mCurentChild = ExpandableListView.getPackedPositionChild(id);
						// выделяем текущий элемент списка
						mGroupModel.get(mCurentGroup).setSelected(true);
						mAdapter.notifyDataSetChanged();
						// активируем CAB
						mActionMode = SearchTextActivity.this.startActionMode(mActionModeCallback);
						setTitleActionBar();
						return true;
					}
					return false;
				}
			});
	}

	@Override
	public void onMethodCallback() {
		invalidateOptionsMenu();
		if (mActionMode != null)
			setTitleActionBar();
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
					showReplaceDialog(); //mGroupModel.get(mCurentGroup).getPath().toString());
					mode.finish();
					return true;
				case R.id.item_send:
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse("file://" + mGroupModel.get(mCurentGroup).getPath()), "text/plain");
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
		}
	};

	// Устанавливаем заголовок ActionBar - кол-во выделенных элементов
	private void setTitleActionBar() {
		int count = 0;
		for (int i=0; i < mGroupModel.size(); i++) {
			if (mGroupModel.get(i).isSelected())
				count++;
		}
		mActionMode.setTitle(getString(R.string.title_actionbar, Integer.toString(count)));
	}


	// Заполняем список результатов поиска
    public ArrayList<GroupModel> setListGroups() {
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
						child.setText(mData.get(j).getText());
						child.setSelected(false);
						child_list.add(child);
					}
				}
				group_list.get(i).setItems(child_list);
			}
		}
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
				//NavUtils.navigateUpFromSameTask(this);
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
		if (mAdapter != null) {
//			MenuItem item_replace = menu.findItem(R.id.item_replace_selected);
//			if (mAdapter.countGroupSelected() != 0) {
//				item_replace.setEnabled(true);
//			} else {
//				item_replace.setEnabled(false);
//			}
		}
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

	// Опции замены
	// TODO (tohateam): добавить проверку введённого текста
	private void showReplaceDialog() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(R.drawable.ic_reply);
		alertDialog.setTitle(getString(R.string.title_replace_options));
		alertDialog.setMessage(getString(R.string.msg_replace, mQuery));

		LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.replace_dialog, null);
		alertDialog.setView(view);
		final AutoCompleteTextView edittext = (AutoCompleteTextView) view.findViewById(R.id.replace_text);
        edittext.setAdapter(mRecentAdapter);

        ImageButton clrBtn = (ImageButton) view.findViewById(R.id.btn_clear_replace);
        clrBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					edittext.setText("");
					edittext.requestFocus();
				}
			});
        ImageButton historyBtn = (ImageButton) view.findViewById(R.id.btn_history_replace);
        historyBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					edittext.showDropDown();
				}
			});

		alertDialog.setPositiveButton(R.string.action_ok,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Добавляем в историю
					mPrefs.addRecent(SearchTextActivity.this, edittext.getText().toString());
					mReplaceQuery = edittext.getText().toString();
					confirmReplace();
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

	// подтверждение замены
	private void confirmReplace() {
		if (mReplaceQuery == null || mReplaceQuery.equals("")) {
			Toast.makeText(getApplicationContext(), R.string.msg_no_replace_query, Toast.LENGTH_LONG).show();
			showReplaceDialog();
		} else {

			String fileName = null;
			String title = null;
			boolean replace = false;
			if (mAdapter.countGroupSelected() == 1) {
				fileName = mGroupModel.get(mCurentGroup).getName();
				title = getString(R.string.msg_replace_confirm, fileName, mQuery, mReplaceQuery);
				replace = false;
			} else if (mAdapter.countGroupSelected() == mAdapter.getGroupCount()) {
				title = getString(R.string.msg_replace_selected_confirm, mQuery, mReplaceQuery);
				replace = false;
			} else {
				title = getString(R.string.msg_replace_all_confirm, mQuery, mReplaceQuery);
				replace = true;
			}
			final boolean replaceAll = replace;

			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
			alertDialog.setIcon(R.drawable.ic_message_alert);
			alertDialog.setTitle(R.string.title_replace);
			alertDialog.setMessage(title);

			alertDialog.setPositiveButton(R.string.action_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						SearchText searchText = new SearchText(SearchTextActivity.this);
						searchText.startReplaceText(mQuery, mReplaceQuery, replaceAll, mGroupModel);
					}
				});

			alertDialog.setNegativeButton(R.string.action_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
//					mGroupModel.get(mCurentGroup).setSelected(false);
//					finish();
					}
				});

			alertDialog.show();
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
