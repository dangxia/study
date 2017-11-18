package com.github.dangxia.lengthOfLongestSubstring;

import java.util.HashMap;
import java.util.Map;

public class Solution {
	public int lengthOfLongestSubstring(String s) {
		int max = 0;
		Map<Character, Integer> map = new HashMap<Character, Integer>();
		int start = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			Integer old = map.put(c, i);
			if (old != null) {
				int len = i - start;
				if (max < len) {
					max = len;
				}

				if (old - start + 1 >= i - old) {
					//rebuild
					map.clear();
					for (int j = old + 1; j <= i; j++) {
						map.put(s.charAt(j), j);
					}
				} else {
					//remove
					for (int j = start; j < old; j++) {
						map.remove(s.charAt(j));
					}
				}
				start = old + 1;
			}
		}

		int len = map.size();
		if (max < len) {
			max = len;
		}
		return max;
	}

}
