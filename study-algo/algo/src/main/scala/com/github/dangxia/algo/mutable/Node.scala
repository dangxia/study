package com.github.dangxia.algo.mutable

class Node(private var _left: Node, var key: Int, private var _right: Node, private var _parent: Node) extends com.github.dangxia.algo.Node {

  def left: Node = _left
  def right: Node = _right
  def parent: Node = _parent

  def left_=(aleft: Node) { _left = aleft }
  def right_=(aright: Node) { _right = aright }
  def parent_=(aparent: Node) { _parent = aparent }

  protected def this() {
    this(null, null.asInstanceOf[Int], null, null)
  }

  def this(key: Int) {
    this(null, key, null, null)
  }

  def this(key: Int, parent: Node) {
    this(null, key, null, parent)
  }

  override def min(): Node = super.min().asInstanceOf[Node]
  override def max(): Node = super.max().asInstanceOf[Node]
  override def lookup(key: Int): Node = super.lookup(key).asInstanceOf[Node]

  def isRoot = parent == null
  def isLeft = if (isRoot) throw new RuntimeException else parent.left eq this
  def isRight = if (isRoot) throw new RuntimeException else parent.right eq this

  def rotate(): Node = {
    if (right != null) {
      rotateL()
    } else if (left != null) {
      rotateR()
    }
    this
  }

  def rotateL(): Node = {
    val p = parent
    val y = right
    val a = left
    val b = y.left
    val c = y.right
    replace(y)
    setChildren(a, b)
    y.setChildren(this, c)
    if (p == null) y else p
  }

  def rotateR(): Node = {
    val p = parent
    val x = left
    val a = x.left
    val b = x.right
    val c = right

    replace(x)
    setChildren(b, c)
    x.setChildren(a, this)
    if (p == null) x else p
  }

  def setLeft(left: Node) {
    this.left = left
    if (left != null) {
      left.parent = this
    }
  }

  def setRight(right: Node) {
    this.right = right
    if (right != null) {
      right.parent = this
    }
  }

  def replace(node: Node) {
    if (parent == null) {
      if (node != null) node.parent = null
    } else if (isLeft) {
      parent.setLeft(node)
    } else {
      parent.setRight(node)
    }
    parent = null
  }

  def setChildren(left: Node, right: Node) {
    setLeft(left)
    setRight(right)
  }

  def insert(key: Int): Node = {
    if (key > this.key) {
      if (right == null) {
        right = new Node(key, this)
      } else {
        right.insert(key)
      }
    } else if (key < this.key) {
      if (left == null) {
        left = new Node(key, this)
      } else {
        left.insert(key)
      }
    }
    this
  }

  def walk(f: (Int ⇒ Int)) {
    Node.walk(this, f)
  }
  def delete(key: Int): Node = {
    if (key == this.key) {
      delete()
    } else {
      if (key > this.key) {
        if (right != null) right.delete(key)
      } else if (key < this.key) {
        if (left != null) left.delete(key)
      }
      this
    }
  }

  def delete(): Node = {
    if (isRoot) {
      if (left == null && right == null) {
        null
      } else if (left != null) {
        rotateR()
        delete()
        parent
      } else {
        rotateL()
        delete()
        parent
      }
    } else {
      if (left == null && right == null) {
        if (isLeft) parent.left = null else parent.right = null
      } else if (left == null) {
        right.parent = parent
        if (isLeft) parent.left = right else parent.right = right
      } else if (right == null) {
        left.parent = parent
        if (isLeft) parent.left = left else parent.right = left
      } else {
        val rmin = right.min
        this.key = rmin.key
        right.delete(this.key)
      }
      this
    }
  }

  def next(): Node = {
    if (right != null) {
      right.min()
    } else {
      var p = parent
      var l = this
      while (p != null && (p.right eq l)) {
        l = p
        p = p.parent
      }
      p
    }
  }
  def prev(): Node = {
    if (left != null) {
      left.max()
    } else {
      var p = parent
      var l = this
      while (p != null && (p.left eq l)) {
        l = p
        p = p.parent
      }
      p
    }
  }

  override def toString(): String = {
    String.valueOf(key)
  }
}

object Node {
  def walk(node: Node, f: (Int ⇒ Int)) {
    node match {
      case null ⇒
      case _ ⇒ {
        node.key = f(node.key)
        walk(node.left, f)
        walk(node.right, f)
      }
    }
  }

  def fromList(list: List[Int]): Node = {
    list.foldLeft(null.asInstanceOf[Node])((curr: Node, key: Int) ⇒ {
      curr match {
        case null ⇒ new Node(key)
        case _ ⇒ curr.insert(key)
      }
    })
  }
  def sort(list: List[Int]) = fromList(list).toList

}