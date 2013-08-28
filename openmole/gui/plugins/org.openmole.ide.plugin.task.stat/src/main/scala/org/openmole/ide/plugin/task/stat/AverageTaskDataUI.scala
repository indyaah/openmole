/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.stat

import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.plugin.task.stat.AverageTask
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class AverageTaskDataUI(val name: String = "",
                        val sequence: List[(PrototypeDataProxyUI, PrototypeDataProxyUI)] = List.empty) extends StatDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = AverageTask(name)(plugins)

    sequence foreach { s ⇒
      gtBuilder addSequence (s._1.dataUI.coreObject.get.asInstanceOf[Prototype[Array[Double]]],
        s._2.dataUI.coreObject.get.asInstanceOf[Prototype[Double]])
    }

    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[AverageTask]

  def fatImagePath = "img/average_fat.png"

  override def imagePath = "img/average.png"

  def buildPanelUI = new AverageTaskPanelUI(this)
}
