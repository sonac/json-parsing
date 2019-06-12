import org.scalatest.WordSpec
import java.io.File
import scala.io.Source

class ServiceSpec extends WordSpec {

  "Json Parsing Service" when {

    val files: List[String] =
      List(
        getClass.getResource("clicks.json").getPath(),
        getClass.getResource("impressions.json").getPath()
      )

    "parsing data from stringified json" should {
      "return proper collection of typed case classes" in {
        val rawJson: String =
          """[
          {
            "impression_id": "43bd7feb-3fea-40b4-a140-d01a35ec1f73",
            "revenue": 2.4794577548980876
          },
          {
            "app_id": 30,
            "advertiser_id": 17,
            "country_code": null,
            "id": "5deacf2d-833a-4549-a398-20a0abeec0bc"
          }]"""
        val res = Service.parseJson(rawJson)
        assert(
          res.head.right.get ==
            Click("43bd7feb-3fea-40b4-a140-d01a35ec1f73", 2.4794577548980876d)
        )
        assert(
          res.tail.head.left.get == Impression(
            "5deacf2d-833a-4549-a398-20a0abeec0bc",
            30,
            None,
            17
          )
        )
      }
    }

    "traversing files submitted as arguments" should {
      "return properly parsed them into collection of Either" in {
        val res = Service.parseFiles(files)
        assert(res.head.right.get == Click("97dd2a0f-6d42-4c63-8cd6-5270c19f20d6",12.091225600111517d))
        assert(res.filter(_.isLeft).head.left.get ==
          Impression("97dd2a0f-6d42-4c63-8cd6-5270c19f20d6",32,Some("UK"),8))
      }
    }

    "processing data" should {
      "return properly parsed them into collection of case classes" in {
        val parsedData = Service.parseFiles(files)
        val res = Service.processData(parsedData)
        assert(res._1.head == PerformanceReport(30,Some("DE"),1,2,4.58964392598506))
        assert(res._2.tail.head == RecommendationsReport(32,Some("UK"),List(8, 17)))
      }
    }

  }

}
