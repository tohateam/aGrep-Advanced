package ua.tohateam.aGrep;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.werdpressed.partisan.rundo.*;
import java.io.*;
import ua.tohateam.aGrep.utils.*;

import android.support.v7.widget.Toolbar;

public class EditorActivity extends AppCompatActivity 
implements RunDo.TextLink, RunDo.Callbacks
{
	private static final String TAG = "aGrep-Editor";
	private static final String EXTRA_TEXT = "text";
	private static final String EXTRA_FILENAME = "filename";
	private static final String EXTRA_CHANGED = "changed";
	private static final String EXTRA_PATH = "path";


	private Toolbar toolbar;
	private MyUtils mUtils;
	private Prefs mPrefs;
	private EditText mText;

	private String mFileName = "";
	private boolean mChanged = false;
	private boolean isChanged = false;
	private int mFontSize = 14;

    private RunDo mRunDo;
	private int mRundoCount = 20;
	private int mCountChanges = 0;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor_activity);

		mUtils = new MyUtils();
		mPrefs = Prefs.loadPrefes(this);
		mFontSize = mPrefs.mFontSizeEditor;

		// менеджер отмен
		mRunDo = RunDo.Factory.getInstance(getSupportFragmentManager());
		mRundoCount = mPrefs.mUndoSize;
		mRunDo.setQueueSize(mRundoCount);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setHomeButtonEnabled(true);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}

		mText = (EditText) findViewById(R.id.editor_text);
		mText.setTextSize(mFontSize);
		mText.addTextChangedListener(textWatcher);

		if (savedInstanceState != null) {
        	restoreState(savedInstanceState);
        } else {
			Intent it = getIntent();
			String path = null;
			if (it != null) {
				Bundle extra = it.getExtras();
				if (extra != null) {
					path = extra.getString(EXTRA_PATH);
				}
			}
			openNamedFile(path);
		}
	}

	@Override
	public EditText getEditTextForRunDo() {
		//Log.i(TAG, "EditText Called");
		return (mText);
	}
	@Override
	public void undoCalled() {
		// TODO: Implement this method
		Log.i(TAG, "undoCalled");
	}

	@Override
	public void redoCalled() {
		// TODO: Implement this method
		Log.i(TAG, "redoCalled");
	}

	// Определение изменения файла
	private TextWatcher textWatcher = new TextWatcher() {
		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			if (after == 1 || count == 1) {
				if (!mChanged) {
					mChanged = true;
					updateTitle();
				}
			}
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
	};

	private void restoreState(Bundle state) {
		mText.setText(state.getString(EXTRA_TEXT));
        mFileName = state.getString(EXTRA_FILENAME);
        mChanged = state.getBoolean(EXTRA_CHANGED);
    }

	protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_TEXT, mText.getText().toString());
        outState.putString(EXTRA_FILENAME, mFileName);
        outState.putBoolean(EXTRA_CHANGED, mChanged);
    }

	protected void openNamedFile(String filename) {
		File f = new File(filename);
		StringBuilder mLoadText = new StringBuilder();

		InputStream is;
		try {
			is = new BufferedInputStream(new FileInputStream(f) , 65536);
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

				String text;
				while ((text = br.readLine()) != null) {
					mLoadText.append(text + "\n");
				}
				br.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		mText.setText(mLoadText);
		mChanged = false;
		mFileName = filename;
		updateTitle();
	}

	private void updateTitle() {
		String subTitle;
		if (mFileName.equals("")) {
			subTitle = "";
		} else {
			subTitle = mFileName.substring(mFileName.lastIndexOf("/") + 1);
		}
		if (mChanged) {
			subTitle = subTitle + "*";
		}
		toolbar.setTitle(getString(R.string.app_edit_view) + ":");
		toolbar.setSubtitle(subTitle);

		invalidateOptionsMenu();
	}

	@Override
	public void onBackPressed() {       
	    if (mChanged) {
			showDialogSave();
	    } else {
//	        super.onBackPressed();
			onExitEditor();
	    }
	}

	private void showDialogSave() {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
		builderSingle.setIcon(R.drawable.ic_content_save);
		builderSingle.setTitle(getString(R.string.title_save_file));
		builderSingle.setMessage(getString(R.string.msg_save_file, mFileName));

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
//					EditorActivity.super.onBackPressed();
					onExitEditor();
				}
			});

		builderSingle.show();
	}

	private void saveFile(boolean exit) {
		if (mPrefs.mCreateBackup) {
			// создать резервную копию
			try {
				mUtils.copyFile(new File(mFileName), new File(mFileName + "~"));
			} catch (IOException e) {}
		}

		String text = mText.getText().toString();
		mUtils.saveFile(new File(mFileName), text);
		isChanged = true;

		if (exit) {
			onExitEditor();
		} else {
			mChanged = false;
			updateTitle();
		}
		String msg = getString(R.string.msg_saved_file, mFileName);
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mChanged)
					showDialogSave();
				else {
					onExitEditor();
				}
				return true;
			case R.id.item_edit_save:
				saveFile(false);
				return true;
            case R.id.item_edit_undo:
                mRunDo.undo();
				return true;
            case R.id.item_edit_redo:
                mRunDo.redo();
				return true;				
			case R.id.item_edit_font_size:
				setFontSize();
				return true;
		}
        return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item_save = menu.findItem(R.id.item_edit_save);
		Drawable icon_save = item_save.getIcon();

		MenuItem item_undo = menu.findItem(R.id.item_edit_undo);
		Drawable icon_undo = item_save.getIcon();

		MenuItem item_redo = menu.findItem(R.id.item_edit_redo);
		Drawable icon_redo = item_save.getIcon();

		if (mChanged) {
			item_save.setEnabled(true);
			if (icon_save != null) {
				icon_save.mutate();
				icon_save.setColorFilter(getResources().getColor(R.color.icons), PorterDuff.Mode.SRC_ATOP);
			}
			item_undo.setEnabled(true);
			if (icon_undo != null) {
				icon_undo.mutate();
				icon_undo.setColorFilter(getResources().getColor(R.color.icons), PorterDuff.Mode.SRC_ATOP);
			}
			item_redo.setEnabled(true);
			if (icon_redo != null) {
				icon_redo.mutate();
				icon_redo.setColorFilter(getResources().getColor(R.color.icons), PorterDuff.Mode.SRC_ATOP);
			}
		} else {
			item_save.setEnabled(false);
			if (icon_save != null) {
				icon_save.mutate();
				icon_save.setColorFilter(getResources().getColor(R.color.icons_disable), PorterDuff.Mode.SRC_ATOP);
			}
			item_undo.setEnabled(false);
			if (icon_undo != null) {
				icon_undo.mutate();
				icon_undo.setColorFilter(getResources().getColor(R.color.icons_disable), PorterDuff.Mode.SRC_ATOP);
			}
			item_redo.setEnabled(false);
			if (icon_redo != null) {
				icon_redo.mutate();
				icon_redo.setColorFilter(getResources().getColor(R.color.icons_disable), PorterDuff.Mode.SRC_ATOP);
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	private void onExitEditor() {
		Intent returnIntent = new Intent();
		if (isChanged) {
			setResult(RESULT_OK, returnIntent);
		} else {
			setResult(RESULT_CANCELED, returnIntent);
		}
		mRunDo.clearAllQueues();
		finish();
	}

	// Установка размера шрифта
	private void setFontSize() {
		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.fontsize_number_picker, null);

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
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
					mPrefs.mFontSizeEditor = mFontSize;
					mPrefs.savePrefs(EditorActivity.this);
					mText.setTextSize(mFontSize);
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

}
