package ua.tohateam.aGrep;

	// you may separate this or combined to caller class.
	public interface AsyncResponse {
		void onProcessFinish(boolean result, int id);
	}
