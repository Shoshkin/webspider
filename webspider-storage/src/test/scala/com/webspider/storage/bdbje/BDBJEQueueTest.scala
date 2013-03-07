package com.webspider.storage.bdbje

import org.specs2.mutable.{BeforeAfter, Specification}
import com.sleepycat.bind.tuple.TupleBinding
import java.util.UUID
import com.webspider.core.Link
import persist.LinkSerializer
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.webspider.storage.LinkQueue

/**
 * User: Eugene Dzhurinsky
 * Date: 2/21/13
 */
@RunWith(classOf[JUnitRunner])
class BDBJEQueueTest extends Specification with BDBJEQueue with BDBJEInitAndClose with TestFolderHelper {

  sequential

  val linkKeySerializer: TupleBinding[UUID] = LinkSerializer.keySerializer
  val linkUrlSerializer: TupleBinding[Link.URLT] = LinkSerializer.linkUrlSerializer

  lazy val bdbAround = new BeforeAfter {

    import collection.JavaConversions._

    def after: Any = {
      println("after 1")
      close {
        envmt => envmt.getDatabaseNames.foreach(envmt.removeDatabase(null, _))
      }
      qSize.set(0)
      println("after 2")
    }

    def before: Any = {
      println("before 1")
      dbPath.mkdirs()
      init()
      println("before 2")
    }
  }


  "BDBJEQueue" should {

    "update records count on database push" in bdbAround {
      val uuid = UUID.randomUUID()
      val parent = UUID.randomUUID()
      val url = "http://123.com"
      val link = Link(url, uuid)
      push(link, parent)
      queueSize() must equalTo(1)
    }

    "update record parent and keep single link" in bdbAround {
      val link = Link("http://123.com", UUID.randomUUID())
      val linkCopy = Link("http://123.com", UUID.randomUUID())
      push(link, UUID.randomUUID()) must equalTo(LinkQueue.Ok)
      push(linkCopy, UUID.randomUUID()) must equalTo(LinkQueue.Ok)
      urlDatabase.count() must equalTo(1)
      queueSize() must equalTo(1)
      mainDatabase.count() must equalTo(1)
      relationDatabase.count() must equalTo(2)
    }
  }

}