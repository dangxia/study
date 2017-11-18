package com.github.dangxia.algo.mutable

import com.github.dangxia.algo.mutable.BRNode.Color.Color
import com.github.dangxia.algo.mutable.BRNode.Color

class BRNode(var color: Color, _left: BRNode, _key: Int, _right: BRNode, _parent: BRNode) extends Node(_left, _key, _right, _parent) {
  override def left = super.left.asInstanceOf[BRNode]
  override def right = super.right.asInstanceOf[BRNode]
  override def parent = super.parent.asInstanceOf[BRNode]

  def this(color: Color, key: Int) {
    this(color, null, key, null, null)
  }
  def this(color: Color, key: Int, parent: BRNode) {
    this(color, null, key, null, parent)
  }

  override def insert(key: Int): BRNode = {
    if (key > this.key) {
      if (right == null) {
        right = new BRNode(Color.R, key, this)
        BRNode.blance(this)
      } else {
        right.insert(key)
      }
    } else if (key < this.key) {
      if (left == null) {
        left = new BRNode(Color.R, key, this)
        BRNode.blance(this)
      } else {
        left.insert(key)
      }
    } else {
      var node = this
      while (node.parent != null) {
        node = node.parent
      }
      node
    }
  }

  override def rotateR() = super.rotateR().asInstanceOf[BRNode]
  override def rotateL() = super.rotateL().asInstanceOf[BRNode]

  override def toString(): String = {
    "[" + color + "," + key + "]"
  }

  def v1(): Boolean = left != null && left.color == Color.R && parent != null && parent.color == Color.B
  def v2(): Boolean = right != null && right.color == Color.R && parent != null && parent.color == Color.B

  override def printV {
    println(this.toString())
  }

}

class TestNode(_left: TestNode, key: Int, _right: TestNode, _parent: TestNode) extends Node(_left, key, _right, _parent) {

  val data = Map(1 -> "a", 2 -> "x", 3 -> "b", 4 -> "y", 5 -> "c", 6 -> "z", 7 -> "d")

  def this(key: Int) {
    this(null, key, null, null)
  }
  def this(key: Int, parent: TestNode) {
    this(null, key, null, parent)
  }

  override def insert(key: Int): TestNode = {
    if (key > this.key) {
      if (right == null) {
        right = new TestNode(key, this)
      } else {
        right.insert(key)
      }
    } else if (key < this.key) {
      if (left == null) {
        left = new TestNode(key, this)
      } else {
        left.insert(key)
      }
    }
    this
  }
  override def printV {
    println(data(key))
  }
}

object BRNode {
  object Color extends Enumeration {
    type Color = Value
    val B, R = Value
  }

  def blance(node: BRNode): BRNode = {
    var prev: BRNode = null
    var curr = node

    while (curr != null) {
      if (curr.color != Color.R || (curr.left == null && curr.right == null)) {

      } else if (curr.v1()) {
        if (curr.isLeft) {
          curr.left.color = Color.B
          curr.parent.rotateR()
        } else {
          curr.color = Color.B
          curr.rotateR()
          curr.parent.rotateL()
        }
      } else if (curr.v2()) {
        if (curr.isRight) {
          curr.right.color = Color.B
          curr.parent.rotateL()
        } else {
          curr.color = Color.B
          curr.rotateL()
          curr.parent.rotateR()
        }
      }
      prev = curr
      curr = curr.parent
    }
    prev.color = Color.B
    prev
  }
  def fromList(list: List[Int]): TestNode = {
    list.foldLeft(null.asInstanceOf[TestNode])((curr: TestNode, key: Int) ⇒ {
      curr match {
        case null ⇒ new TestNode(key)
        case _ ⇒ curr.insert(key)
      }
    })
  }

  def fromList2(list: List[Int]): BRNode = {
    list.foldLeft(null.asInstanceOf[BRNode])((curr: BRNode, key: Int) ⇒ {
      curr match {
        case null ⇒ new BRNode(Color.R, key)
        case _ ⇒ {
          val n = curr.insert(key)
          n
        }
      }
    })
  }

  val a = 1
  val b = 3
  val c = 5
  val d = 7
  val x = 2
  val y = 4
  val z = 6

  val list1 = List(z, y, d, x, c, a, b)
  val list2 = List(x, a, y, b, z, c, d)
  val list3 = List(z, x, d, a, y, b, c)
  val list4 = List(x, a, z, y, d, b, c)

  def main(args: Array[String]): Unit = {
    fromList2(list1).print
    fromList2(list2).print
    fromList2(list3).print
    fromList2(list4).print
  }

  //  def main(args: Array[String]): Unit = {
  //    var node1 = fromList(list1);
  //    node1.rotateR().print
  //    fromList(list2).rotateL().print
  //    var node3 = fromList(list3)
  //    node3.left.rotateL().rotateR().print
  //    var node4 = fromList(list4)
  //    node4.right.rotateR().rotateL().print
  //  }
}