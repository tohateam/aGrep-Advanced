package ua.tohateam.aGrep;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import ua.tohateam.aGrep.model.*;
import ua.tohateam.aGrep.utils.*;

public class SearchText implements AsyncResponse
{
	private static final int RESULT_SEARCH = 0;
	private static final int RESULT_REPLACE = 1;

	private Context mContext;
	private MyUtils mUtils;
	private Prefs mPrefs;

	private Pattern mPattern;
	private GrepTask mTask;
	private ReplaceTask mReplaceTask;

//	private String mQuery;

	private ArrayList<SearchModel> mData;
	private SearchTextCallback mSearchTextCallback;

	public static interface SearchTextCallback
	{
		void onMethodCallback(ArrayList<SearchModel> data, int idResult, boolean result);
	}

	public SearchText(Context context) {
		this.mContext = context;
		//this.mReplaceQuery = replaceQuery;
		this.mPrefs = Prefs.loadPrefes(mContext);
		this.mUtils = new MyUtils();

		try {
			this.mSearchTextCallback = ((SearchTextCallback) context);
		} catch (ClassCastException e) {
			throw new ClassCastException("Activity must implement AdapterCallback.");
		}

	}

	public void startSearchText(String query) {
		//mQuery = query;
		mData = new ArrayList<SearchModel>();

		if (query != null && query.length() > 0) {
			mPrefs.addRecent(mContext, query);
			mPattern = mUtils.getPattern(mContext, query);

			startTaskSearch(query);
		}
	}

	public void startReplaceText(String query, String replace, boolean replaceAll, ArrayList<GroupModel> mGroupModel) {
		mPattern = mUtils.getPattern(mContext, query);
		mReplaceTask = new ReplaceTask(RESULT_REPLACE, replaceAll, mGroupModel);
		mReplaceTask.delegate = this;
		mReplaceTask.execute(query, replace);
	}

