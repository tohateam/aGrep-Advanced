package ua.tohateam.aGrep;

import android.app.*;
import android.content.pm.*;
import android.os.*;
import android.widget.*;

public class AboutActivity extends Activity 
{
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);
		String mAppVersion;
		try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mAppVersion = pInfo.versionName;
        } catch(PackageManager.NameNotFoundException e) {
            mAppVersion = "unknown";
        }
		TextView aboutTitle = (TextView) findViewById(R.id.about_title);
		aboutTitle.setText(getString(R.string.app_name) +" "+ mAppVersion);
	}
}
