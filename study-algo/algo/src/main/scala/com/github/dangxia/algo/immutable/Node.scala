package com.github.dangxia.algo.immutable

class Node(val left: Node, val key: Int, val right: Node) extends com.github.dangxia.algo.Node {
  def this(key: Int) {
    this(null, key, null);
  }
  def insert(key: Int): Node = Node.insert(this, key);
  def walk(f: (Int ⇒ Int)) = Node.walk(this, f)
  def delete(key: Int): Node = null
}

object Node {
  def insert(node: Node, key: Int): Node = {
    node match {
      case null ⇒ new Node(key)
      case _ ⇒ {
        if (key == node.key) {
          node
        } else if (key > node.key) {
          new Node(node.left, node.key, insert(node.right, key))
        } else {
          new Node(insert(node.left, key), node.key, node.right)
        }
      }
    }
  }

  def walk(node: Node, f: Int ⇒ Int): Node = {
    node match {
      case null ⇒ null
      case _ ⇒ new Node(walk(node.left, f), f(node.key), walk(node.right, f))
    }
  }
  
  def walkCond(node: Node, f: Int ⇒ Int, minV: Int, maxV: Int): Node = {
    node match {
      case null ⇒ null
      case x ⇒
        x.key match {
          case `minV` ⇒ new Node(null, f(node.key), walkCond(node.right, f, minV, maxV))
          case `maxV` ⇒ new Node(walkCond(node.left, f, minV, maxV), f(node.key), null)
          case _ ⇒ {
            if (node.key < minV) {
              walkCond(node.right, f, minV, maxV)
            } else if (node.key > maxV) {
              walkCond(node.left, f, minV, maxV)
            } else {
              new Node(walkCond(node.left, f, minV, maxV), f(node.key), walkCond(node.right, f, minV, maxV))
            }
          }
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