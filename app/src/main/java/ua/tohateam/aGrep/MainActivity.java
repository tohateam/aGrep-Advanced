package ua.tohateam.aGrep;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.util.*;
import ru.bartwell.exfilepicker.*;
import ua.tohateam.aGrep.utils.*;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatActivity 
{
	private static final String TAG= "aGrep";
	private static final int EX_FILE_PICKER_RESULT = 0;

	private Toolbar toolbar;

	private ArrayAdapter<String> mRecentAdapter;
	private Prefs mPrefs;
	private LinearLayout mDirListView;
    private LinearLayout mExtListView;
    private View.OnLongClickListener mDirListener;
    private View.OnLongClickListener mExtListener;
	private CompoundButton.OnCheckedChangeListener mCheckListener;
	private AutoCompleteTextView edittext;

	private Context mContext;
	private long back_pressed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		mContext = this;
		mPrefs = Prefs.loadPrefes(this);
        setContentView(R.layout.main_activity);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle(getString(R.string.app_name));
				getSupportActionBar().setLogo(R.drawable.ic_launcher);
			}
		}

		initSearch();
    } // end onCreate

	private void initSearch() {
        mDirListView = (LinearLayout) findViewById(R.id.listdir);
        mExtListView = (LinearLayout) findViewById(R.id.listext);

        mDirListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final CheckedString strItem = (CheckedString) view.getTag();
                // Show Dialog
                new AlertDialog.Builder(mContext)
                    .setTitle(R.string.title_remove_item)
                    .setMessage(getString(R.string.msg_remove_item, strItem))
                    .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mPrefs.mDirList.remove(strItem);
                            refreshDirList();
                            mPrefs.savePrefs(mContext);
                        }
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .setCancelable(true)
                    .show();
                return true;
            }
        };

        mExtListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final String strText = (String)((TextView) view).getText();
                final CheckedString strItem = (CheckedString) view.getTag();
                // Show Dialog
                new AlertDialog.Builder(mContext)
                    .setTitle(R.string.title_remove_item)
                    .setMessage(getString(R.string.msg_remove_item, strText))
                    .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mPrefs.mExtList.remove(strItem);
                            refreshExtList();
                            mPrefs.savePrefs(mContext);
                        }
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .setCancelable(true)
                    .show();
                return true;
            }
        };

        mCheckListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final CheckedString strItem = (CheckedString) buttonView.getTag();
                strItem.checked = isChecked;
                mPrefs.savePrefs(mContext);
            }
        };

        refreshDirList();
        refreshExtList();

        final CheckBox chkRe = (CheckBox) findViewById(R.id.cb_re);
        final CheckBox chkIc = (CheckBox) findViewById(R.id.cb_ic);
        final CheckBox chkWo = (CheckBox) findViewById(R.id.cb_mw);
		chkRe.setChecked(mPrefs.mRegularExrpression);
		chkIc.setChecked(mPrefs.mIgnoreCase);
		chkWo.setChecked(mPrefs.mWordOnly);

        chkRe.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mPrefs.mRegularExrpression = chkRe.isChecked();
					mPrefs.savePrefs(mContext);
				}
			});
        chkIc.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mPrefs.mIgnoreCase = chkIc.isChecked();
					mPrefs.savePrefs(mContext);
				}
			});

        chkWo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mPrefs.mWordOnly = chkWo.isChecked();
					mPrefs.savePrefs(mContext);
				}
			});

        // Поиск
        edittext = (AutoCompleteTextView) findViewById(R.id.search_text);
        edittext.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
						startSearch(edittext.getText().toString());
						return true;
					}
					return false;
				}
			});
        mRecentAdapter = new ArrayAdapter <String>(mContext, 
													 R.layout.my_spinner_item, 
													new ArrayList <String>());
