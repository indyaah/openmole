/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.mole

import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.tools.IContextBuffer
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.IGenericTransition
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.mole.IMoleExecution
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashSet

object SubMoleExecution {
  
  def apply(moleExecution: IMoleExecution): SubMoleExecution = {
    val ret = new SubMoleExecution(None, moleExecution)
    moleExecution.register(ret)
    ret
  }
  
  def apply(moleExecution: IMoleExecution, parent: ISubMoleExecution): SubMoleExecution =  {
    val ret = new SubMoleExecution(Some(parent), moleExecution)
    moleExecution.register(ret)
    ret
  }
  
}


class SubMoleExecution(val parent: Option[ISubMoleExecution], val moleExecution: IMoleExecution) extends ISubMoleExecution {

  private var submittedJobs = TreeSet[IMoleJob]()
  private var waiting = List[IJob]()
  private var _nbJobInProgress = 0
  private var _nbJobWaitingInGroup = 0
  private var childs = new HashSet[ISubMoleExecution]
  
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, IContextBuffer]
  val transitionRegistry = new RegistryWithTicket[IGenericTransition, IContextBuffer]

  @transient lazy val internalLock = new Object
  
  parrentApply(_.addChild(this))

  override def isRoot = !parent.isDefined
  
  override def nbJobInProgess = _nbJobInProgress //_nbJobInProgress
  
  override def += (moleJob: IMoleJob) = internalLock.synchronized {
    submittedJobs += moleJob
    incNbJobInProgress(1)
  }
  
  override def -= (moleJob: IMoleJob) = internalLock.synchronized {
    submittedJobs -= moleJob
    decNbJobInProgress(1)
  }
  
  override def addWaiting(job: IJob) = 
    if(internalLock.synchronized {waiting :+= job; checkAllJobsWaitingInGroup}) allWaitingEvent
  
  override def removeAllWaiting: Iterable[IJob]= internalLock.synchronized {
    val ret = waiting
    waiting = List.empty[IJob]
    decNbJobWaitingInGroup(ret.map{_.moleJobs.size}.sum)
    ret
  }
  
  override def cancel = {
    submittedJobs.foreach{_.cancel}
    childs.foreach{_.cancel}
    parrentApply(_.removeChild(this))
  }
  
  override def addChild(submoleExecution: ISubMoleExecution) = internalLock.synchronized {
    childs += submoleExecution
  }

  override def removeChild(submoleExecution: ISubMoleExecution) = internalLock.synchronized {
    childs -= submoleExecution
  }
  
  override def incNbJobInProgress(nb: Int) =  {
    internalLock.synchronized {_nbJobInProgress += nb}
    parrentApply(_.incNbJobInProgress(nb))
  }

  override def decNbJobInProgress(nb: Int) = {
    if(internalLock.synchronized{_nbJobInProgress -= nb; checkAllJobsWaitingInGroup}) allWaitingEvent
    parrentApply(_.decNbJobInProgress(nb))
  }
  
  override def incNbJobWaitingInGroup(nb: Int) = {
    if(internalLock.synchronized {_nbJobWaitingInGroup += nb; checkAllJobsWaitingInGroup}) allWaitingEvent
    parrentApply(_.incNbJobWaitingInGroup(nb))
  }

  override def decNbJobWaitingInGroup(nb: Int) = internalLock.synchronized {
    _nbJobWaitingInGroup -= nb
    parrentApply(_.decNbJobWaitingInGroup(nb))
  }
  
  private def parrentApply(f: ISubMoleExecution => Unit) =
    parent match {
      case None => 
      case Some(p) => f(p)
    }
    
  private def checkAllJobsWaitingInGroup = (nbJobInProgess == _nbJobWaitingInGroup && _nbJobWaitingInGroup > 0)
  
  private def allWaitingEvent = EventDispatcher.objectChanged(this, ISubMoleExecution.AllJobsWaitingInGroup)
  
}
