package org.openmole.gui.client.core

/*
 * Copyright (C) 23/07/15 // mathieu.leclaire@openmole.org
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

import org.openmole.core.buildinfo.{ MarketIndex, MarketIndexEntry }
import org.openmole.gui.client.core.alert.{ AlertPanel, AbsolutePositioning }
import AbsolutePositioning.CenterPagePosition
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.ext.data.{ Processing, ProcessState }
import org.openmole.gui.misc.js.{ InputFilter }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.shared.Api
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.client.core.files.TreeNodePanel
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._
import org.openmole.gui.client.core.CoreUtils._
import org.openmole.gui.ext.data._
import Waiter._
import autowire._
import rx._
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import bs._

class MarketPanel extends ModalPanel {
  lazy val modalID = "marketPanelID"

  private val marketIndex: Var[Option[MarketIndex]] = Var(None)
  val tagFilter = InputFilter(pHolder = "Filter")
  val selectedEntry: Var[Option[MarketIndexEntry]] = Var(None)
  lazy val downloading: Var[Seq[(MarketIndexEntry, Var[_ <: ProcessState])]] = Var(marketIndex.now.map {
    _.entries.map {
      (_, Var(Processed()))
    }
  }.getOrElse(Seq()))
  val overwriteAlert: Var[Option[MarketIndexEntry]] = Var(None)

  lazy val marketTable = div(
    sheet.paddingTop(20),
    Rx {
      marketIndex().map { mindex ⇒
        for {
          entry ← mindex.entries if tagFilter.exists(entry.tags)
        } yield {
          val isSelected = Some(entry) == selectedEntry()
          Seq(
            div(omsheet.docEntry)(
              div(colMD(3) +++ sheet.paddingTop(7))(
                tags.a(entry.name, cursor := "pointer", ms("whiteBold"), onclick := { () ⇒
                  selectedEntry() = {
                    if (isSelected) None
                    else Some(entry)
                  }
                })
              ),
              div(colMD(2))(downloadButton(entry, () ⇒ {
                exists(manager.current.now.safePath() ++ entry.name, entry)
              })),
              div(colMD(7) +++ sheet.paddingTop(7))(
                entry.tags.map { e ⇒ tags.label(e)(sheet.label_primary +++ omsheet.tableTag) }
              ), tags.div(
                if (isSelected) omsheet.docEntry else emptyMod,
                selectedEntry().map { se ⇒
                  if (isSelected) div(ms("mdRendering"))(sheet.paddingTop(40))(
                    RawFrag(entry.readme.getOrElse(""))
                  )(colspan := 12)
                  else tags.div()
                }
              )
            )
          )
        }.render
      }
    }
  )

  def exists(sp: SafePath, entry: MarketIndexEntry) =
    OMPost[Api].exists(sp).call().foreach { b ⇒
      if (b) overwriteAlert() = Some(entry)
      else download(entry)
    }

  def download(entry: MarketIndexEntry) = {
    val path = manager.current.now.safePath.now ++ entry.name
    downloading() = downloading.now.updatedFirst(_._1 == entry, (entry, Var(Processing())))
    OMPost[Api].getMarketEntry(entry, path).call().foreach { d ⇒
      downloading() = downloading.now.updatedFirst(_._1 == entry, (entry, Var(Processed())))
      downloading.now.headOption.foreach(_ ⇒ close)
      TreeNodePanel.refreshAndDraw
    }
  }

  def downloadButton(entry: MarketIndexEntry, todo: () ⇒ Unit = () ⇒ {}) =
    downloading.now.find {
      _._1 == entry
    }.map {
      case (e, state: Var[ProcessState]) ⇒
        state.withTransferWaiter { _ ⇒
          if (selectedEntry.now == Some(e)) bs.glyphButton(" Download", btn_warning, glyph_download_alt, todo) else tags.div()
        }
    }.getOrElse(tags.div())

  def onOpen() = marketIndex.now match {
    case None ⇒ OMPost[Api].marketIndex.call().foreach { m ⇒
      marketIndex() = Some(m)
    }
    case _ ⇒
  }

  def onClose() = {}

  def deleteFile(sp: SafePath, marketIndexEntry: MarketIndexEntry) =
    OMPost[Api].deleteFile(sp, ServerFileSytemContext.project).call().foreach { d ⇒
      download(marketIndexEntry)
    }

  val dialog = bs.modalDialog(
    modalID,
    headerDialog(
      tags.span(tags.b("Market place"))
    ),
    bodyDialog({
      Rx {
        overwriteAlert() match {
          case Some(e: MarketIndexEntry) ⇒
            AlertPanel.string(
              e.name + " already exists. Overwrite ? ",
              () ⇒ {
                overwriteAlert() = None
                deleteFile(manager.current().safePath() ++ e.name, e)
              }, () ⇒ {
                overwriteAlert() = None
              }, CenterPagePosition
            )
            tags.div
          case _ ⇒
        }
      }
      tags.div(
        tagFilter.tag,
        marketTable
      )
    }),
    footerDialog(closeButton)
  )

}
