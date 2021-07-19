package org.zensearch.formatting

import spray.json._
import org.zensearch.ZenSearchTestBase
import org.zensearch.messages.Messages.FormatResult
import org.zensearch.model.Model.{Criteria, DataEntry, DataType, InternalId, Request, Response, Users}

trait FormatterTestBase extends ZenSearchTestBase {
  val dtype1 = DataType("t")
}

class BasicJsonFormatterTest extends FormatterTestBase {
  
  val formatter = new BasicJsonFormatter()
  
  test("Basic Json formatter should return correctly formatted output") {
    
    val id1 = InternalId(dtype1, 1)
    val id2 = InternalId(dtype1, 2)

    val json1 = """{"_id": 21,"name":"name1"}""".parseJson.asJsObject
    val json2 = """{"_id": 42,"name":"name2"}""".parseJson.asJsObject

    val expected1 = json1.prettyPrint
    val expected2 = json2.prettyPrint

    val req = Request(Criteria(dtype1, "name"), None)
    val data1 = DataEntry(id1, json1)
    val data2 = DataEntry(id2, json2)
    val resp = Response(req, data1 :: data2 :: Nil)
    
    val res: FormatResult = formatter.formatResponse(resp)
    
    assert(res.data.size == 2)
    assert(res.data.head.data == data1)
    assert(res.data.head.text.text == expected1)
    assert(res.data(1).data == data2)
    assert(res.data(1).text.text == expected2)
  }
}
