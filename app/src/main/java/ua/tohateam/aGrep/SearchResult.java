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
	private static final int RESULT_SEARCH = 0;
	private static final int RESULT_REPLACE = 1;

	private Toolbar toolbar;
	private ActionMode mActionMode;

	private MyUtils mUtils;
    private Prefs mPrefs;
	private ExpandableListView mResultList;
	private SearchAdapter mAdapter;
	private Pattern mPattern;
	private GrepTask mTask;
	private ReplaceTask mReplaceTask;
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

				startTaskSearch();
            } else {
                finish();
            }
		}
	}

	private void startTaskSearch() {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			mData.removeAll(mData);
			mTask = new GrepTask(RESULT_SEARCH);
			mTask.delegate = this;
			mTask.execute(mQuery);
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
						// выделяем текущий элемент списка
						mGroupModel.get(mCurentGroup).setSelected(true);
						mAdapter.notifyDataSetChanged();
						// активируем CAB
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

	// Уведомляем о результах поиска/замены
	@Override
	public void onProcessFinish(boolean result, int id) {
		if (result) {
			if (id == RESULT_SEARCH && mData.size() != 0) {
				// Сортируем по пути
				Collections.sort(mData, new Comparator<SearchModel>() {
						@Override
						public int compare(SearchModel p1, SearchModel p2) {
							return p1.getPath().getParent().compareToIgnoreCase(p2.getPath().getParent());
						}
					});
				showResult();
			} else {
				showResultSearch();
			}

			if (id == RESULT_REPLACE) { // замена
				String msg = result ? getString(R.string.msg_replace_ok, mQuery, mReplaceQuery) : getString(R.string.msg_replace_canceled, mQuery, mReplaceQuery);
				mReplaceQuery = null;
				showResultDialog(msg);
			}
		}
	}

	/*
	 * Результаты замены
	 * msg_result - текст сообщения
	 */
	private void showResultDialog(String msg_result) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(R.drawable.ic_message_draw);
		alertDialog.setTitle(R.string.title_replace);
		alertDialog.setMessage(msg_result);

		alertDialog.setPositiveButton(R.string.action_rescan,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					startTaskSearch();
					//finish();
				}
			});

		alertDialog.setNegativeButton(R.string.action_search_close,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mGroupModel.get(mCurentGroup).setSelected(false);
					finish();
				}
			});

		alertDialog.show();
	}

	// Поиск - не найдено
	private void showResultSearch() {
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
					mPrefs.addRecent(SearchResult.this, edittext.getText().toString());
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
						mReplaceTask = new ReplaceTask(RESULT_REPLACE, replaceAll);
						mReplaceTask.delegate = SearchResult.this;
						mReplaceTask.execute(mQuery, mReplaceQuery);
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
		private boolean replaceAll;

		class Progress
		{
			String label;
			int totalSize;
		}

		public ReplaceTask(int id, boolean replace) {
			this.mResultId = id;
			this.replaceAll = replace;
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

				if (!mGroupModel.get(i).isSelected() && !replaceAll) {
					continue;
				}
				saveFile(mGroupModel.get(i).getPath(), params[1]);
				mGroupModel.get(mCurentGroup).setSelected(false);
				res = true;
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
            mReplaceTask = null;
			delegate.onProcessFinish(result, mResultId);
        }

		private void saveFile(File path, String replace) {
			buffer = mUtils.replaceFile(path, replace, mPattern);
			if (buffer != null) {
				if (mPrefs.mCreateBackup) {
					// создать резервную копию
					try {
						mUtils.copyFile(path, new File(path.toString() + "~"));
					} catch (IOException e) {}
				}

				mUtils.saveFile(path, buffer.toString());
			}
		}
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
					if (file.getName().indexOf('.') == -1 && ext.string.equals("*.")) {
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

}
