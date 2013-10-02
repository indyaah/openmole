/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.serializer

import com.thoughtworks.xstream.XStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.core.serializer.structure.PluginClassAndFiles
import java.util.UUID
import org.openmole.core.serializer.converter._
import org.openmole.core.serializer.structure.FileInfo
import com.ice.tar.TarOutputStream
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import scala.collection.immutable.TreeMap
import com.ice.tar.TarInputStream
import org.openmole.misc.tools.service.LockUtils._
import java.util.concurrent.locks.{ ReentrantReadWriteLock, ReadWriteLock }
import collection.mutable.ListBuffer
import org.openmole.core.serializer.file.FileSerialisation

object SerializerService extends Logger with FileSerialisation {

  private val lock = new ReentrantReadWriteLock
  private val xStreamOperations = ListBuffer.empty[(XStream ⇒ _)]

  private val xstream = new XStream
  private val content = "content.xml"

  private trait Initialized extends Factory {
    override def initialize(t: T) = lock.read {
      for {
        xs ← t.xStreams
        op ← xStreamOperations
      } op(xs)
      t
    }
  }

  private val serializerWithPathHashInjectionFactory = new Factory with Initialized {
    type T = SerializerWithPathHashInjection
    def make = new SerializerWithPathHashInjection
  }

  private val serializerWithFileAndPluginListingFactory = new Factory with Initialized {
    type T = SerializerWithFileAndPluginListing
    def make = new SerializerWithFileAndPluginListing
  }

  private val deserializerWithFileInjectionFromFileFactory = new Factory with Initialized {
    type T = DeserializerWithFileInjectionFromFile
    def make = new DeserializerWithFileInjectionFromFile
  }

  private val deserializerWithFileInjectionFromPathHashFactory = new Factory with Initialized {
    type T = DeserializerWithFileInjectionFromPathHash
    def make = new DeserializerWithFileInjectionFromPathHash
  }

  private def xStreams =
    xstream ::
      serializerWithPathHashInjectionFactory.instantiated.flatMap(_.xStreams) :::
      serializerWithFileAndPluginListingFactory.instantiated.flatMap(_.xStreams) :::
      deserializerWithFileInjectionFromFileFactory.instantiated.flatMap(_.xStreams) :::
      deserializerWithFileInjectionFromPathHashFactory.instantiated.flatMap(_.xStreams)

  def register(op: XStream ⇒ Unit) = lock.write {
    xStreamOperations += op
    xStreams.foreach(op)
  }

  def deserialize[T](file: File): T = lock.read {
    val is = new FileInputStream(file)
    try deserialize(is)
    finally is.close
  }

  def deserialize[T](is: InputStream): T = lock.read(xstream.fromXML(is).asInstanceOf[T])

  def deserializeAndExtractFiles[T](file: File, extractDir: File = Workspace.tmpDir): T = {
    val tis = new TarInputStream(file.bufferedInputStream)
    try deserializeAndExtractFiles(tis, extractDir)
    finally tis.close
  }

  def deserializeAndExtractFiles[T](tis: TarInputStream, extractDir: File): T = lock.read {
    val archiveExtractDir = extractDir.newDir("archive")
    tis.extractDirArchiveWithRelativePath(archiveExtractDir)
    val fileReplacement = deserialiseFileReplacements(archiveExtractDir, extractDir)
    val contentFile = new File(archiveExtractDir, content)
    val obj = deserializeReplaceFiles[T](contentFile, fileReplacement)
    contentFile.delete
    archiveExtractDir.delete
    obj
  }

  def serializeAndArchiveFiles(obj: Any, f: File): Unit = {
    val os = new TarOutputStream(f.bufferedOutputStream)
    try serializeAndArchiveFiles(obj, os)
    finally os.close
  }

  def serializeAndArchiveFiles(obj: Any, tos: TarOutputStream): Unit = lock.read {
    val objSerial = Workspace.newFile
    val serializationResult = serializeGetPluginsAndFiles(obj, objSerial)
    tos.addFile(objSerial, content)
    objSerial.delete
    serialiseFiles(serializationResult.files, tos)
  }

  def serializeFilePathAsHashGetFiles(obj: Any, file: File): Map[File, FileInfo] = lock.read {
    val os = file.bufferedOutputStream
    try serializeFilePathAsHashGetFiles(obj, os)
    finally os.close
  }

  def serializeFilePathAsHashGetFiles(obj: Any, os: OutputStream): Map[File, FileInfo] =
    lock.read(serializerWithPathHashInjectionFactory.exec(_.toXML(obj.asInstanceOf[AnyRef], os)))

  def serializeGetPluginsAndFiles(obj: Any, file: File): PluginClassAndFiles = lock.read {
    val os = file.bufferedOutputStream
    try serializeGetPluginsAndFiles(obj, os)
    finally os.close
  }

  def serializeGetPluginsAndFiles(obj: Any, os: OutputStream): PluginClassAndFiles =
    lock.read(serializerWithFileAndPluginListingFactory.exec {
      serializer ⇒
        val (files, plugins) = serializer.toXMLAndListPluginFiles(obj.asInstanceOf[AnyRef], os)
        new PluginClassAndFiles(files, plugins)
    })

  def deserializeReplaceFiles[T](file: File, files: PartialFunction[File, File]): T = lock.read {
    val is = file.bufferedInputStream
    try deserializeReplaceFiles[T](is, files)
    finally is.close
  }

  def deserializeReplaceFiles[T](is: InputStream, files: PartialFunction[File, File]): T =
    lock.read(deserializerWithFileInjectionFromFileFactory.exec {
      serializer ⇒
        serializer.files = files
        serializer.fromXML[T](is)
    })

  def serialize(obj: Any) = lock.read(xstream.toXML(obj))

  def serialize(obj: Any, os: OutputStream) = lock.read(xstream.toXML(obj, os))

  def serialize(obj: Any, file: File): Unit = lock.read {
    val os = file.bufferedOutputStream
    try serialize(obj, os)
    finally os.close
  }

  def deserializeReplacePathHash[T](file: File, files: PartialFunction[FileInfo, File]): T = lock.read {
    val is = file.bufferedInputStream
    try deserializeReplacePathHash[T](is, files)
    finally is.close
  }

  def deserializeReplacePathHash[T](is: InputStream, files: PartialFunction[FileInfo, File]) =
    lock.read(deserializerWithFileInjectionFromPathHashFactory.exec {
      deserializer ⇒
        deserializer.files = files
        deserializer.fromXML[T](is)
    })

}
