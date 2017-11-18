package com.github.dangxia.addTwoNumbers;

public class Solution {
	public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
		int total = l1.val + l2.val;
		int reminder = total % 10;
		int inc = total != reminder ? 1 : 0;
		ListNode root = new ListNode(reminder);

		ListNode parent = root;
		while (l1.next != null && l2.next != null) {
			l1 = l1.next;
			l2 = l2.next;

			total = l1.val + l2.val + inc;
			reminder = total % 10;
			inc = total != reminder ? 1 : 0;

			ListNode curr = new ListNode(reminder);
			parent.next = curr;
			parent = curr;
		}

		if (l1.next != null || l2.next != null) {
			ListNode tail = l1.next != null ? l1.next : l2.next;
			while (inc > 0 && tail != null) {
				total = tail.val + inc;
				reminder = total % 10;
				inc = total != reminder ? 1 : 0;

				ListNode curr = new ListNode(reminder);
				parent.next = curr;
				parent = curr;

				tail = tail.next;
			}

			if (inc == 0) {
				parent.next = tail;
			}
		}

		if (inc > 0) {
			parent.next = new ListNode(inc);
		}

		return root;
	}
}