package ua.tohateam.aGrep;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import ua.tohateam.aGrep.model.*;
import ua.tohateam.aGrep.utils.*;

public class SearchFiles implements AsyncResponse
{	
	private static final int RESULT_SEARCH = 0;
	private static final int RESULT_REPLACE = 1;

	private Context mContext;
	private MyUtils mUtils;
    private Prefs mPrefs;

	private Pattern mPattern;

	private ArrayList<SearchModel> mData;
	private SearchFilesCallback mSearchFilesCallback;

	public static interface SearchFilesCallback
	{
        void onResultCallback(ArrayList<SearchModel> data, int idResult, boolean result);
    }

	public SearchFiles(Context context) {
		this.mContext = context;
		this.mPrefs = Prefs.loadPrefes(mContext);
		this.mUtils = new MyUtils();

		try {
            this.mSearchFilesCallback = ((SearchFilesCallback) context);
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement AdapterCallback.");
        }
	}

	public void initSearchFiles(String query) {
		mData = new ArrayList<SearchModel>();

		if (query != null && query.length() > 0) {
			mPrefs.addRecent(mContext, query);
			mPattern = mUtils.getPattern(mContext, query);

			startTaskSearch(query);
		}
	}

	private void startTaskSearch(String query) {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			mData.removeAll(mData);
			SearchTask mTask = new SearchTask(RESULT_SEARCH);
			mTask.delegate = this;
			mTask.execute(query);
		}
	}

	@Override
	public void onProcessFinish(boolean result, int id) {
		try {
			mSearchFilesCallback.onResultCallback(mData, id, result);
		} catch (ClassCastException exception) {
			// do something
		}
	}

	/******************************************************************/

	class SearchTask extends AsyncTask<String, SearchModel, Boolean>
	{
		private ProgressDialog mProgressDialog;
		private boolean mCancelled;

		public AsyncResponse delegate = null;
		private int mResultId;
		private int mFileCounts = 0;
		private String mQuery;

		public SearchTask(int id) {
			this.mResultId = id;
		}

		@Override
		protected void onPreExecute() {
			mCancelled = false;
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setTitle(R.string.title_grep_spinner);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setCancelable(true);
			mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						mCancelled = true;
						cancel(false);
					}
				});
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			mQuery = params[0];
			return grepRoot();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			onPostExecute(false);
		}

        @Override
        protected void onProgressUpdate(SearchModel... progress) {
            if (isCancelled()) {
                return;
            }
            mProgressDialog.setMessage(mContext.getString(R.string.msg_progress_files, mFileCounts));
            if (progress != null) {
                synchronized (mData) {
                    for (SearchModel data : progress) {
                        mData.add(data);
                    }
                }
            }
        }

		@Override
		protected void onPostExecute(Boolean result) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
			delegate.onProcessFinish(result, mResultId);
		}

		// Получаем список директорий поиска
		boolean grepRoot() {
			boolean result = false;
            for (CheckedString dir : mPrefs.mDirList) {
                if (dir.checked) {
					if(grepDirectory(new File(dir.string)) || mFileCounts>0)
						result = true;
                } else {
					continue;
				}
            }
            return result;
        }

		// Поиск в папке и подпапках
		boolean grepDirectory(File dir) {
            if (isCancelled()) {
                return false;
            }
            if (dir == null) {
                return false;
            }

            File[] flist = dir.listFiles();
			boolean result = false;

            if (flist != null) {
                for (File f : flist) {
					// Если скрытый и выкл. поиск в скрытых, то дальше
					if (!mPrefs.mSearchHidden && f.getName().startsWith(".")) {
						continue;
					}

                    if (f.isDirectory()) {
                        grepDirectory(f);
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
						} // проверка расширения
						if(!extok) {
							continue;
						}

						/******************СРАВНЕНИЕ*******************/
						String group = f.getParent().substring(f.getParent().lastIndexOf("/") + 1);
						String name = f.getName();

						Pattern pattern = mPattern;
						Matcher m = null;
						ArrayList<SearchModel> data = null ;

						if (m == null) {
							m = pattern.matcher(name);
						} else {
							m.reset(name);
						}
						
						if (m.find()) {
							synchronized (mData) {
								if (data == null) {
									data = new ArrayList<SearchModel>();
								}
								data.add(new SearchModel(0, group, name, f));
								publishProgress(data.toArray(new SearchModel[0]));
								data = null;
							}

							if (mCancelled) {
								return false;
							}
							result = true;
							mFileCounts++;
						}
					} // end file
				} // end for
			} // end if
            return result;
        } // end grepDirectory
	}

}
