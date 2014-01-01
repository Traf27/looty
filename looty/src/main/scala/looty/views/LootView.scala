package looty
package views

import org.scalajs.jquery.JQueryStatic
import scala.scalajs.js
import looty.model.{ComputedItemProps, PoeCacher, ComputedItem}
import looty.poeapi.PoeTypes.{AnyItem, Leagues}
import looty.model.parsers.ItemParser


//////////////////////////////////////////////////////////////
// Copyright (c) 2013 Ben Jackman, Jeff Gomberg
// All Rights Reserved
// please contact ben@jackman.biz or jeff@cgtanalytics.com
// for licensing inquiries
// Created by bjackman @ 12/9/13 11:17 PM
//////////////////////////////////////////////////////////////


class LootView() extends View {
  val jq          : JQueryStatic           = global.jQuery.asInstanceOf[JQueryStatic]
  var grid        : js.Dynamic             = null
  var displayItems: js.Array[ComputedItem] = null
  var allItems    : js.Array[ComputedItem] = null


  def start() {
    val items = new js.Array[ComputedItem]()
    val pc = new PoeCacher()

    val fut = for {
      tabInfos <- pc.getStashInfo(Leagues.Standard)
      tabs <- pc.getAllStashTabs(Leagues.Standard)
      invs <- pc.getAllInventories(Leagues.Standard)
    } yield {

      //TODO Remove take1
      for {
        (tab, i) <- tabs.zipWithIndex //.take(1)
        item <- tab.items
      } {
        ItemParser.parseItem(item).foreach { ci =>
          ci.location = tabInfos(i).n
          items.push(ci)
        }
      }

      //TODO Remove take1
      for {
        (char, inv) <- invs //.take(1)
        item <- inv.items
      } {

        ItemParser.parseItem(item).foreach { ci =>
          ci.location = char
          items.push(ci)
        }
      }

      this.displayItems = items
      this.allItems = items

      setHtml()
    }

    fut.log()
  }

  def stop() {}

  private def setHtml() {
    val el = jq("#content")
    el.empty()
    el.append("""<div id="controls"></div>""")
    el.append("""<div id="grid"></div>""")
    el.append("""<div id="itemdetail" style="z-index:100;color:white;background-color:black;opacity:.9;display:none;position:fixed;left:50px;top:100px">SAMPLE DATA<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a</div>""")
    appendControls()
    appendGrid(displayItems)

  }

  private def appendControls() {
    val jq: JQueryStatic = global.jQuery.asInstanceOf[JQueryStatic]
    val el = jq("#controls")
    el.empty()

    val pc = new PoeCacher()

    //Buttons for stashed
    for {
      stis <- pc.Net.getStisAndStore(Leagues.Standard)
      sti <- stis.toList
    } {
      val button = jq(s"""<button style="color: white;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;background-color:${sti.colour.toRgb}">${sti.n}</button>""")
      el.append(button)
      button.on("click", (a: js.Any) => {
        //Set the grid to only have this tabs items in it and refresh this tab
        pc.Net.getStashTabAndStore(Leagues.Standard, sti.i.toInt).foreach(st => showItems(st.items, sti.n))
      })
    }

    //Buttons for characters
    for {
      chars <- pc.Net.getCharsAndStore
      char <- chars.toList
    } {
      val button = jq(s"""<button>${char.name}</button>""")
      el.append(button)
      button.on("click", (a: js.Any) => {
        //Set the grid to only have this tabs items in it and refresh this tab
        pc.Net.getInvAndStore(char.name).foreach(inv => showItems(inv.items, char.name))
      })
    }

  }

  private def showItems(xs: js.Array[AnyItem], location: String) {
    val pc = new PoeCacher()
    val items = new js.Array[ComputedItem]()
    for {
      item <- xs
      ci <- ItemParser.parseItem(item)
    } {
      ci.location = location
      items.push(ci)
    }
    displayItems = items
    grid.setData(displayItems)
    grid.invalidate()
    grid.render()

  }

