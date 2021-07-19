package org.zensearch.util

class AppException(message: String, cause: Throwable) extends RuntimeException(message: String, cause: Throwable)

object AppException {
  def apply(message: String) = new AppException(message, null)
  def apply(message: String, cause: Throwable) = new AppException(message, cause)
}
