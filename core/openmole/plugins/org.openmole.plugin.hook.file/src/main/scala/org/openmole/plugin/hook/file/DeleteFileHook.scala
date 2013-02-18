/*
 * Copyright (C) 2011 Leclaire Mathieu  <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.hook.file

import java.io.File
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.model.job._
import org.openmole.core.implementation.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.data._
import org.openmole.misc.exception._

class DeleteFileHook(toDelete: Prototype[File]*) extends Hook {

  override def process(context: Context) = {
    toDelete.foreach {
      prototype ⇒
        context.option(prototype) match {
          case Some(file) ⇒ file.recursiveDelete
          case None ⇒ throw new UserBadDataError("No variable " + prototype + " found.")
        }
    }
  }

  override def inputs = DataSet(toDelete)

}