/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core.model.transition;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.job.IContext;

/**
 *
 * A condition returning true or false.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface ICondition {

    /**
     * 
     * Evaluate the value of this condition in a given context.
     * 
     * @param context the context in which the condition is evaluated
     * @return the value of this condition
     * @throws InternalProcessingError  if something goes wrong because of a system failure
     * @throws UserBadDataError         if something goes wrong because it is missconfigured
     */
    boolean evaluate(IContext global, IContext context) throws UserBadDataError, InternalProcessingError;
}
