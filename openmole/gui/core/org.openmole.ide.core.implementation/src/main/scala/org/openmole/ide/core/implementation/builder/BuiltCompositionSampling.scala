/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
package org.openmole.ide.core.implementation.builder

import org.openmole.ide.core.implementation.sampling._

case class BuiltCompositionSampling(
    builtSamplings: Seq[SamplingProxyUI] = Seq(),
    builtDomains: Seq[DomainProxyUI] = Seq(),
    builtFactors: Seq[IFactorProxyUI] = Seq(),
    builtConnections: Seq[(SamplingOrDomainProxyUI, SamplingOrDomainProxyUI)] = Seq()) extends IBuiltCompositionSampling {

  def copyWithSamplings(sp: SamplingProxyUI) = copy(builtSamplings = builtSamplings :+ sp)

  def copyWithDomains(dp: DomainProxyUI) = copy(builtDomains = builtDomains :+ dp)

  def copyWithFactors(fp: IFactorProxyUI) = copy(builtFactors = builtFactors :+ fp)

  def copyWithConnections(c: (SamplingOrDomainProxyUI, SamplingOrDomainProxyUI)) = copy(builtConnections = builtConnections :+ c)
}
