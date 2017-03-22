package ua.tohateam.aGrep.utils;

import android.content.*;
import android.text.*;
import android.text.style.*;
import android.webkit.*;
import java.io.*;
import java.util.regex.*;
import org.mozilla.universalchardet.*;

public class MyUtils
{

	public MyUtils() {

	}

	public String escapeMetaChar(String pattern) {
        final String metachar = ".^${}[]*+?|()\\";
        StringBuilder newpat = new StringBuilder();

        for (int i=0;i < pattern.length();i++) {
            char c = pattern.charAt(i);
            if (metachar.indexOf(c) >= 0) {
                newpat.append('\\');
            }
            newpat.append(c);
        }
        return newpat.toString();
    } // end escapeMetaChar

    public String convertOrPattern(String pattern) {
        if (pattern.contains(" ")) {
            return "(" + pattern.replace(" ", "|") + ")";
        } else {
            return pattern;
        }
    } // end convertOrPattern

	// get encoding file, use juniversalchardet-1.0.3.jar
	public String getDetectedEncoding(InputStream is) throws IOException {
		UniversalDetector detector=new UniversalDetector(null);
		byte[] buf=new byte[4096];
		int nread;
		while ((nread = is.read(buf)) > 0 && !detector.isDone()) {
			detector.handleData(buf, 0, nread);
		}
		detector.dataEnd();
		return detector.getDetectedCharset();
	}

	public SpannableString highlightKeyword(CharSequence text, Pattern p, int fgcolor, int bgcolor) {
        SpannableString ss = new SpannableString(text);

        int start = 0;
        int end;
        Matcher m = p.matcher(text);
        while (m.find(start)) {
            start = m.start();
            end = m.end();

            BackgroundColorSpan bgspan = new BackgroundColorSpan(bgcolor);
            ss.setSpan(bgspan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ForegroundColorSpan fgspan = new ForegroundColorSpan(fgcolor);
            ss.setSpan(fgspan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            start = end;
        }
        return ss;
    }

	/********************************************************
	 * Проверка на существование файла или папки
	 ********************************************************/
	public boolean onFileExists(String path) {
		File f = new File(path);
		if (f.exists()) {
			return true;
		}
		return false;
	}

	/********************************************************
	 * Save file to SD
	 ********************************************************/
	public boolean saveFile(File path, String text) {
		try {
//			File myFile = new File(path);
			if (path.exists()) {
				path.delete();
			}

			path.createNewFile();
			FileOutputStream fOut = new FileOutputStream(path);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append(text);
			//myOutWriter.write(text);
			myOutWriter.close();
			fOut.flush();
			fOut.close();
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	// Копирование файла
	public void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	// Замена текста в файле
	public StringBuffer replaceFile(File file, String replace, Pattern mPattern) {
		StringBuffer buffer = null;
		
		if (file == null) {
			return null;
		}

		InputStream is;
		try {
			is = new BufferedInputStream(new FileInputStream(file) , 65536);
			is.mark(65536);

			String encode = getDetectedEncoding(is);
			is.reset();

			BufferedReader br=null;
			try {
				if (encode != null) {
					br = new BufferedReader(new InputStreamReader(is , encode) , 8192);
				} else {
					br = new BufferedReader(new InputStreamReader(is) , 8192);
				}

				String text;
				int line = 0;
				Pattern pattern = mPattern;
				Matcher m = null;
				buffer = new StringBuffer();

				while ((text = br.readLine()) != null) {
					line ++;
					if (m == null) {
						m = pattern.matcher(text);
					} else {
						m.reset(text);
					}

					while (m.find()) {
						m.appendReplacement(buffer, replace);
					}
					m.appendTail(buffer);
					buffer.append("\n");
				}
				br.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return buffer;
	}

	public Pattern getPattern(Context context, String patternText) {
		Pattern mPattern = null;
		Prefs mPrefs = Prefs.loadPrefes(context);
		if (!mPrefs.mRegularExrpression) {
			patternText = escapeMetaChar(patternText);
			patternText = convertOrPattern(patternText);
		}

		if (mPrefs.mIgnoreCase) {
			mPattern = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
		} else {
			mPattern = Pattern.compile(patternText);
		}

		if (mPrefs.mWordOnly) {
			mPattern = Pattern.compile("\\b" + patternText + "\\b");
		}
		return mPattern;
	}
	
	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}

	public boolean isEmpty(String text) {
       text = text.trim();

        if(text==null||text.equals("")) {
            return true;
        }
		return false;
	}
}