	private void startTaskSearch(String query) {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			mData.removeAll(mData);
			mTask = new GrepTask(RESULT_SEARCH);
			mTask.delegate = this;
			mTask.execute(query);
		}
	}

	// Уведомляем о результах поиска/замены
	@Override
	public void onProcessFinish(boolean result, int id) {
		try {
			mSearchTextCallback.onMethodCallback(mData, id, result);
		} catch (ClassCastException exception) {
			// do something
		}
	}

	/**
	 * -----===== ЗАМЕНА =====-----
	 */

	class ReplaceTask extends AsyncTask<String, Integer, Boolean>
	{
		private ProgressDialog mProgressDialog;
		private boolean mCancelled;
		private StringBuffer buffer;

		public AsyncResponse delegate = null;
		private int mResultId;
		private boolean replaceAll;

		private ArrayList<GroupModel> mGroupModel;

		public ReplaceTask(int id, boolean replace, ArrayList<GroupModel> groupModel) {
			this.mResultId = id;
			this.replaceAll = replace;
			this.mGroupModel = groupModel;
		}

		@Override
		protected void onPreExecute() {
			mCancelled = false;
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setTitle(R.string.title_replace_spinner);
			//mProgressDialog.setMessage(mQuery);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setCancelable(true);
			mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, 
				mContext.getString(R.string.action_cancel), 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mCancelled = true;
						cancel(false);
					}
				});
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			boolean res = false;
			int count = 0;
			for (int i=0; i < mGroupModel.size(); i++) {
				if (!mGroupModel.get(i).isSelected() && !replaceAll) {
					continue;
				}

				saveFile(mGroupModel.get(i).getPath(), params[1]);
				res = true;
				publishProgress(count);
				count++;
			}
			return res;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mResultId = 2;
			onPostExecute(false);
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			if (isCancelled()) {
				return;
			}
			mProgressDialog.setMessage(mContext.getString(R.string.msg_progress_replace , progress));
		}
	

		@Override
		protected void onPostExecute(Boolean result) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
			mReplaceTask = null;
			delegate.onProcessFinish(result, mResultId);
		}

		private void saveFile(File path, String replace) {
			buffer = mUtils.replaceFile(path, replace, mPattern);
			if (buffer != null) {
				if (mPrefs.mCreateBackup) {
					// создать резервную копию
					try {
						mUtils.copyFile(path, new File(path.toString() + "~"));
					} catch (IOException e) {}
				}

				mUtils.saveFile(path, buffer.toString());
			}
		}
	}

	/**
	 * -----===== ПОИСК =====-----
	 */
	class GrepTask extends AsyncTask<String, SearchModel, Boolean>
	{
		private ProgressDialog mProgressDialog;
		private int mFileCount=0;
		private int mFoundcount=0;
		private boolean mCancelled;
		private int mResultId;
		private String mQuery;

		public AsyncResponse delegate = null;

		public GrepTask(int id) {
			this.mResultId = id;
		}

		@Override
		protected void onPreExecute() {
			mCancelled = false;
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setTitle(R.string.title_grep_spinner);
			mProgressDialog.setIcon(R.drawable.ic_file_find);
			//mProgressDialog.setMessage(mQuery);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setCancelable(true);
			mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, 
				mContext.getString(R.string.action_cancel), 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mCancelled = true;
						cancel(false);
					}
				});
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			mQuery = params[0];
			return grepRoot(params[0]);
		}


		@Override
		protected void onPostExecute(Boolean result) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
			mTask = null;
			delegate.onProcessFinish(result, mResultId);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mResultId = 2;
			onPostExecute(false);
		}

		@Override
		protected void onProgressUpdate(SearchModel... progress) {
			if (isCancelled()) {
				return;
			}
			mProgressDialog.setMessage(mContext.getString(R.string.msg_progress , mQuery, mFileCount));
			if (progress != null) {
				synchronized (mData) {
					for (SearchModel data : progress) {
						mData.add(data);
					}
				}
			}
		}

		boolean grepRoot(String text) {
			for (CheckedString dir : mPrefs.mDirList) {
				if (dir.checked && !grepDirectory(new File(dir.string))) {
					return false;
				}
			}
			return true;
		}

		boolean grepDirectory(File dir) {
			if (isCancelled()) {
				return false;
			}
			if (dir == null) {
				return false;
			}

			File[] flist = dir.listFiles();

			if (flist != null) {
				for (File f : flist) {
					// Если скрытый и выкл. поиск в скрытых, то дальше
					if (!mPrefs.mSearchHidden && f.getName().startsWith(".")) {
						continue;
					}

					boolean res = false;
					if (f.isDirectory()) {
						res = grepDirectory(f);
					} else {
						boolean extok=false;
						for (CheckedString ext : mPrefs.mExtList) {
							if (ext.checked) {
								if (f.getName().indexOf('.') == -1 && ext.string.equals("*.")) {
									extok = true;
									break;
								} else if (f.getName().toLowerCase().endsWith("." + ext.string.toLowerCase())) {
									extok = true;
									break;
								} if (ext.string.equals("*")) {
									extok = true;
									break;
								}
							} // end extChecked
						}
						if (extok)
							res = grepFile(f);
						else
							continue;
					}

					if (!res) {
						return false;
					}
				}
			}
			return true;
		}


		boolean grepFile(File file) {
			if (isCancelled()) {
				return false;
			}
			if (file == null) {
				return false;
			}

			InputStream is;
			try {
				is = new BufferedInputStream(new FileInputStream(file) , 65536);
				is.mark(65536);

				String encode = mUtils.getDetectedEncoding(is);
				is.reset();

				BufferedReader br=null;
				try {
					if (encode != null) {
						br = new BufferedReader(new InputStreamReader(is , encode) , 8192);

					} else {
						br = new BufferedReader(new InputStreamReader(is) , 8192);
					}

					String group = file.getName();
					String text;
					int line = 0;
					boolean found = false;
					Pattern pattern = mPattern;
					Matcher m = null;
					ArrayList<SearchModel> data = null ;
					mFileCount++;
					while ((text = br.readLine()) != null) {
						line ++;
						if (m == null) {
							m = pattern.matcher(text);
						} else {
							m.reset(text);
						}
						if (m.find()) {
							found = true;

							synchronized (mData) {
								mFoundcount++;
								if (data == null) {
									data = new ArrayList<SearchModel>();
								}
								data.add(new SearchModel(line, group, text, file));

								if (mFoundcount < 10) {
									publishProgress(data.toArray(new SearchModel[0]));
									data = null;
								}
							}
							if (mCancelled) {
								break;
							}
						}
					}
					br.close();
					is.close();
					if (data != null) {
						publishProgress(data.toArray(new SearchModel[0]));
						data = null;
					}
					if (!found) {
						if (mFileCount % 10 == 0) {
							publishProgress((SearchModel[])null);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return true;
		}
	}

}
