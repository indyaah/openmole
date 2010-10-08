/*
 *  Copyright (C) 2010 reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.plugin.domain.interval;

import java.util.Iterator;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.domain.Domain;
import org.openmole.core.model.job.IContext;

/**
 *
 * @author reuillon
 */
public class InfiniteCounterDomain extends Domain<Long> {

    final Iterable<Long> counter;

    public InfiniteCounterDomain() {
        this(0L, 1L);
    }

    public InfiniteCounterDomain(final Long start, final Long step) {
        counter = new Iterable<Long>() {

            @Override
            public Iterator<Long> iterator() {
                return new Iterator<Long>() {

                    Long value = start;

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Long next() {
                        Long ret = value;
                        value += step;
                        return ret;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                };
            }
        };
    }

    @Override
    public Iterator<? extends Long> iterator(IContext global, IContext context) throws UserBadDataError, InternalProcessingError {
        return counter.iterator();
    }
}
