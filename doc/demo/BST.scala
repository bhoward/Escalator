package demo

sealed trait Tree[+T] {
  def toList: List[T]
}

case object Tip extends Tree[Nothing] {
  def toList = Nil
}

case class Node[T](left: Tree[T], value: T, right: Tree[T]) extends Tree[T] {
  def toList = left.toList ::: value :: right.toList
}

object Tree {
  def insert[T <% Ordered[T]](x: T, tree: Tree[T]): Tree[T] = tree match {
    case Tip => Node(Tip, x, Tip)
    case Node(left, value, right) if x < value =>
      Node(insert(x, left), value, right)
    case Node(left, value, right) =>
      Node(left, value, insert(x, right))
  }

  def search[T <% Ordered[T]](x: T, tree: Tree[T]): Boolean = tree match {
    case Tip => false
    case Node(left, value, right) if x < value => search(x, left)
    case Node(left, value, right) if x > value => search(x, right)
    case _ => true
  }

  def tsort[T <% Ordered[T]](values: List[T]) = values.foldRight(Tip: Tree[T])(insert).toList
}
