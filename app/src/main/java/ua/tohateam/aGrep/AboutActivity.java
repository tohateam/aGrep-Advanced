package ua.tohateam.aGrep;

import android.app.*;
import android.content.pm.*;
import android.os.*;
import android.widget.*;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class AboutActivity extends AppCompatActivity 
{
	private Toolbar toolbar;
	
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);
		
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle(getString(R.string.app_about));
				getSupportActionBar().setDisplayShowHomeEnabled(true);
				getSupportActionBar().setLogo(R.drawable.ic_launcher);
				getSupportActionBar().setDisplayUseLogoEnabled(true);
				//toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
			}
		}
		
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
