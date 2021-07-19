package org.zensearch.formatting

import org.zensearch.ZenSearchTestBase
import org.zensearch.main.ConfigModel.CrossLink
import org.zensearch.messages.Messages.FormatResult
import org.zensearch.model.Model._
import spray.json._

import System.{lineSeparator => LS}

trait TextFormatterTestBase extends ZenSearchTestBase {
  def fixLineEnds(str: String) =
      str.replaceAll("\r\n","<CR>")
         .replaceAll("\r","<CR>")
         .replaceAll("\n","<CR>")
         .replaceAll(LS,"<CR>")
}

class TextFormatterTest extends TextFormatterTestBase {
  
  val formatter = new TextFormatter()
  
  test("Text formatter should return correctly formatted output") {
    
    val id1 = InternalId(Users, 1)
    val id2 = InternalId(Users, 2)
    val id3 = InternalId(Organizations, 3)
    val id4 = InternalId(Tickets, 4)

    val json1 = """{"_id": 21,"name":"name1","organization_id":33}""".parseJson.asJsObject
    val json2 = """{"_id": 42,"name":"name2"}""".parseJson.asJsObject
    val json3 = """{"_id": 33,"name":"org3"}""".parseJson.asJsObject
    val json4 = """{"_id": 55,"subject":"some_subject","submitter_id":42}""".parseJson.asJsObject

    val expected1 =
      s"""
         |${Users.name} - name = (empty)
         |_id                 21
         |name                "name1"
         |organization_id     33
         |organization_name   "org3"
         |""".stripMargin
    val expected2 =
      s"""
         |${Users.name} - name = (empty)
         |_id                 42
         |name                "name2"
         |submitted tickets:
         |  _id = 55:
         |    subject         "some_subject"
         |""".stripMargin
    
    val req = Request(Criteria(Users, "name"), None)
    val data1 = DataEntry(id1, json1)
    val data2 = DataEntry(id2, json2)
    
    val data3 = DataEntry(id3, json3)
    val data4 = DataEntry(id4, json4)

    val crossResp1 = Response(req, data3 :: Nil)
    
    val link1Json = """{
                      |        "type_name": "organization",
                      |        "ref_field": "organization_id",
                      |        "display_name": "organization_name",
                      |        "linked_field": "_id",
                      |        "display_from_linked": ["name"],
                      |        "display_in_linked": ["_id", "name"],
                      |        "back_link_display_name": "users",
                      |        "back_link_display": ["name",""]
                      |      }""".stripMargin
    val link1: CrossLink = link1Json.parseJson.convertTo[CrossLink]
    val link2Json = """{
                      |        "type_name": "user",
                      |        "ref_field": "submitter_id",
                      |        "display_name": "submitter_name",
                      |        "linked_field": "_id",
                      |        "display_from_linked": ["name"],
                      |        "back_link_display_name": "submitted tickets",
                      |        "back_link_display": ["_id", "subject"]
                      |      }""".stripMargin
    val link2: CrossLink = link2Json.parseJson.convertTo[CrossLink].flip("ticket")
    val crossResp2 = Response(req, data4 :: Nil)
    val crossLinked: List[(DataEntry, Response, CrossLink)] = (data1, crossResp1, link1) :: (data2, crossResp2, link2) :: Nil
    
    val resp = Response(req, data1 :: data2 :: Nil, crossLinked)
    
    val res: FormatResult = formatter.formatResponse(resp)
    
    assert(res.data.size == 2)
    assert(res.data.head.data == data1)
    assert(fixLineEnds(res.data.head.text.text) == fixLineEnds(expected1))
    assert(res.data(1).data == data2)
    assert(fixLineEnds(res.data(1).text.text) == fixLineEnds(expected2))
  }
}
