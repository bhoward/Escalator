/**
 * Here is a thoroughly artifical set of definitions, just to test the range of code fragments
 */
object test {
  /* Should include this preceding comment through the closing paren */
  val c =
  (
    1,
    2,
    3
  )
  
  // Should include this preceding comment through the closing brace
  var b = {
    1; 2; 3
  }
  
  /**
   * Should include this Scaladoc through the closing square bracket
   */
  def a = b.asInstanceOf[
    Any
  ]
}