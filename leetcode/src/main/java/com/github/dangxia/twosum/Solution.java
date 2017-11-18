package com.github.dangxia.twosum;

import java.util.HashMap;
import java.util.Map;

public class Solution {
	public int[] twoSum(int[] nums, int target) {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (int i = 0; i < nums.length; i++) {
			int v = nums[i];
			Integer index1 = map.get(v);
			if (index1 != null) {
				return new int[]{index1, i};
			}
			map.put(target - v, i);
		}
		return new int[]{};
	}
}