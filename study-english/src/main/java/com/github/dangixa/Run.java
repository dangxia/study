package com.github.dangixa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import com.github.dangixa.WordInfoUtils.WordInfo;

public class Run {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("need file path");
			System.exit(0);
		}
		String path = args[0];

		File file = new File(path);
		if (!file.isFile()) {
			System.exit(0);
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Enter Word\n");
		while (true) {
			String word = br.readLine();
			deal(path, word);
		}

	}

	public static void deal(String path, String word) {
		WordInfo info = WordInfoUtils.getWordInfo(word);
		File file = new File(path);
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
			writer.write(info.get());
		} catch (Exception e) {
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
