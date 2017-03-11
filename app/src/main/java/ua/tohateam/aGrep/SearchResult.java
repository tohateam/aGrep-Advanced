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

public class SearchResult extends AppCompatActivity 
implements AsyncResponse, SearchAdapter.AdapterCallback
{
	private Toolbar toolbar;
	private ActionMode mActionMode;

	private MyUtils mUtils;
    private Prefs mPrefs;
	private ExpandableListView mResultList;
	private SearchAdapter mAdapter;
	private Pattern mPattern;
	private GrepTask mTask;
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

        mRecentAdapter = new ArrayAdapter<String>(mContext, 
												  android.R.layout.simple_dropdown_item_1line, 
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
                mPrefs.addRecent(this, mQuery);
                String patternText = mQuery;

                if (!mPrefs.mRegularExrpression) {
                    patternText = mUtils.escapeMetaChar(patternText);
                    patternText = mUtils.convertOrPattern(patternText);
                }

                if (mPrefs.mIgnoreCase) {
                    mPattern = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                } else {
                    mPattern = Pattern.compile(patternText);
                }

				if (mPrefs.mWordOnly) {
					mPattern = Pattern.compile("\\b" + patternText + "\\b");
				}

                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    mData.removeAll(mData);
					mTask = new GrepTask(0);
					mTask.delegate = this;
					mTask.execute(mQuery);

                }
            } else {
                finish();
            }
		}
	}

	private void showResult() {
        mGroupModel = setListGroups();
        mAdapter = new SearchAdapter(this, mGroupModel);
		mAdapter.setFormat(mPattern , mPrefs.mHighlightFg , mPrefs.mHighlightBg , mPrefs.mFontSize);
        mResultList.setAdapter(mAdapter);

        mResultList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
					//ArrayList<ChildModel> ch_list = ExpListItems.get(groupPosition).getItems();
					String patch = mGroupModel.get(groupPosition).getPath().getPath();
					Intent it = new Intent(SearchResult.this, TextViewerActivity.class);
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
						mActionMode = SearchResult.this.startActionMode(mActionModeCallback);
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
		if(mActionMode != null)
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
					showReplaceDialog(true);
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

	private void setTitleActionBar() {
		int count = 0;
		for (int i=0; i < mGroupModel.size(); i++) {
			if (mGroupModel.get(i).isSelected())
				count++;
		}
		mActionMode.setTitle(getString(R.string.title_actionbar, Integer.toString(count)));
	}


	// Заполняем список
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
						return Integer.toString(p1.getLine()).compareToIgnoreCase(Integer.toString(p2.getLine()));
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

	// Уведомляем о результах поиска/замены
	@Override
	public void onProcessFinish(boolean result, int id) {
		if (result) {
			if (id == 0 && mData.size() != 0) {
				// Сортируем по пути
				Collections.sort(mData, new Comparator<SearchModel>() {
						@Override
						public int compare(SearchModel p1, SearchModel p2) {
							return p1.getPath().getParent().compareToIgnoreCase(p2.getPath().getParent());
						}
					});
				showResult();
			} else {
				showResultDialog(getString(R.string.msg_replace_no, mQuery), false);
			}

			if (id == 1) { // замена
				String msg = result ? getString(R.string.msg_replace_ok, mQuery, mReplaceQuery) : getString(R.string.msg_replace_canceled, mQuery, mReplaceQuery);
				showResultDialog(msg, true);
			}
		}
	}

	/*
	 * Диалог завершения поиска/замены
	 * msg_result - текст сообщения
	 * type - true/false замена/поиск
	 */
	private void showResultDialog(String msg_result, boolean type) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppCompatAlertDialogStyle));

		Drawable micon = null;
		String mtitile = null;
		if (type) {
			micon = getDrawable(R.drawable.ic_alert_box);
			mtitile = getString(R.string.title_replace);
		} else {
			micon = getDrawable(R.drawable.ic_alert);
			mtitile = getString(R.string.title_search);
		}
		alertDialog.setIcon(micon);
		alertDialog.setTitle(mtitile);
		alertDialog.setMessage(msg_result);

		alertDialog.setPositiveButton(R.string.action_ok,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});

		alertDialog.show();
	}


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
				NavUtils.navigateUpFromSameTask(this);
				return true;
			case R.id.item_expand_all:
				expandAll();
				return true;
			case R.id.item_collapse_all:
				collapseAll();
				return true;
			case R.id.item_replace_selected:
				showReplaceDialog(true);
				break;
			case R.id.item_replace_all:
				showReplaceDialog(false);
				break;
