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

public class ViewerActivity extends AppCompatActivity 
implements AsyncResponse
{	
    private static final String EXTRA_QUERY = "query";
    private static final String EXTRA_PATH = "path";

	private static final String STATE_PATH = "viewPath";
	private static final String STATE_QUERY = "viewQuery";

	private Toolbar toolbar;
	private MyUtils mUtils;
    private Prefs mPrefs;
	private Context mContext;
	private TextLoadTask mTask;
	private ReplaceTask mReplece;

	private ArrayAdapter<String> mRecentAdapter;
	private TextView mTextPreview;
	private TextView mTextPath;
	private StringBuilder mLoadText = null;

	private String mPath;
	private String mQuery;
	private String mReplaceQuery;
//	private String mTitle;
//	private String mSubTitle;

	private int mFontSize = 14;

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
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setHomeButtonEnabled(true);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}

		mTextPreview = (TextView) findViewById(R.id.text_view_body);
		mTextPreview.setTextSize(mFontSize);
		mTextPreview.setTextIsSelectable(true);
		mTextPath = (TextView) findViewById(R.id.text_view_path);
		
        mRecentAdapter = new ArrayAdapter <String>(mContext, 
												   R.layout.my_spinner_item, 
												   new ArrayList <String>());

		if (savedInstanceState != null) {
			mPath = savedInstanceState.getString(STATE_PATH);
			mQuery = savedInstanceState.getString(STATE_QUERY);
		} else {
			Intent it = getIntent();
			if (it != null) {
				Bundle extra = it.getExtras();
				if (extra != null) {
					mPath = extra.getString(EXTRA_PATH);
					mQuery = extra.getString(EXTRA_QUERY);
				}
			}
		}
//		mTitle = getString(R.string.app_text_view) + ":";
//		mSubTitle = mPath.substring(mPath.lastIndexOf("/") + 1);
		getSupportActionBar().setTitle(getString(R.string.app_text_view) + ":");
		getSupportActionBar().setSubtitle(mPath.substring(mPath.lastIndexOf("/") + 1));
		mTextPath.setText(mPath);
		
		startLoadFile(mQuery);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putString(STATE_PATH, mPath);
		savedInstanceState.putString(STATE_QUERY, mQuery);
		super.onSaveInstanceState(savedInstanceState);
	}

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				super.onBackPressed();
				return true;
			case R.id.item_view_mode:
				Intent it = new Intent(ViewerActivity.this, EditorActivity.class);
				it.setAction(Intent.ACTION_SEARCH);
				it.putExtra("path", mPath);
				startActivityForResult(it, 1);
				return true;
			case R.id.item_view_font_size:
				setFontSize();
				return true;
			case R.id.item_view_copy:
				ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("aGrep Text Viewer", mTextPreview.getText());
				cm.setPrimaryClip(clip);
				Toast.makeText(ViewerActivity.this, R.string.msg_copied, Toast.LENGTH_LONG).show();
				return true;
			case R.id.item_view_replace:
				showReplaceDialog();
				return true;
			case R.id.item_view_send:
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);					
				intent.setDataAndType(Uri.parse("file://" + mPath), "text/plain");
				startActivity(intent);
				return true;
		}
        return super.onOptionsItemSelected(item);
	}

	// Возрат из редактора
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (requestCode == 1) {
		if (resultCode == RESULT_OK) {
			startLoadFile(mQuery);
		}
		if (resultCode == RESULT_CANCELED) {
			//Do nothing?
		}
//		}
	}//onActivityResult

	@Override
	public void onBackPressed() {
		super. onBackPressed();
	}

	// Установка размера шрифта
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
				public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
					//Display the newly selected number from picker
					mFontSize = newVal;
				}
			});
		alertDialog.setPositiveButton(R.string.action_ok,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mPrefs.mFontSize = mFontSize;
					mPrefs.savePrefs(ViewerActivity.this);
					mTextPreview.setTextSize(mFontSize);
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
					// Добавляем в историю
					mPrefs.addRecent(mContext, edittext.getText().toString());
					mReplece = new ReplaceTask(1);
					mReplece.delegate = ViewerActivity.this;
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
