package ua.tohateam.aGrep;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.net.*;
import android.os.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import ua.tohateam.aGrep.utils.*;

import android.app.AlertDialog;
import android.support.v7.widget.Toolbar;

public class TextViewerActivity extends AppCompatActivity 
implements AsyncResponse
{	
    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_PATH = "path";

	private Toolbar toolbar;
	ActionMode mActionMode;
	
	private MyUtils mUtils;
    private Prefs mPrefs;
	private Context mContext;
	private TextLoadTask mTask;
	private ReplaceTask mReplece;

	private ArrayAdapter<String> mRecentAdapter;
	private TextView mTextPreview;
	private StringBuilder mLoadText = null;

	private String mPath;
	private String mQuery;
	private String mReplaceQuery;

	private int mFontSize;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.textview_activity);

		mContext = this;
		mUtils = new MyUtils();
		mPrefs = Prefs.loadPrefes(this);
		mFontSize = mPrefs.mFontSize;

		Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                mPath = extra.getString(EXTRA_PATH);
                mQuery = extra.getString(EXTRA_QUERY);
				startLoadFile(mQuery);
            }
		}

		String title = getString(R.string.app_text_view) + " : " + mPath.substring(mPath.lastIndexOf("/") + 1);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle(title);
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
        mRecentAdapter = new ArrayAdapter<String>(mContext, 
												  android.R.layout.simple_dropdown_item_1line, 
												  new ArrayList <String>());
	}

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		Resources res = getResources();

		switch (item.getItemId()) {
			case R.id.item_view_font_up:
				if (mFontSize < res.getInteger(R.integer.max_font_size))
					mFontSize = mFontSize + 1;
				mTextPreview.setTextSize(mFontSize);
				return true;
			case R.id.item_view_font_down:
				if (mFontSize > res.getInteger(R.integer.min_font_size))
					mFontSize = mFontSize - 1;
				mTextPreview.setTextSize(mFontSize);
				return true;
			case R.id.item_view_font_default:
				mFontSize = mPrefs.mFontSize;
				mTextPreview.setTextSize(mFontSize);
				return true;
		}
        return super.onOptionsItemSelected(item);
	}

	private void showReplaceDialog() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
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
						   result ?R.string.msg_grep_finished: R.string.msg_grep_canceled, 
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
