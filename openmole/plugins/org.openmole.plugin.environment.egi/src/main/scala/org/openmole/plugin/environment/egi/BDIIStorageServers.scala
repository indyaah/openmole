/*
 * Copyright (C) 10/06/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.egi

import java.util.concurrent.TimeUnit

import org.openmole.core.exception.InternalProcessingError
import org.openmole.tool.file._
import org.openmole.core.tools.service.{ Scaling, Random }
import org.openmole.core.batch.environment.BatchEnvironment
import fr.iscpif.gridscale.egi.{ GlobusAuthentication, BDII }
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.batch.storage.StorageService
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.workspace._
import org.openmole.tool.hash.Hash
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._
import concurrent.stm._
import java.io.File
import Random._
import Scaling._

object BDIIStorageServers extends Logger

import BDIIStorageServers.Log._

trait BDIIStorageServers extends BatchEnvironment { env ⇒
  type SS = EGIStorageService

  def bdiiServer: BDII
  def voName: String
  def proxyCreator: () ⇒ GlobusAuthentication.Proxy

  @transient lazy val storages = {
    def timeout = Workspace.preferenceAsDuration(EGIEnvironment.FetchResourcesTimeOut)
    val webdavStorages = bdiiServer.queryWebDAVLocations(voName, timeout)
    if (!webdavStorages.isEmpty) {
      logger.fine("Use webdav storages:" + webdavStorages.mkString(","))
      webdavStorages.map { s ⇒ EGIWebDAVStorageService(s, env, proxyCreator) }
    }
    else {
      val srmStorages = bdiiServer.querySRMLocations(voName, timeout)
      logger.fine("Use srm storages:" + srmStorages.mkString(","))
      srmStorages.map { s ⇒ EGISRMStorageService(s, env, proxyCreator) }
    }
  }

  def selectAStorage(usedFileHashes: Iterable[(File, Hash)]): (StorageService, AccessToken) = {
    import EGIEnvironment._

    val sss = storages
    if (sss.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")

    val nonEmpty = sss.filter(!_.usageControl.isEmpty)
    lazy val sizes = usedFileHashes.map { case (f, _) ⇒ f -> f.size }.toMap
    lazy val totalFileSize = sizes.values.sum

    lazy val onStorage = ReplicaCatalog.withSession(ReplicaCatalog.inCatalog(_))
    lazy val maxTime = nonEmpty.map(_.usageControl.time).max
    lazy val minTime = nonEmpty.map(_.usageControl.time).min

    lazy val waitTimes = nonEmpty.map(_.usageControl.waitTime)
    lazy val waitMaxTime = waitTimes.max
    lazy val waitMinTime = waitTimes.min

    def rate(ss: EGIStorageService) = {
      val sizesOnStorage = usedFileHashes.filter { case (_, h) ⇒ onStorage.getOrElse(ss.id, Set.empty).contains(h.toString) }.map { case (f, _) ⇒ sizes(f) }
      val sizeOnStorage = sizesOnStorage.sum

      val sizeFactor = if (totalFileSize != 0) sizeOnStorage.toDouble / totalFileSize else 0.0

      val time = ss.usageControl.time
      val timeFactor = if (minTime == maxTime) 1.0 else 1.0 - time.normalize(minTime, maxTime)

      val waitTime = ss.usageControl.waitTime
      val waitTimeFactor = if (waitMinTime == waitMaxTime) 1.0 else 1.0 - waitTime.normalize(minTime, maxTime)

      math.pow(
        Workspace.preferenceAsDouble(StorageSizeFactor) * sizeFactor +
          Workspace.preferenceAsDouble(StorageTimeFactor) * timeFactor +
          Workspace.preferenceAsDouble(StorageWaitTimeFactor) * waitTimeFactor +
          Workspace.preferenceAsDouble(StorageSuccessRateFactor) * ss.usageControl.successRate,
        Workspace.preferenceAsDouble(StorageFitnessPower))
    }

    select(sss.toList, rate)
  }

  def clean = ReplicaCatalog.withSession { implicit c ⇒
    val cleaningThreadPool = fixedThreadPool(Workspace.preferenceAsInt(EGIEnvironment.EnvironmentCleaningThreads))
    storages.foreach {
      s ⇒
        background {
          s.withToken { implicit t ⇒ s.clean }
        }(cleaningThreadPool)
    }
    cleaningThreadPool.shutdown()
    cleaningThreadPool.awaitTermination(Long.MaxValue, TimeUnit.DAYS)
  }

}