//		mRecentAdapter.setDropDownViewResource(R.layout.my_spinner_dropdown_item);
        edittext.setAdapter(mRecentAdapter);

        ImageButton clrBtn = (ImageButton) findViewById(R.id.btn_clear_search);
        clrBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					edittext.setText("");
					edittext.requestFocus();
				}
			});

        ImageButton searchBtn = (ImageButton) findViewById(R.id.btn_search);
        searchBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					startSearch(edittext.getText().toString());
				}
			});

        ImageButton historyBtn = (ImageButton) findViewById(R.id.btn_history_search);
        historyBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					edittext.showDropDown();
				}
			});
	}

	private void startSearch(String query) {
		// Если не указаны директории поиска то выходим
		boolean checkDir = false;
		boolean checkExt = false;
		for (CheckedString dir : mPrefs.mDirList) {
			if (dir.checked) {
				checkDir = true;
			}
		}
		for (CheckedString ext : mPrefs.mExtList) {
			if (ext.checked) {
				checkExt = true;
			}
		}
		

		if (mPrefs.mDirList.size() == 0 || !checkDir) {
            Toast.makeText(getApplicationContext(), R.string.msg_no_target_dir, Toast.LENGTH_LONG).show();
        } else if (mPrefs.mExtList.size() == 0 || !checkExt) {
            Toast.makeText(getApplicationContext(), R.string.msg_no_target_ext, Toast.LENGTH_LONG).show();			
        } else if (query.equals("")) {
            Toast.makeText(getApplicationContext(), R.string.msg_no_search_query, Toast.LENGTH_LONG).show();			
		}

		Intent it = new Intent(this, SearchTextActivity.class);
		it.setAction(Intent.ACTION_SEARCH);
        it.putExtra(SearchManager.QUERY, query);
		startActivity(it);
	}

	@Override
	protected void onResume() {
		super.onResume();
		final List<String> recent = mPrefs.getRecent(this);
		mRecentAdapter.clear();
		mRecentAdapter.addAll(recent);
		mRecentAdapter.notifyDataSetChanged();
	}

	private void refreshDirList() {
        setListItem(mDirListView, mPrefs.mDirList, mDirListener, mCheckListener);
    }
    private void refreshExtList() {
        setListItem(mExtListView, mPrefs.mExtList, mExtListener, mCheckListener);
    }

	private void setListItem(LinearLayout view,
							 ArrayList <CheckedString> list,
							 View.OnLongClickListener logclicklistener,
							 CompoundButton.OnCheckedChangeListener checkedChangeListener) {

        view.removeAllViews();
        Collections.sort(list, new Comparator <CheckedString>() {
				@Override
				public int compare(CheckedString object1, CheckedString object2) {
					return object1.string.compareToIgnoreCase(object2.string);
				}
			});
        for (CheckedString s: list) {
            CheckBox v = (CheckBox) View.inflate(this, R.layout.item_row_dirext, null);
            if (s.equals("*.")) {
                v.setText(R.string.action_no_ext);
            } else {
                v.setText(s.string);
            }
            v.setChecked(s.checked);
            v.setTag(s);
            v.setOnLongClickListener(logclicklistener);
            v.setOnCheckedChangeListener(checkedChangeListener);
            view.addView(v);
        }
    }

    /**
     * Диалог выбора папок
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EX_FILE_PICKER_RESULT) {
            if (data != null) {
                ExFilePickerParcelObject object = data.getParcelableExtra(ExFilePickerParcelObject.class.getCanonicalName());
                if (object.count > 0) {
                    for (int i = 0; i < object.count; i++) {
                        String dirname = object.path + object.names.get(i);
                        for (CheckedString t: mPrefs.mDirList) {
                            if (t.string.equalsIgnoreCase(dirname)) {
                                return;
                            }
                        }
                        mPrefs.mDirList.add(new CheckedString(dirname));
                        refreshDirList();
                        mPrefs.savePrefs(MainActivity.this);
                    }
                }
            }
        }
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_preference) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.item_add_dir) {
			dialogAddDir();
		} else if (item.getItemId() == R.id.item_add_ext) {
			dialogAddExt();
		} else if (item.getItemId() == R.id.item_exit) {
			finish();
		}
        return super.onOptionsItemSelected(item);
    }

	private void dialogAddDir() {
		Intent intent = new Intent(getApplicationContext(), ru.bartwell.exfilepicker.ExFilePickerActivity.class);
		intent.putExtra(ExFilePicker.SET_CHOICE_TYPE, ExFilePicker.CHOICE_TYPE_DIRECTORIES);
		intent.putExtra(ExFilePicker.ENABLE_QUIT_BUTTON, true);
		intent.putExtra(ExFilePicker.DISABLE_SORT_BUTTON, false);
		intent.putExtra(ExFilePicker.DISABLE_NEW_FOLDER_BUTTON, true);
		//intent.putExtra(ExFilePicker.SET_FILTER_EXCLUDE, new String[]{"png", "jpg", "apk", "raw", "zip", "rar", "tar"});
		startActivityForResult(intent, EX_FILE_PICKER_RESULT);
	}
	private void dialogAddExt() {
		final EditText edtInput = new EditText(mContext);
		edtInput.setSingleLine();
		// Show Dialog
		new AlertDialog.Builder(mContext)
			.setTitle(R.string.title_label_addext)
			.setView(edtInput)
			.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String ext = edtInput.getText().toString();
					if (ext != null && ext.length() > 0) {
						for (CheckedString t: mPrefs.mExtList) {
							if (t.string.equalsIgnoreCase(ext)) {
								return;
							}
						}
						mPrefs.mExtList.add(new CheckedString(ext));
						refreshExtList();
						mPrefs.savePrefs(mContext);
					}
				}
			})
			.setNeutralButton(R.string.action_no_ext, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String ext = "*.";
					// двойная проверка
					for (CheckedString t: mPrefs.mExtList) {
						if (t.string.equalsIgnoreCase(ext)) {
							return;
						}
					}
					mPrefs.mExtList.add(new CheckedString(ext));
					refreshExtList();
					mPrefs.savePrefs(mContext);
				}
			})
			.setNegativeButton(R.string.action_cancel, null)
			.setCancelable(true)
			.show();
	}

	/*********************************************************************
	 * Выход по двойному нажатию на кнопку [назад]
	 *********************************************************************/
	@Override
	public void onBackPressed() {
		if (back_pressed + 2000 > System.currentTimeMillis())
			super.onBackPressed();
		else
			Toast.makeText(getBaseContext(), getString(R.string.msg_back_pressed), Toast.LENGTH_SHORT).show();
		back_pressed = System.currentTimeMillis();
	}

	/***************************************************************************
	 * Смена темы и перезапуск программы
	 **************************************************************************/
	public void changeTheme() {
        //PreferencesManager.getInstance().setAppTheme(selected_theme_id);
        restart();
    }

	public void restart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            recreate();
        } else {
            finish();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
