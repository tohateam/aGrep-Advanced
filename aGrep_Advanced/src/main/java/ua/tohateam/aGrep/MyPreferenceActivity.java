package ua.tohateam.aGrep;

import android.os.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;

public class MyPreferenceActivity extends AppCompatActivity
{

	Toolbar toolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_activity);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle(getString(R.string.app_settings));
				getSupportActionBar().setLogo(R.drawable.ic_launcher);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				
				toolbar.setNavigationOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							onBackPressed();
						}
					});
			}
		}

		getFragmentManager()
			.beginTransaction()
			.replace(R.id.fragment_container, 
					 new MyPreferenceFragment())
			.commit();
	}
}
