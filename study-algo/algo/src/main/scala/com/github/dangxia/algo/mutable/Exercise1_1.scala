package com.github.dangxia.algo.mutable

import scala.collection.mutable.ListBuffer

object Exercise1_1 {
  val infixList = List(4, 2, 1, 5, 3, 6)
  val prefixList = List(1, 2, 4, 3, 5, 6)

  val node = generate(prefixList, infixList, null, null.asInstanceOf[Boolean]);

  def main(args: Array[String]): Unit = {
    println(node.infix)
    println(node.prefix)
    println(node.suffix)
  }

  def generate(prefixList: List[Int], infixList: List[Int], parent: Node, isLeft: Boolean): Node = {
    if (parent == null && prefixList.isEmpty) {
      null
    } else {
      if (!prefixList.isEmpty) {
        var n: Node = new Node(prefixList.head);
        n.parent = parent
        if (!prefixList.tail.isEmpty) {
          var (infixLeft, infixRight) = split(infixList, prefixList.head)
          var (prefixLeft, prefixRight) = split(prefixList.tail, infixLeft)

          generate(prefixLeft, infixLeft, n, true)
          generate(prefixRight, infixRight, n, false)
        }
        if (parent != null)
          if (isLeft) parent.left = n else parent.right = n
        n
      } else {
        null
      }
    }

  }
  def split(prefixList: List[Int], infixLeft: List[Int]): (List[Int], List[Int]) = {
    val left = infixLeft.toSet
    val pl = new ListBuffer[Int]
    val pr = new ListBuffer[Int]

    for (x ← prefixList) {
      if (left.contains(x)) {
        pl += x
      } else {
        pr += x
      }
    }
    (pl.toList, pr.toList)
  }

  def split(infixList: List[Int], k: Int): (List[Int], List[Int]) = {
    val h = new ListBuffer[Int]
    var remain = infixList
    var end = false
    while (!remain.isEmpty && !end) {
      if (remain.head == k) {
        end = true
      } else {
        h += remain.head
      }
      remain = remain.tail
    }
    (h.toList, remain)
  }
}