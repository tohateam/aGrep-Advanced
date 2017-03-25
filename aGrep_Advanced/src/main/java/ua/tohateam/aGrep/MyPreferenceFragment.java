package ua.tohateam.aGrep;

import android.content.*;
import android.content.SharedPreferences.*;
import android.content.pm.*;
import android.os.*;
import android.preference.*;
import android.support.v7.app.*;
import android.view.*;
import android.widget.*;
import com.rarepebble.colorpicker.*;
import ua.tohateam.aGrep.*;
import ua.tohateam.aGrep.views.*;

import ua.tohateam.aGrep.R;

public class MyPreferenceFragment extends PreferenceFragment
implements OnSharedPreferenceChangeListener
{	
	private final String KEY_HIGHLIGHTFG = "HighlightFg";
	private final String KEY_HIGHLIGHTBG = "HighlightBg";

	private SharedPreferences mPreference;
//	private SeekBarPreference mUndoSizePrefs;
	//private NumberPickerPreference mUndoSizePrefs;
	
	private int mHighlightBg = 0xFF00FFFF;
	private int mHighlightFg = 0xFF000000;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

		mPreference = PreferenceManager.getDefaultSharedPreferences(getActivity());

		Preference mApplicationVersion = findPreference("application_version");
		String mAppVersion;
		try {
            PackageInfo pInfo = getActivity()
				.getPackageManager()
				.getPackageInfo(getActivity().getPackageName(), 0);
            mAppVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            mAppVersion = "unknown";
        }
		mApplicationVersion.setSummary(mAppVersion);

		// Установка фона и цвета выделеного текста
		mHighlightFg = mPreference.getInt(KEY_HIGHLIGHTFG , 0xFF000000);
		mHighlightBg = mPreference.getInt(KEY_HIGHLIGHTBG , 0xFF00FFFF);

		final ColorPickerView pickerFg = new ColorPickerView(getContext());
		pickerFg.setColor(mHighlightFg);
		mHighlightFg = pickerFg.getColor();

		final ColorPickerView pickerBg = new ColorPickerView(getContext());
		pickerBg.setColor(mHighlightBg);
		mHighlightBg = pickerFg.getColor();

		// кол-во отмен
//		mUndoSizePrefs = (SeekBarPreference) findPreference(KEY_UNDO_SIZE);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//		if (KEY_UNDO_SIZE.equals(key)) {
//			mUndoSize = prefs.getInt(key, 50);
//			mUndoSizePrefs.setSummary(mUndoSize);
//		} else if (STR_1.equals(key)) {
//			String str = prefs.getString(key, "");
//			mStr1.setSummary(str);
//		} else if (STR_2.equals(key)) {
//			String str = prefs.getString(key, "");
//			mStr2.setSummary(str);
//		} else if (SEEK_1.equals(key)) {
//			int i = prefs.getInt(key, DEFAULT_1);
//			mSeek1.setSummary("$ " + i);
//		} else if (SEEK_2.equals(key)) {
//			int i = prefs.getInt(key, DEFAULT_2);
//			mSeek2.setSummary("$ " + i);
//		} else if (NUM_1.equals(key)) {
//			int i = prefs.getInt(key, DEFAULT_3);
//			mNum1.setSummary("" + i);
//		} 		
	}    
	@Override
	public void onResume() {
	    super.onResume();
	    mPreference.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		mPreference.unregisterOnSharedPreferenceChangeListener(this);
	}
}
