package edu.depauw

//<Factorial>
object Factorial {
  def apply(n: Int): BigInt = {
    if (n <= 1)
      1
    else
      Factorial(n-1) * n
  }
}
//</Factorial>

object test {
  //<main>
  def main(args: Array[String]) {
    println(Factorial(5))
  }
  //</main>
}
