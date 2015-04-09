/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.task.scala

import java.io.File

import org.openmole.core.console.ScalaREPL
import org.openmole.core.exception.InternalProcessingError
import scala.util.Try

trait ScalaCompilation {
  def usedClasses: Seq[Class[_]]
  def libraries: Seq[File]

  def compile(code: String) = Try {
    val interpreter = new ScalaREPL(usedClasses ++ Seq(getClass), libraries)

    val evaluated =
      try interpreter.eval(code)
      catch {
        case e: Throwable ⇒ throw new InternalProcessingError(e, s"Error compiling $code")
      }

    if (evaluated == null) throw new InternalProcessingError(
      s"""The return value of the script was null:
         |$code""".stripMargin
    )

    evaluated
  }
}
