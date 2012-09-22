package com.spidersaas.parser.link

import org.scalatest.FlatSpec
import com.spidersaas.core.Link

class SimpleLinkNormalizerTest extends FlatSpec {

  val normalizer = new SimpleLinkNormalizer

  "SimpleLinkNormalizer" should "correctly handle url list" in {
    io.Source.fromURL(classOf[SimpleLinkNormalizerTest].
      getResource("/url-normalize-list")).
      getLines().
      filter(!_.trim().isEmpty).
      grouped(3).foreach {
      case List(current, raw, formatted) =>
        expect(formatted) {
          normalizer.normalize(new Link(current), raw)
        }
      case unknown => println("Wrong chunk: %1$s".format(unknown))
    }
  }

}