//        if (item.getItemId() == R.id.action_preference) {
//            Intent intent = new Intent(this, SettingsActivity.class);
//            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mAdapter != null) {
			MenuItem item_replace = menu.findItem(R.id.item_replace_selected);
			if (mAdapter.countGroupSelected() != 0) {
				item_replace.setEnabled(true);
			} else {
				item_replace.setEnabled(false);
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	//method to expand all groups
	private void expandAll() {
		int count = mAdapter.getGroupCount();
		for (int i = 0; i < count; i++) {
			mResultList.expandGroup(i);
		}
	}

	//method to collapse all groups
	private void collapseAll() {
		int count = mAdapter.getGroupCount();
		for (int i = 0; i < count; i++) {
			mResultList.collapseGroup(i);
		}
	}

	private void showReplaceDialog(final boolean replaceGroup) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppCompatAlertDialogStyle));
		alertDialog.setIcon(R.drawable.ic_reply);
		alertDialog.setTitle(getString(R.string.title_replace));
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
					mPrefs.addRecent(SearchResult.this, edittext.getText().toString());
					ReplaceTask mReplece = new ReplaceTask(1, replaceGroup);
					mReplece.delegate = SearchResult.this;
					mReplaceQuery = edittext.getText().toString();
					mReplece.execute(mQuery, mReplaceQuery);
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

	@Override
	protected void onResume() {
		super.onResume();
		invalidateOptionsMenu();
		final List<String> recent = mPrefs.getRecent(this);
		mRecentAdapter.clear();
		mRecentAdapter.addAll(recent);
		mRecentAdapter.notifyDataSetChanged();
	}



	/**
	 * -----===== ПОИСК =====-----
	 */
    class GrepTask extends AsyncTask<String, SearchModel, Boolean>
    {
        private ProgressDialog mProgressDialog;
        private int mFileCount=0;
        private int mFoundcount=0;
        private boolean mCancelled;
		private int mId;

		public AsyncResponse delegate = null;

		public GrepTask(int id) {
			this.mId = id;
		}

        @Override
        protected void onPreExecute() {
            mCancelled = false;
            mProgressDialog = new ProgressDialog(new ContextThemeWrapper(SearchResult.this, R.style.AppCompatAlertDialogStyle));
            mProgressDialog.setTitle(R.string.title_grep_spinner);
			mProgressDialog.setIcon(R.drawable.ic_file_find);
            mProgressDialog.setMessage(mQuery);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						mCancelled = true;
						cancel(false);
					}
				});
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return grepRoot(params[0]);
        }


        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();
            mProgressDialog = null;

            Toast.makeText(getApplicationContext(), result ?R.string.msg_grep_finished: R.string.msg_grep_canceled, Toast.LENGTH_LONG).show();
            mTask = null;
			delegate.onProcessFinish(result, mId);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onPostExecute(false);
        }

        @Override
        protected void onProgressUpdate(SearchModel... progress) {
            if (isCancelled()) {
                return;
            }
            mProgressDialog.setMessage(SearchResult.this.getString(R.string.msg_progress , mQuery, mFileCount));
            if (progress != null) {
                synchronized (mData) {
                    for (SearchModel data : progress) {
                        mData.add(data);
                    }
                }
            }
        }

        boolean grepRoot(String text) {
            for (CheckedString dir : mPrefs.mDirList) {
                if (dir.checked && !grepDirectory(new File(dir.string))) {
                    return false;
                }
            }
            return true;
        }

        boolean grepDirectory(File dir) {
            if (isCancelled()) {
                return false;
            }
            if (dir == null) {
                return false;
            }

            File[] flist = dir.listFiles();

            if (flist != null) {
                for (File f : flist) {
					// Если скрытый и выкл. поиск в скрытых, то дальше
					if (!mPrefs.mSearchHidden && f.getName().startsWith(".")) {
						continue;
					}

                    boolean res = false;
                    if (f.isDirectory()) {
                        res = grepDirectory(f);
                    } else {
                        res = grepFile(f);
                    }
                    if (!res) {
                        return false;
                    }
                }
            }
            return true;
        }


        boolean grepFile(File file) {
            if (isCancelled()) {
                return false;
            }
            if (file == null) {
                return false;
            }

            boolean extok=false;
            for (CheckedString ext : mPrefs.mExtList) {
                if (ext.checked) {
					if (file.getName().indexOf('.') == -1) {
						extok = true;
						break;
					} else if (file.getName().toLowerCase().endsWith("." + ext.string.toLowerCase())) {
                        extok = true;
                        break;
                    } if (ext.string.equals("*")) {
						extok = true;
						break;
                    }
                } // end extChecked
            }

            if (!extok) {
                return true;
            }

            InputStream is;
            try {
                is = new BufferedInputStream(new FileInputStream(file) , 65536);
                is.mark(65536);

				String encode = mUtils.getDetectedEncoding(is);
                is.reset();

                BufferedReader br=null;
                try {
                    if (encode != null) {
                        br = new BufferedReader(new InputStreamReader(is , encode) , 8192);

                    } else {
                        br = new BufferedReader(new InputStreamReader(is) , 8192);
                    }

					String group = file.getName();
                    String text;
                    int line = 0;
                    boolean found = false;
                    Pattern pattern = mPattern;
                    Matcher m = null;
                    ArrayList<SearchModel> data = null ;
                    mFileCount++;
                    while ((text = br.readLine()) != null) {
                        line ++;
                        if (m == null) {
                            m = pattern.matcher(text);
                        } else {
                            m.reset(text);
                        }
                        if (m.find()) {
                            found = true;

                            synchronized (mData) {
                                mFoundcount++;
                                if (data == null) {
                                    data = new ArrayList<SearchModel>();
                                }
                                data.add(new SearchModel(line, group, text, file));

                                if (mFoundcount < 10) {
                                    publishProgress(data.toArray(new SearchModel[0]));
                                    data = null;
                                }
                            }
                            if (mCancelled) {
                                break;
                            }
                        }
                    }
                    br.close();
                    is.close();
                    if (data != null) {
                        publishProgress(data.toArray(new SearchModel[0]));
                        data = null;
                    }
                    if (!found) {
                        if (mFileCount % 10 == 0) {
                            publishProgress((SearchModel[])null);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return true;
        }
    }

	/**
	 * -----===== ЗАМЕНА =====-----
	 */
    class ReplaceTask extends AsyncTask<String, ReplaceTask.Progress, Boolean>
    {
        private ProgressDialog mProgressDialog;
        private boolean mCancelled;
		private StringBuffer buffer;

		public AsyncResponse delegate = null;
		private int mResultId;
		private boolean replaceGroup;

		class Progress
		{
			String label;
			int totalSize;
		}

		public ReplaceTask(int id, boolean group) {
			this.mResultId = id;
			this.replaceGroup = group;
		}

        @Override
        protected void onPreExecute() {
            mCancelled = false;
            mProgressDialog = new ProgressDialog(new ContextThemeWrapper(SearchResult.this, R.style.AppCompatAlertDialogStyle));
            mProgressDialog.setTitle(R.string.title_replace_spinner);
            mProgressDialog.setMessage(mQuery);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						mCancelled = true;
						cancel(false);
					}
				});
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
			boolean res = false;
			Progress progress = new Progress();
			progress.totalSize = mGroupModel.size();

			for (int i=0; i < mGroupModel.size(); i++) {
				progress.label = mGroupModel.get(i).getName();
				publishProgress(progress);

				if (replaceGroup && !mGroupModel.get(i).isSelected()) {
					continue;
				}

				buffer = mUtils.replaceFile(mGroupModel.get(i).getPath(), params[1], mPattern);
				if (buffer != null) {
					if (mPrefs.mCreateBackup) {
						// создать резервную копию
						try {
							mUtils.copyFile(mGroupModel.get(i).getPath(), new File(mGroupModel.get(i).getPath().toString() + "~"));
						} catch (IOException e) {}
					}

					mUtils.saveFile(mGroupModel.get(i).getPath(), buffer.toString());
					res = true;
				}
			}
            return res;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onPostExecute(false);
        }

        @Override
        protected void onProgressUpdate(Progress... values) {
            if (isCancelled()) {
                return;
            }
			Progress progress = values[0];

			mProgressDialog.setMessage(progress.label);
			if (mProgressDialog.getMax() == 100) {
				mProgressDialog.setMax(progress.totalSize);
			}

			mProgressDialog.incrementProgressBy(1);
        }

		@Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();
            mProgressDialog = null;

//            Toast.makeText(getApplicationContext(), 
//						   result ?R.string.msg_grep_finished: R.string.msg_grep_canceled, 
//						   Toast.LENGTH_LONG).show();
            mTask = null;
			delegate.onProcessFinish(result, mResultId);
        }

	}
}
