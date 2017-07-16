package ua.tohateam.aGrep;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.ExFilePickerParcelObject;
import ua.tohateam.aGrep.search.SearchResultActivity;
import ua.tohateam.aGrep.settings.Prefs;
import ua.tohateam.aGrep.settings.SettingsActivity;
import ua.tohateam.aGrep.settings.ThemeChoose;
import ua.tohateam.aGrep.textviewer.TextviewerActivity;
import ua.tohateam.aGrep.utils.CheckedString;
import ua.tohateam.aGrep.utils.EditHistoryActivity;

public class MainActivity extends AppCompatActivity {
//    private static final String TAG = "aGrep";

    private static final int EX_FILE_PICKER_RESULT = 0;
    private static final int REQUEST_EDIT_HISTORY = 1;
    private static final int OPEN_FILE_RESULT = 2;
    private static final int REQUEST_WRITE_STORAGE = 112;

    private ArrayAdapter<String> mRecentAdapter;
    private Prefs mPrefs;

    private LinearLayout mDirListView;
    private LinearLayout mExtListView;
    private View.OnLongClickListener mDirListener;
    private View.OnLongClickListener mExtListener;

    private CompoundButton.OnCheckedChangeListener mCheckListener;
    private AutoCompleteTextView mSearchQuery;

    private Context mContext;
    private long back_pressed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeChoose.onActivityCreateSetTheme(this);
        super.onCreate(savedInstanceState);

