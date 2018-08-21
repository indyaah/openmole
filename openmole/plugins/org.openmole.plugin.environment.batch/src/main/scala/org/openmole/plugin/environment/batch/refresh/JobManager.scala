/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.environment.batch.refresh

import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

import org.openmole.core.tools.service.Retry.retry
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.jobservice.BatchJobControl
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.thread._

import scala.tools.reflect.WrappedProperties

object JobManager extends JavaLogger { self ⇒
  import Log._

  def killPriority = 10

  def messagePriority(message: DispatchedMessage) =
    message match {
      case msg: Upload    ⇒ 10
      case msg: Submit    ⇒ 50
      case msg: Refresh   ⇒ 5
      case msg: GetResult ⇒ 50
      case msg: Error     ⇒ 100 // This is very quick to process
      case _              ⇒ 1
    }

  object DispatcherActor {
    def receive(dispatched: DispatchedMessage)(implicit services: BatchEnvironment.Services) =
      dispatched match {
        case msg: Upload      ⇒ if (!msg.job.job.finished) UploadActor.receive(msg) else self ! Kill(msg.job, None, None)
        case msg: Submit      ⇒ if (!msg.job.job.finished) SubmitActor.receive(msg) else self ! Kill(msg.job, None, Some(msg.serializedJob))
        case msg: Refresh     ⇒ if (!msg.job.job.finished) RefreshActor.receive(msg) else self ! Kill(msg.job, Some(msg.batchJob), Some(msg.serializedJob))
        case msg: GetResult   ⇒ if (!msg.job.job.finished) GetResultActor.receive(msg) else self ! Kill(msg.job, Some(msg.batchJob), Some(msg.serializedJob))
        case msg: RetryAction ⇒ RetryActionActor.receive(msg)
        case msg: Error       ⇒ ErrorActor.receive(msg)
      }
  }

  def dispatch(msg: DispatchedMessage)(implicit services: BatchEnvironment.Services) = services.threadProvider.submit(messagePriority(msg)) { () ⇒ DispatcherActor.receive(msg) }

  def !(msg: JobMessage)(implicit services: BatchEnvironment.Services): Unit = msg match {
    case msg: Upload      ⇒ dispatch(msg)
    case msg: Submit      ⇒ dispatch(msg)
    case msg: Refresh     ⇒ dispatch(msg)
    case msg: GetResult   ⇒ dispatch(msg)
    case msg: RetryAction ⇒ dispatch(msg)
    case msg: Error       ⇒ dispatch(msg)

    case Manage(job) ⇒
      self ! Upload(job)

    case Delay(msg, delay) ⇒
      services.threadProvider.scheduler.schedule((self ! msg): Runnable, delay.millis, TimeUnit.MILLISECONDS)

    case Uploaded(job, sj) ⇒ self ! Submit(job, sj)

    case Submitted(job, sj, bj) ⇒
      self ! Delay(Refresh(job, sj, bj, job.environment.updateInterval.minUpdateInterval), job.environment.updateInterval.minUpdateInterval)

    case Kill(job, batchJob, serializedJob) ⇒
      BatchEnvironment.finishedExecutionJob(job.environment, job)
      tryKillAndClean(job, batchJob, serializedJob)
      job.state = ExecutionState.KILLED
      if (job.job.finished) BatchEnvironment.finishedJob(job.environment, job.job)
      if (!job.job.finished && BatchEnvironment.numberOfExecutionJobs(job.environment, job.job) == 0) job.environment.submit(job.job)

    case Resubmit(job, storage, batchJob, serializedJob) ⇒
      tryKillAndClean(job, Some(batchJob), Some(serializedJob))
      job.state = ExecutionState.READY
      dispatch(Upload(job))

    case MoleJobError(mj, j, e) ⇒
      val er = Environment.MoleJobExceptionRaised(j, e, WARNING, mj)
      j.environment.error(er)
      services.eventDispatcher.trigger(j.environment: Environment, er)
      logger.log(FINE, "Error during job execution, it will be resubmitted.", e)

  }

  def tryKillAndClean(job: BatchExecutionJob, bj: Option[BatchJobControl], sj: Option[SerializedJob])(implicit services: BatchEnvironment.Services) = {
    def killBatchJob(bj: BatchJobControl)(implicit services: BatchEnvironment.Services) = AccessControl.withPermit(bj.accessControl) {
      retry(services.preference(BatchEnvironment.killJobRetry))(bj.delete)
    }

    def cleanSerializedJob(sj: SerializedJob)(implicit services: BatchEnvironment.Services) = AccessControl.withPermit(sj.storage.accessControl) {
      retry(services.preference(BatchEnvironment.cleanJobRetry))(sj.clean(sj.path))
    }

    try bj.foreach(killBatchJob) catch {
      case e: Throwable ⇒ self ! Error(job, e, None)
    }

    try sj.foreach(cleanSerializedJob) catch {
      case e: Throwable ⇒ self ! Error(job, e, None)
    }
  }

}
