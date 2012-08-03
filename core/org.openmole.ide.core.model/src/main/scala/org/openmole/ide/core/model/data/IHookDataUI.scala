/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.data

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.core.model.hook.ICapsuleExecutionHook
import org.openmole.core.model.hook.IHook
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.misc.tools.Counter

trait IHookDataUI {
  def id = Counter.id.getAndIncrement

  def activated: Boolean

  def activated_=(a: Boolean)

  def coreClass: Class[_ <: ICapsuleExecutionHook]

  def coreObject(executionManager: IExecutionManager,
                 moleExecution: IMoleExecution,
                 capsule: ICapsule): Iterable[IHook]

  def buildPanelUI(task: ITaskDataProxyUI): IHookPanelUI
}
