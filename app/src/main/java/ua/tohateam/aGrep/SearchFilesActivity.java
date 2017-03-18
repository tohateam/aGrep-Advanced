package ua.tohateam.aGrep;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.view.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;
import android.widget.SearchView.*;
import java.util.*;
import ua.tohateam.aGrep.model.*;
import ua.tohateam.aGrep.utils.*;

import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;

public class SearchFilesActivity extends AppCompatActivity 
implements SearchFiles.SearchFilesCallback,
SearchView.OnQueryTextListener
{	
	private Toolbar toolbar;
	//private ActionMode mActionMode;

	private MyUtils mUtils;
    private Prefs mPrefs;
	private Context mContext;

	private ArrayAdapter<String> mRecentAdapter;
	private ArrayList<SearchModel> mData;

	//SearchView sv;
	private RecyclerView mFilesList;
	private MyAdapter mFilesAdaptor;
	private MenuItem searchMenuItem;

	private SearchView searchView;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searchfiles_activity);

		mContext = this;
		mUtils = new MyUtils();
		mPrefs = Prefs.loadPrefes(this);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle(getString(R.string.app_search_files));
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setHomeButtonEnabled(true);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}

        mRecentAdapter = new ArrayAdapter<String>(mContext, 
												  R.layout.my_spinner_item, 
												  new ArrayList <String>());
		mData = new ArrayList<SearchModel>();
        mFilesList = (RecyclerView) findViewById(R.id.myRecycler);

		Intent it = getIntent();
		if (it != null && Intent.ACTION_SEARCH.equals(it.getAction())) {
            Bundle extras = it.getExtras();
            String mQuery = extras.getString(SearchManager.QUERY);
			getSupportActionBar().setTitle(getString(R.string.app_search_result) + " : " + mQuery);

            if (mQuery != null && mQuery.length() > 0) {
				SearchFiles searchFiles = new SearchFiles(this);
				searchFiles.initSearchFiles(mQuery);
            } else {
                finish();
            }
		}
	}

	@Override
	public void onResultCallback(ArrayList<SearchModel> data, int idResult, boolean result) {
		// TODO: Implement this method
		int id = idResult;
		boolean res = result;

		if (idResult == 0 && data.size() > 0) {
			// Сортируем по пути
			Collections.sort(data, new Comparator<SearchModel>() {
					@Override
					public int compare(SearchModel p1, SearchModel p2) {
						return p1.getPath().getParent().compareToIgnoreCase(p2.getPath().getParent());
					}
				});
			showResult(data);
		}
	}

	private void showResult(ArrayList<SearchModel> data) {
		mFilesList.setLayoutManager(new LinearLayoutManager(this));
        mFilesList.setItemAnimator(new DefaultItemAnimator());
        mFilesAdaptor=new MyAdapter(this, data);
        mFilesList.setAdapter(mFilesAdaptor);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.searchfiles_menu, menu);

		final MenuItem searchItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		searchView.setOnQueryTextListener(this);

		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// TODO: Implement this method
		return false;
	}

	@Override
	public boolean onQueryTextChange(String query) {
		if(mFilesAdaptor != null)
			mFilesAdaptor.getFilter().filter(query);
		return false;
	}
	
}