  private def appendGrid(items0: js.Array[ComputedItem]) {
    displayItems = items0
    def makeColumn(name: String, tooltip: String)(f: ComputedItem => js.Any) = {
      val o = newObject
      o.id = name
      o.name = name
      o.field = name
      o.toolTip = tooltip
      o.sortable = true
      o.getter = f
      o
    }
    val columns = js.Array[js.Dynamic]()

    ComputedItemProps.all.foreach {
      p =>
        columns.push(makeColumn(p.shortName, p.description)(p.getJs))
    }

    val options = {
      val o = newObject
      o.enableCellNavigation = true
      o.enableColumnReorder = false
      o.multiColumnSort = true
      o.dataItemColumnValueExtractor = (item: ComputedItem, column: js.Dynamic) => {
        column.getter(item.asInstanceOf[js.Any])
      }
      o
    }

    grid = js.Dynamic.newInstance(global.Slick.Grid)("#grid", displayItems, columns, options)
    addSort()
    addMouseover()
    def resize() {
      jq("#grid").css("height", global.window.innerHeight - 120)
      grid.resizeCanvas()
    }
    jq(global.window).resize(() => resize())

    resize()

  }

  private def addSort() {
    grid.onSort.subscribe((e: js.Dynamic, args: js.Dynamic) => {
      val cols = args.sortCols.asInstanceOf[js.Array[js.Dynamic]]

      displayItems.sort {
        (a: ComputedItem, b: ComputedItem) =>
          var i = 0
          var ret = 0.0
          while (i < cols.length && ret == 0) {
            val col = cols(i)
            val sign = if (cols(i).sortAsc.asInstanceOf[js.Boolean]) 1 else -1
            val a1: js.Dynamic = col.sortCol.getter(a.asInstanceOf[js.Any])
            val b1: js.Dynamic = col.sortCol.getter(b.asInstanceOf[js.Any])

            val res = a1 - b1
            if (js.isNaN(res)) {
              if (a1.toString > b1.toString) {
                ret = sign
              } else if (b1.toString > a1.toString) {
                ret = -sign
              }

            } else {
              ret = sign * res
            }

            i += 1
          }
          ret: js.Number
      }

      grid.invalidate()
      grid.render()
    })
  }

  def showItemDetail(
    top: Option[js.Number],
    right: Option[js.Number],
    bottom: Option[js.Number],
    left: Option[js.Number],
    item: ComputedItem) {

    val d = jq("#itemdetail")
    d.show()
    d.css("top", top.getOrElse("".toJs))
    d.css("right", right.getOrElse("".toJs))
    d.css("bottom", bottom.getOrElse("".toJs))
    d.css("left", left.getOrElse("".toJs))
    val color = item.item.getFrameType.color
    def requirements = {
      val xs = for {
        rs <- item.item.requirements.toOption.toList
        r <- rs.toList
        n <- r.name.toOption.toList
        vs <- r.values.toList
      } yield {
        s"$n ${vs(0).toString}"
      }
      xs.oIf(_.nonEmpty, _ => xs.mkString("Requires ", ", ", ""), _ => "")
    }
    def properties = {
      (for {
        props <- item.item.properties.toOption.toList
        prop <- props.toList
      } yield {
        val vs = for {
          v <- prop.values.toList
        } yield {
          v(0)
        }
        prop.name + " " + vs.mkString("")
      }).mkString("<br>")
    }
    def flavorText = {
      item.item.flavourText.toOption.map(_.toList.mkString("<br>")).getOrElse("")
    }
    val sections = List(
      item.item.name.toString,
      item.item.typeLine.toString,
      properties,
      requirements,
      item.item.descrText.toOption.map(_.toString).getOrElse(""),
      item.item.implicitModList.mkString("<br>"),
      item.item.explicitModList.mkString("<br>"),
      item.item.secDescrText.toOption.map(_.toString).getOrElse(""),
      flavorText
    ).filter(_.nonEmpty)
    val h = s"""
    <div style="color:$color;padding:5px">
    ${sections.mkString("<hr>")}
    </div>
    """

    d.html(h)

    console.log(item)
  }

  private def addMouseover() {
    grid.onMouseEnter.subscribe((e: js.Dynamic, args: js.Any) => {
      val row = grid.getCellFromEvent(e).row
      if (row.nullSafe.isDefined) {
        val (top, bottom) = if (e.clientY / global.window.innerHeight < .5) {
          Some(e.clientY.toJsNum + 10) -> None
        } else {
          None -> Some(global.window.innerHeight - e.clientY + 10)
        }

        val (right, left) = if (e.clientX / global.window.innerWidth < .5) {
          None -> Some(e.clientX.toJsNum + 10)
        } else {
          Some(global.window.innerWidth - e.clientX + 10) -> None
        }

        val item = grid.getDataItem(row).asInstanceOf[ComputedItem]
        showItemDetail(top, right, bottom, left, item)
      }
    })
    grid.onMouseLeave.subscribe((e: js.Dynamic, args: js.Any) => {
      jq("#itemdetail").hide()
    })
  }
}


