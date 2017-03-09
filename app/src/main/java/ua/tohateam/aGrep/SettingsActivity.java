package ua.tohateam.aGrep;

import android.content.*;
import android.content.SharedPreferences.*;
import android.content.pm.*;
import android.os.*;
import android.preference.*;
import ua.tohateam.aGrep.utils.*;

public class SettingsActivity extends PreferenceActivity
implements OnSharedPreferenceChangeListener
{
	final private int REQUEST_CODE_HIGHLIGHT = 0x1000;
    final private int REQUEST_CODE_BACKGROUND = 0x1001;
	
	private final String KEY_APPVERSION = "application_version";
	private final String KEY_FONTSIZE = "FontSize";
    private final String KEY_HIGHLIGHTFG = "HighlightFg";
    private final String KEY_HIGHLIGHTBG = "HighlightBg";

	private SharedPreferences mPreference;
	private Preference mColorFg;
	private Preference mColorBg;
	private ListPreference mPrefFontSize;
	
	private int mFontSize = 12;
    private int mHighlightBg = 0xFF00FFFF;
    private int mHighlightFg = 0xFF000000;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		
		mPreference = PreferenceManager.getDefaultSharedPreferences(this);
		mPreference.registerOnSharedPreferenceChangeListener(this);
		
		Preference mApplicationVersion = findPreference(KEY_APPVERSION);
		String mAppVersion;
		try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mAppVersion = pInfo.versionName;
        } catch(PackageManager.NameNotFoundException e) {
            mAppVersion = "unknown";
        }
		mApplicationVersion.setSummary(mAppVersion);
		
		mColorFg = findPreference(KEY_HIGHLIGHTFG);
		mColorFg.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(SettingsActivity.this , ColorPickerActivity.class);
					intent.putExtra(ColorPickerActivity.EXTRA_TITLE, getString(R.string.title_highlight_fg));
					startActivityForResult(intent, REQUEST_CODE_HIGHLIGHT);
					return true;
				}
			});

		mColorBg = findPreference(KEY_HIGHLIGHTBG);
		mColorBg.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(SettingsActivity.this , ColorPickerActivity.class);
					intent.putExtra(ColorPickerActivity.EXTRA_TITLE, getString(R.string.title_highlight_bg));
					startActivityForResult(intent, REQUEST_CODE_BACKGROUND);
					return true;
				}
			});
		
		mPrefFontSize = (ListPreference) findPreference(KEY_FONTSIZE);
		mFontSize = Integer.parseInt(mPreference.getString(KEY_FONTSIZE , "-1"));
		setSummaries(KEY_FONTSIZE);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences preference, String key) {
		// TODO: Implement this method
		setSummaries(key);
	}

	@Override
	public void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            int color = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR, 0x00FFFF);
            if (requestCode == REQUEST_CODE_HIGHLIGHT) {
                mHighlightFg = color;
            } else if (requestCode == REQUEST_CODE_BACKGROUND) {
                mHighlightBg = color;
            }
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.edit()
				.putInt(Prefs.KEY_HIGHLIGHTFG, mHighlightFg)
				.putInt(Prefs.KEY_HIGHLIGHTBG, mHighlightBg)
				.apply();
        }
    }
	
	private void setSummaries(String key) {
        /* update summary font */
        if (key.equals(KEY_FONTSIZE)) {
            mPrefFontSize.setSummary((mPrefFontSize).getEntry());
        }	
	}
	

}
