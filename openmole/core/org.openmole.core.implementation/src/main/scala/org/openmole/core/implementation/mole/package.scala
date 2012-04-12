/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.implementation

import org.openmole.core.model.IPuzzleFirst
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole

package object mole {
  implicit def capsuleToSlotConverter(capsule: ICapsule) = capsule.defaultInputSlot
  implicit def puzzleToMoleConverter(puzzle: IPuzzleFirst) = new Mole(puzzle.first.capsule)
  implicit def moleToMoleExecutionConverter(mole: IMole) = new MoleExecution(mole)
  implicit def puzzleToMoleExecutionConverter(puzzle: IPuzzleFirst) = new MoleExecution(puzzle)
}
