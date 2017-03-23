package ua.tohateam.aGrep;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.text.*;
import android.view.*;
import android.view.View.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import ua.tohateam.aGrep.utils.*;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.support.v7.widget.Toolbar;

public class TextViewerActivity extends AppCompatActivity 
implements AsyncResponse
{	
    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_PATH = "path";
	
	private Toolbar toolbar;
	private ActionMode mActionMode;

	private MyUtils mUtils;
    private Prefs mPrefs;
	private Context mContext;
	private TextLoadTask mTask;
	private ReplaceTask mReplece;

	private ArrayAdapter<String> mRecentAdapter;
	private TextView mTextPreview;
	private EditText mEditText;
	private StringBuilder mLoadText = null;

	private String mPath;
	private String mQuery;
	private String mReplaceQuery;
	private String mTitle;

	private int mFontSize;
	private boolean mEditMode = false;
	private boolean mTextChanged = false;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.textview_activity);

		mContext = this;
		mUtils = new MyUtils();
		mPrefs = Prefs.loadPrefes(this);
		mFontSize = mPrefs.mFontSize;

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				//getSupportActionBar().setTitle(mTitle);
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setHomeButtonEnabled(true);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}

		mTextPreview = (TextView) findViewById(R.id.text_view_body);
		mTextPreview.setTextSize(mFontSize);
		mTextPreview.setOnLongClickListener(new View.OnLongClickListener() {
				// Called when the user long-clicks on someView
				public boolean onLongClick(View view) {
					if (mActionMode != null) {
						return false;
					}
					mActionMode = startActionMode(mActionModeCallback);
					view.setSelected(true);
					return true;
				}
			});
        mRecentAdapter = new ArrayAdapter <String>(mContext, 
												   R.layout.my_spinner_item, 
												   new ArrayList <String>());
		
		mEditText = (EditText) findViewById(R.id.text_view_edit);
		mEditText.setTextSize(mFontSize);
		mEditText.addTextChangedListener(textWatcher);

		Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                mPath = extra.getString(EXTRA_PATH);
                mQuery = extra.getString(EXTRA_QUERY);
				setViewMode();
				startLoadFile(mQuery);
            }
		}
	}

	// Определение изменения файла
	private TextWatcher textWatcher = new TextWatcher() {
		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			if (after == 1 || count == 1) {
				mTextChanged = true;
				getSupportActionBar().setTitle(mTitle + " *");
			}
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {

		}
	};

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.textview_action_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if nothing is done
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
				case R.id.item_view_copy:
					ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("aGrep Text Viewer", mTextPreview.getText());
					cm.setPrimaryClip(clip);
					Toast.makeText(TextViewerActivity.this, R.string.msg_copied, Toast.LENGTH_LONG).show();
					mode.finish();
					return true;
				case R.id.item_view_replace:
					showReplaceDialog();
					mode.finish();
					return true;
				case R.id.item_view_send:
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse("file://" + mPath), "text/plain");
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

	private void startLoadFile(String query) {
		if (!mPrefs.mRegularExrpression) {
			mQuery = mUtils.escapeMetaChar(query);
		}

		mTask = new TextLoadTask();
		mTask.execute(mPath);
	}

	@Override
	protected void onResume() {
		super.onResume();
		final List<String> recent = mPrefs.getRecent(this);
		mRecentAdapter.clear();
		mRecentAdapter.addAll(recent);
		mRecentAdapter.notifyDataSetChanged();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.textview_menu, menu);
        return true;
    }

	private void setViewMode() {
		// 0-visiable, 4- invisiable, 8- gone
		if (mEditMode) {
			mTextPreview.setVisibility(8);
			mEditText.setVisibility(0);
			if(!mTextPreview.getText().equals("") || mTextPreview.getText() != null)
				mEditText.setText(mTextPreview.getText());
			mTextPreview.setText("");
			mTitle = getString(R.string.app_edit_view) + " : " + mPath.substring(mPath.lastIndexOf("/") + 1);
		} else {
			mTextPreview.setVisibility(0);
			mEditText.setVisibility(8);
			if(!mEditText.getText().equals("") || mEditText.getText() != null)
				mTextPreview.setText(mEditText.getText());
			mEditText.setText("");
			mTitle = getString(R.string.app_text_view) + " : " + mPath.substring(mPath.lastIndexOf("/") + 1);
		}
		getSupportActionBar().setTitle(mTitle);
		invalidateOptionsMenu();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mTextChanged)
					dialogSave();
				else
					//NavUtils.navigateUpFromSameTask(this);
					super. onBackPressed();
				return true;
			case R.id.item_view_mode:
				mEditMode = !mEditMode;
				setViewMode();
				return true;
			case R.id.item_view_save:
				saveFile(false);
				return true;
			case R.id.item_view_font_size:
				setFontSize();
				return true;
		}
        return super.onOptionsItemSelected(item);
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item_view = menu.findItem(R.id.item_view_mode);
		MenuItem item_save = menu.findItem(R.id.item_view_save);
		if (mTextPreview.getVisibility() == 0) {
			item_view.setIcon(R.drawable.ic_eye);
			if (mTextChanged)
				item_save.setVisible(true);
			else
				item_save.setVisible(false);
		} else {
			item_view.setIcon(R.drawable.ic_pencil);
			item_save.setVisible(true);
		}
		//menu.findItem(R.id.item_file).getSubMenu().setGroupEnabled(R.id.group_file, true);
		return super.onPrepareOptionsMenu(menu);
	}

	private void setFontSize() {
		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.fontsize_number_picker, null);
		
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
		alertDialog.setIcon(R.drawable.ic_format_size);
		alertDialog.setTitle(getString(R.string.title_font_size));

		alertDialog.setView(dialoglayout);
		
		NumberPicker np = (NumberPicker) dialoglayout.findViewById(R.id.np);
		// Скрыть клавиатуру
		np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		np.setMinValue(12);
		np.setMaxValue(32);
        np.setWrapSelectorWheel(true);
		np.setValue(mFontSize);
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
				@Override
				public void onValueChange(NumberPicker picker, int oldVal, int newVal){
					//Display the newly selected number from picker
					mFontSize = newVal;
				}
			});
		alertDialog.setPositiveButton(R.string.action_ok,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mPrefs.mFontSize = mFontSize;
					mPrefs.savePrefs(TextViewerActivity.this);
					mTextPreview.setTextSize(mFontSize);
					mEditText.setTextSize(mFontSize);
				}
			});
		alertDialog.setNegativeButton(R.string.action_cancel,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});

		alertDialog.show();		
	}

	private void showReplaceDialog() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
		alertDialog.setIcon(R.drawable.ic_message_draw);
		alertDialog.setTitle(getString(R.string.title_replace));
		alertDialog.setMessage(getString(R.string.msg_replace, mQuery));

		LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_layout_replace, null);
		alertDialog.setView(view);
		final AutoCompleteTextView edittext = (AutoCompleteTextView) view.findViewById(R.id.replace_query_input);
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
					mPrefs.addRecent(mContext, edittext.getText().toString());
					mReplece = new ReplaceTask(1);
					mReplece.delegate = TextViewerActivity.this;
					mReplaceQuery = edittext.getText().toString();
					mReplece.execute(mQuery, mReplaceQuery);
				}
			});

		alertDialog.setNegativeButton(R.string.action_cancel,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});

		alertDialog.show();
	}

	@Override
	public void onProcessFinish(boolean result, int id) {
		// TODO: Implement this method
		startLoadFile(mReplaceQuery);
	}

	private void dialogSave() {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
		builderSingle.setIcon(R.drawable.ic_content_save);
		builderSingle.setTitle(getString(R.string.title_save_file));
		builderSingle.setMessage(getString(R.string.msg_save_file, mPath.substring(mPath.lastIndexOf("/") + 1)));

		builderSingle.setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		builderSingle.setPositiveButton(R.string.action_yes,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					saveFile(true);
				}
			});
		builderSingle.setNeutralButton(R.string.action_no,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
//					NavUtils.navigateUpFromSameTask(TextViewerActivity.this);
					finish();
				}
			});

		builderSingle.show();
	}

	private void saveFile(boolean exit) {
		if (mPrefs.mCreateBackup) {
			// создать резервную копию
			try {
				mUtils.copyFile(new File(mPath), new File(mPath + "~"));
			} catch (IOException e) {}
		}

		String text = null;
		if (mTextPreview.getVisibility() == 0) {
			text = mTextPreview.getText().toString();
		} else {
			text = mEditText.getText().toString();
		}
		mUtils.saveFile(new File(mPath), text);

		if (exit)
//			NavUtils.navigateUpFromSameTask(this);
			finish();
		else {
			mTextChanged = false;
			getSupportActionBar().setTitle(mTitle);
		}
		String msg = getString(R.string.msg_saved_file, mPath.substring(mPath.lastIndexOf("/") + 1));
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}


	/********************************************************************************/
	/***** Замена в файлах *****/
    class ReplaceTask extends AsyncTask<String, Void, Boolean>
    {
        private ProgressDialog mProgressDialog;
        private boolean mCancelled;
		private StringBuffer buffer;

		public AsyncResponse delegate = null;
		private int mResultId;

		public ReplaceTask(int id) {
			this.mResultId = id;
		}

        @Override
        protected void onPreExecute() {
            mCancelled = false;
            mProgressDialog = new ProgressDialog(mContext);
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
			File file = new File(mPath);
			Pattern pattern;
			if (mPrefs.mIgnoreCase) {
				pattern = Pattern.compile(mQuery, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
			} else {
				pattern = Pattern.compile(mQuery);
			}
			if (mPrefs.mWordOnly) {
				pattern = Pattern.compile("\\b" + mQuery + "\\b");
			}

			buffer = mUtils.replaceFile(file, params[1], pattern);
			if (buffer != null) {
				if (mPrefs.mCreateBackup) {
					// создать резервную копию
					try {
						mUtils.copyFile(file, new File(mPath + "~"));
					} catch (IOException e) {}
				}

				mUtils.saveFile(file, buffer.toString());
				res = true;
			}
			return res;
		}

        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();
            mProgressDialog = null;

            Toast.makeText(getApplicationContext(), 
						   result ?R.string.msg_search_canceled: R.string.msg_search_canceled, 
						   Toast.LENGTH_LONG).show();
			delegate.onProcessFinish(result, mResultId);
			mReplece = null;

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onPostExecute(false);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (isCancelled()) {
                return;
            }
        }
	}


    class TextLoadTask extends AsyncTask<String, Integer, Boolean>
	{
        int mOffsetForLine=-1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected Boolean doInBackground(String... params) {
            File f = new File(params[0]);
            if (f.exists()) {

                InputStream is;
                try {
                    is = new BufferedInputStream(new FileInputStream(f) , 65536);
                    is.mark(65536);

					String encode = mUtils.getDetectedEncoding(is);
					is.reset();

                    BufferedReader br=null;
					mLoadText = new StringBuilder();
                    try {
                        if (encode != null) {
                            br = new BufferedReader(new InputStreamReader(is , encode) , 8192);
                        } else {
                            br = new BufferedReader(new InputStreamReader(is) , 8192);
                        }

                        String text;
                        while ((text = br.readLine()) != null) {
							mLoadText.append(text + "\n");
                        }
                        br.close();
                        is.close();
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return false;
        }

		@Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Pattern pattern;
                if (mPrefs.mIgnoreCase) {
                    pattern = Pattern.compile(mQuery, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                } else {
                    pattern = Pattern.compile(mQuery);
                }
				if (mPrefs.mWordOnly) {
					pattern = Pattern.compile("\\b" + mQuery + "\\b");
				}

				mTextPreview.setText(mUtils.highlightKeyword(mLoadText.toString(), pattern, mPrefs.mHighlightFg , mPrefs.mHighlightBg));
                mTask = null;
            }
        }

	}
}
