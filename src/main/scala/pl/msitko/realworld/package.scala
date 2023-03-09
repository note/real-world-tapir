package pl.msitko

import cats.data.ValidatedNec

package object realworld:
  type Validated[T] = ValidatedNec[(String, String), T]
