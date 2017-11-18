package com.github.dangxia.algo.mutable

object Exercise1_4 {
  def main(args: Array[String]): Unit = {
    var nodes = List(6, 2, 7, 1, 4, 8, 3, 5, 12, 10, 13, 9, 11)
    var node = Node.fromList(nodes)
    node.print
    println(node.diameter)

    node.lookup(8).rotateL()
    node.print

    nodes = List(13, 7, 2, 8, 1, 6, 10, 4, 9, 11, 3, 5, 12)
    Node.fromList(nodes).print
    println(Node.fromList(nodes).diameter)
  }
}