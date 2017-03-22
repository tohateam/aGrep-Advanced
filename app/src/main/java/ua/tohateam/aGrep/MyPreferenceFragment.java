package ua.tohateam.aGrep;

import android.content.pm.*;
import android.os.*;
import android.preference.*;

public class MyPreferenceFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

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
	}
}
