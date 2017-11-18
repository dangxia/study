package com.github.dangxia.algo

trait Node {
  def left: Node
  def key: Int
  def right: Node
  def prefix = Node.prefix(this)
  def suffix = Node.suffix(this)
  def infix = Node.infix(this)
  def toList = infix
  def min(): Node = {
    if (left != null) left.min() else this
  }
  def max(): Node = {
    if (right != null) right.max() else this
  }
  def insert(key: Int): Node
  def walk(f: (Int ⇒ Int))
  def lookup(key: Int): Node = Node.lookup(this, key)
  def delete(key: Int): Node

  def diameter = Node.diameter(this)
  def height = Node.height(this)

  def print = Node.printTree(this)

  protected def printV = println(key)
}
object Node {
  def prefix(node: Node): List[Int] = {
    node match {
      case null ⇒ Nil
      case _ ⇒ node.key :: prefix(node.left) ::: prefix(node.right)
    }
  }
  def suffix(node: Node): List[Int] = {
    node match {
      case null ⇒ Nil
      case _ ⇒ suffix(node.left) ::: suffix(node.right) ::: List(node.key)
    }
  }
  def infix(node: Node): List[Int] = {
    node match {
      case null ⇒ Nil
      case _ ⇒ infix(node.left) ::: List(node.key) ::: infix(node.right)
    }
  }
  def lookup(node: Node, key: Int): Node = {
    node match {
      case null ⇒ null
      case _ ⇒ {
        if (key == node.key) {
          node
        } else if (key < node.key) {
          lookup(node.left, key)
        } else {
          lookup(node.right, key)
        }
      }
    }
  }
  def height(node: Node): Int = {
    if (node == null) 0
    else {
      (height(node.left) max height(node.right)) + 1
    }
  }

  def diameter(node: Node): Int = {
    if (node == null) return 0;
    val lh = height(node.left)
    val rh = height(node.right)

    val ld = diameter(node.left)
    val lr = diameter(node.right)
    ld max lr max (lh + rh + 1)
  }

  def printTree(node: Node) {
    if (node.right != null) {
      printTree(node.right, true, "")
    }
    node.printV
    if (node.left != null) {
      printTree(node.left, false, "")
    }
  }
  def printTree(node: Node, isRight: Boolean, indent: String) {
    if (node.right != null)
      printTree(node.right, true, indent + (if (isRight) "        " else " |      "))
    print(indent)
    if (isRight) {
      print(" /")
    } else {
      print(" \\")
    }
    print("----- ")
    node.printV
    if (node.left != null)
      printTree(node.left, false, indent + (if (isRight) " |      " else "        "))
  }
}