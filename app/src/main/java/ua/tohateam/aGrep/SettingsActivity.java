package ua.tohateam.aGrep;

import android.content.*;
import android.content.SharedPreferences.*;
import android.content.pm.*;
import android.os.*;
import android.preference.*;
import com.rarepebble.colorpicker.*;
import ua.tohateam.aGrep.utils.*;

public class SettingsActivity extends PreferenceActivity
implements OnSharedPreferenceChangeListener
{
	final private static int REQUEST_CODE_HIGHLIGHT = 0x1000;
    final private static int REQUEST_CODE_BACKGROUND = 0x1001;
	
	private final String KEY_APPVERSION = "application_version";
	private final String KEY_FONTSIZE = "FontSize";
    private final String KEY_HIGHLIGHTFG = "HighlightFg";
    private final String KEY_HIGHLIGHTBG = "HighlightBg";

	private SharedPreferences mPreference;
	private ListPreference mPrefFontSize;
	
	
	private int mFontSize = 12;
    private int mHighlightBg = 0xFF00FFFF;
    private int mHighlightFg = 0xFF000000;
	
	private PreferenceManager mPm;
	private PreferenceScreen mPs;

	private ColorPickerView pickerFg;
	private ColorPickerView pickerBg;
	//private PreferenceScreen mPs = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		
		mPreference = PreferenceManager.getDefaultSharedPreferences(this);
		mPreference.registerOnSharedPreferenceChangeListener(this);

		mPm = getPreferenceManager();
        mPs = mPm.createPreferenceScreen(this);
		
		Preference mApplicationVersion = findPreference(KEY_APPVERSION);
		String mAppVersion;
		try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mAppVersion = pInfo.versionName;
        } catch(PackageManager.NameNotFoundException e) {
            mAppVersion = "unknown";
        }
		mApplicationVersion.setSummary(mAppVersion);
		
		mPrefFontSize = (ListPreference) findPreference(KEY_FONTSIZE);
		mFontSize = Integer.parseInt(mPreference.getString(KEY_FONTSIZE , "-1"));
		setSummaries(KEY_FONTSIZE);

		mHighlightFg = mPreference.getInt(KEY_HIGHLIGHTFG , 0xFF000000);
		mHighlightBg = mPreference.getInt(KEY_HIGHLIGHTBG , 0xFF00FFFF);

		pickerFg = new ColorPickerView(this);
		pickerFg.setColor(mHighlightFg);

		pickerBg = new ColorPickerView(this);
		pickerBg.setColor(mHighlightBg);
		
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preference, String key) {
		// TODO: Implement this method
		SharedPreferences.Editor editor = mPreference.edit();
		
		if(key.equals(KEY_HIGHLIGHTFG)) {
			mHighlightFg = pickerFg.getColor();
			editor.putInt(KEY_HIGHLIGHTFG, mHighlightFg);
		} else if(key.equals(KEY_HIGHLIGHTBG)) {
			mHighlightBg = pickerBg.getColor();
			editor.putInt(KEY_HIGHLIGHTBG, mHighlightBg);
		}
		editor.commit();
		
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
	
	private void setSummaries(String key) {
        /* update summary font */
        if (key.equals(KEY_FONTSIZE)) {
            mPrefFontSize.setSummary((mPrefFontSize).getEntry());
        }		
	}
	

}