        mContext = this;
        mPrefs = Prefs.loadPrefes(this);
        setContentView(R.layout.main_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.app_name));
                getSupportActionBar().setLogo(R.drawable.ic_launcher);
            }
        }

        // Запрос на доступ к память
        // https://inducesmile.com/android/android-6-marshmallow-runtime-permissions-request-example
        if (ContextCompat
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission
                                .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat
                    .requestPermissions(MainActivity.this,
                            new String[]{Manifest
                                    .permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }

        initSearch();
    } // end onCreate


    // проверка разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initSearch();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //Show an explanation to the user *asynchronously*
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder
                            .setMessage("This permission is important to access cdcard.")
                            .setTitle("Important permission required")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                                    initSearch();
                                }
                            });
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
//                } else {
                    //Never ask again and handle your app without permission.
                }
            }
        }
    }

    private void initSearch() {
        mDirListView = (LinearLayout) findViewById(R.id.listdir);
        mExtListView = (LinearLayout) findViewById(R.id.listext);

        mDirListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final CheckedString strItem = (CheckedString) view.getTag();
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
                final String strText = (String) ((TextView) view).getText();
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
        final CheckBox chkHd = (CheckBox) findViewById(R.id.cb_hidden);

        chkRe.setChecked(mPrefs.mRegularExrpression);
        chkIc.setChecked(mPrefs.mIgnoreCase);
        chkWo.setChecked(mPrefs.mWordMath);
        chkHd.setChecked(mPrefs.mSearchHidden);

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
                mPrefs.mWordMath = chkWo.isChecked();
                mPrefs.savePrefs(mContext);
            }
        });

        chkHd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrefs.mSearchHidden = chkHd.isChecked();
                mPrefs.savePrefs(mContext);
            }
        });


        // Поиск
        mSearchQuery = (AutoCompleteTextView) findViewById(R.id.query_input);
        mSearchQuery.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_UP) {
                    startSearch(mSearchQuery.getText().toString());
                    return true;
                }
                return false;
            }
        });

        mRecentAdapter = new ArrayAdapter<>(mContext, R.layout.my_spinner_item, new ArrayList<String>());
        mSearchQuery.setAdapter(mRecentAdapter);

        ImageButton clrBtn = (ImageButton) findViewById(R.id.btn_clear_search);
        ImageButton searchBtn = (ImageButton) findViewById(R.id.btn_search);
        ImageButton historyBtn = (ImageButton) findViewById(R.id.btn_history_search);
        ImageButton btnClearDir = (ImageButton) findViewById(R.id.btn_clear_dir);
        ImageButton btnClearExt = (ImageButton) findViewById(R.id.btn_clear_ext);

        if(mPrefs.mAppTheme == 0) {
            clrBtn.setColorFilter(Color.argb(255, 50, 50, 50));
            searchBtn.setColorFilter(Color.argb(255, 50, 50, 50));
            historyBtn.setColorFilter(Color.argb(255, 50, 50, 50));
            btnClearDir.setColorFilter(Color.argb(255, 50, 50, 50));
            btnClearExt.setColorFilter(Color.argb(255, 50, 50, 50));
        }


        clrBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mSearchQuery.setText("");
                mSearchQuery.requestFocus();
            }
        });

        searchBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                startSearch(mSearchQuery.getText().toString());
            }
        });

        historyBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mSearchQuery.showDropDown();
            }
        });

        btnClearDir.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mPrefs.mDirList.removeAll(mPrefs.mDirList);
                refreshDirList();
                mPrefs.savePrefs(mContext);
            }
        });

        btnClearExt.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mPrefs.mExtList.removeAll(mPrefs.mExtList);
                mPrefs.mExtList.add(new CheckedString("txt"));
                refreshExtList();
                mPrefs.savePrefs(mContext);
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

        boolean error = false;
        if (mPrefs.mDirList.size() == 0 || !checkDir) {
            showToast(getString(R.string.msg_no_target_dir));
            error = true;
        } else if (mPrefs.mExtList.size() == 0 || !checkExt) {
            showToast(getString(R.string.msg_no_target_ext));
            error = true;
        } else if (query.equals("")) {
            mSearchQuery.setError(getString(R.string.msg_is_empty));
            error = true;
        }

        if (!error) {
            Intent it;
            it = new Intent(this, SearchResultActivity.class);
            it.setAction(Intent.ACTION_SEARCH);
            it.putExtra(SearchManager.QUERY, query);
            startActivity(it);
        } else {
            initSearch();
        }
    }

    public void showToast(String msg) {
        if (mPrefs.mShowToast)
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
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
                             ArrayList<CheckedString> list,
                             View.OnLongClickListener logclicklistener,
                             CompoundButton.OnCheckedChangeListener checkedChangeListener) {

        view.removeAllViews();
        Collections.sort(list, new Comparator<CheckedString>() {
            @Override
            public int compare(CheckedString object1, CheckedString object2) {
                return object1.string.compareToIgnoreCase(object2.string);
            }
        });

        for (CheckedString checkedString : list) {
            CheckBox checkBox = (CheckBox) View.inflate(this, R.layout.item_row_dirext, null);

            if (checkedString.equals("*.")) {
                checkBox.setText(R.string.action_no_ext);
            } else {
                checkBox.setText(checkedString.string);
            }

            checkBox.setChecked(checkedString.checked);
            checkBox.setTag(checkedString);
            checkBox.setOnLongClickListener(logclicklistener);
            checkBox.setOnCheckedChangeListener(checkedChangeListener);
            view.addView(checkBox);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EX_FILE_PICKER_RESULT) {
            if (data != null) {
                ExFilePickerParcelObject object = data.getParcelableExtra(ExFilePickerParcelObject.class.getCanonicalName());
                if (object.count > 0) {
                    for (int i = 0; i < object.count; i++) {
                        String dirname = object.path + object.names.get(i);
                        for (CheckedString t : mPrefs.mDirList) {
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
        } else if (requestCode == REQUEST_EDIT_HISTORY) {
            if (resultCode == RESULT_OK) {
                initSearch();
//            } else if (resultCode == RESULT_CANCELED) {
                //Do nothing?
            }
        } else if (requestCode == OPEN_FILE_RESULT) {
            if (data != null) {
                ExFilePickerParcelObject object = data.getParcelableExtra(ExFilePickerParcelObject.class.getCanonicalName());
                if (object.count > 0) {
                    String patch = object.path + object.names.get(0);
                    Intent it = new Intent(this, TextviewerActivity.class);
                    it.setAction(Intent.ACTION_SEARCH);
                    it.putExtra(SearchManager.QUERY, "");
                    it.putExtra("filePath", patch);
                    startActivity(it);
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
        } else if (item.getItemId() == R.id.item_edit_history) {
            Intent intent = new Intent(this, EditHistoryActivity.class);
            startActivityForResult(intent, 1);
        } else if (item.getItemId() == R.id.item_exit) {
            finish();
        } else if (item.getItemId() == R.id.item_open_file) {
            Intent intent = new Intent(getApplicationContext(), ru.bartwell.exfilepicker.ExFilePickerActivity.class);
            intent.putExtra(ExFilePicker.SET_CHOICE_TYPE, ExFilePicker.CHOICE_TYPE_FILES);
            intent.putExtra(ExFilePicker.SET_ONLY_ONE_ITEM, true);
            intent.putExtra(ExFilePicker.ENABLE_QUIT_BUTTON, true);
            intent.putExtra(ExFilePicker.DISABLE_SORT_BUTTON, true);
            intent.putExtra(ExFilePicker.DISABLE_NEW_FOLDER_BUTTON, true);
            intent.putExtra(ExFilePicker.SET_FILTER_EXCLUDE, new String[]{"png", "jpg", "apk", "raw", "zip", "rar", "tar", "~"});
            startActivityForResult(intent, OPEN_FILE_RESULT);
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
        startActivityForResult(intent, EX_FILE_PICKER_RESULT);
    }

    private void dialogAddExt() {
        final EditText edtInput = new EditText(mContext);
        edtInput.setSingleLine();
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.title_label_addext)
                .setView(edtInput)
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String ext = edtInput.getText().toString();

                        if (ext.length() > 0) {
                            for (CheckedString t : mPrefs.mExtList) {
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
                        for (CheckedString t : mPrefs.mExtList) {
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
        if (back_pressed + 500 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), getString(R.string.msg_back_pressed), Toast.LENGTH_SHORT).show();

        back_pressed = System.currentTimeMillis();
    }
}
