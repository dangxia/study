package com.github.dangixa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class WordInfoUtils {

	public static class WordInfo {
		public String word;
		public String soundmark;
		public String paraphrase;

		public String get() {
			StringBuffer sb = new StringBuffer();
			sb.append("+ ");
			sb.append("[");
			sb.append(word);
			sb.append(" /");
			sb.append(soundmark);
			sb.append("/](#v) ");
			sb.append(paraphrase);
			sb.append("\n");
			return sb.toString();
		}
	}

	public static String getHtml(String word) {
		try {
			return _getHtml(word);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String _getHtml(String word) throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = null;
		CloseableHttpResponse response = null;
		String html = null;
		try {
			HttpGet httpget = new HttpGet("http://dict.youdao.com/search?q=" + word + "&keyfrom=dict.index");
			httpclient = HttpClients.createDefault();
			response = httpclient.execute(httpget);
			html = EntityUtils.toString(response.getEntity());
		} finally {
			if (response != null) {
				response.close();
			}
			if (httpclient != null) {
				httpclient.close();
			}
		}
		return html;
	}

	public static WordInfo getWordInfo(String word) {
		String html = getHtml(word);

		WordInfo info = new WordInfo();
		info.word = word;

		int from = html.indexOf("class=\"phonetic\">") + "class=\"phonetic\">".length() + 1;
		int to = html.indexOf('<', from);
		info.soundmark = html.substring(from, to - 1);

		from = html.indexOf("class=\"trans-container\">", to) + "class=\"trans-container\">".length();
		to = html.indexOf("</ul>", from);
		info.paraphrase = getParaphrase(html.substring(from, to));

		return info;
	}

	private static final Pattern p = Pattern.compile("<li>(.*?)</li>", Pattern.MULTILINE);

	private static String getParaphrase(String str) {
		List<String> strs = new ArrayList<String>();
		str = str.trim();
		Matcher m = p.matcher(str);
		while (m.find()) {
			String item = m.group(1).trim();
			if (item.length() > 0) {
				strs.add(item);
			}

		}
		if (strs.size() > 0) {
			StringBuffer sb = new StringBuffer();
			for (String item : strs) {
				if (sb.length() > 0) {
					sb.append("; ");
				}
				sb.append(item);
			}
			return sb.toString();
		}
		return "";

	}


}
