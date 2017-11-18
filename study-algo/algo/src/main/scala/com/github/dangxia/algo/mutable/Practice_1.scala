package com.github.dangxia.algo.mutable

import Node._

object Practice_1 {
  def main(args: Array[String]): Unit = {
    val list = List(4, 3, 1, 2, 8, 7, 16, 10, 9, 14);
    var node = fromList(list);
    println("prefix:", node.prefix)
    println("suffix:", node.suffix)
    println("infix:", node.infix)
    println("toList:", node.toList)
    node.walk { _ + 2 }
    println("walk:+2 ", node.toList)
    node.walk { _ - 2 }
    println("walk:-2 ", node.toList)
    println("sort:", sort(list))
    println("lookup 10 then prefix ", node.lookup(10).prefix)
    println("min:", node.min, "max:", node.max)
    testNext(node.min.key, node)
    testNext(8, node)
    testPrev(node.max.key, node)
    testPrev(8, node)

    node.print
    
    list.foldLeft(node)((node, x) => {
      val r = node.delete(x)
      println("delete " + x)
      if(r != null) r.print
//      println("delete " + x, if (r == null) Nil else r.toList)
      r
    })

  }

  def testNext(start: Int, node: Node): Unit = {
    var item: Node = node.lookup(start);
    var list: List[Int] = Nil
    while (item != null) {
      list = item.key :: list
      item = item.next()
    }
    println("next start from", start, list.reverse)
  }

  def testPrev(start: Int, node: Node): Unit = {
    var item: Node = node.lookup(start);
    var list: List[Int] = Nil
    while (item != null) {
      list = item.key :: list
      item = item.prev
    }
    println("prev start from", start, list.reverse)
  }
}