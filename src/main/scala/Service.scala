import scala.io.Source
import scala.collection.parallel.immutable.{ParMap, ParSeq, ParSet}
import java.io._

import cats.syntax.functor._
import io.circe._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

object Service {

  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
  implicit val decodeModel: Decoder[SourceModel] =
    List[Decoder[SourceModel]](
      Decoder[Click].widen,
      Decoder[Impression].widen
    ).reduceLeft(_ or _)

  implicit val encodeReport: Encoder[PerformanceReport] =
    deriveEncoder

  def parseJson(rawJson: String): Seq[Either[Impression, Click]] = {
    parse(rawJson)
      .getOrElse(Json.Null)
      .as[Seq[SourceModel]] match {
      case Left(_) => Seq()
      case Right(s) =>
        s.map(_ match {
          case click: Click    => Right(click)
          case imp: Impression => Left(imp)
        })

    }

  }

  def parseFiles(
      files: List[String]
  ): ParSeq[Either[Impression, Click]] = {
    // Parsing all files in parallel
    files.par.flatMap { f =>
      val file = Source.fromFile(f)
      val parsed = parseJson(file.getLines().mkString)
      file.close()
      parsed
    }
  }

  def processData(
      data: ParSeq[Either[Impression, Click]]
  ): (List[PerformanceReport], List[RecommendationsReport]) = {
    val clicks: ParSeq[Click] = data
      .filter(_.isRight)
      .map(m => m.right.get)
    val impressions: ParSeq[Impression] =
      data.filter(_.isLeft).map(m => m.left.get)
    val aggregatedData = aggregateWithAdvertiser(impressions, clicks)
    val performanceReportData: List[PerformanceReport] = aggregatedData
    // Removing advertiser data, which is no longer needed here, re-aggregating data due to lower granularity
      .foldLeft(Map[(Int, Option[String]), (Int, Int, Double)]()) {
        case (m, v) =>
          m + ((v._1._1, v._1._2) -> {
            val mv = m.getOrElse((v._1._1, v._1._2), (0, 0, 0d))
            (mv._1 + v._2._1, mv._2 + v._2._2, mv._3 + v._2._3)
          })
      }
      .map { case (k, v) => PerformanceReport(k._1, k._2, v._1, v._2, v._3) }
      .toList
    val recommendationsData = aggregatedData
      .map {
        case (k, v) =>
          (k._1, k._2, k._3, v._3 / v._2)
      }
      .toSeq
      .sortWith(_._4 > _._4)
      .foldLeft(Map[(Int, Option[String]), List[Int]]()) {
        case (m, v) =>
          m + ((v._1, v._2) -> {
            val mv: List[Int] = m.getOrElse((v._1, v._2), List[Int]())
            if (mv.length > 4) mv
            else mv :+ v._3
          })
      }
      .map { case (k, v) => RecommendationsReport(k._1, k._2, v) }
      .toList
    (performanceReportData, recommendationsData)
  }

  def writeResults(
      performanceData: List[PerformanceReport],
      recommendationsData: List[RecommendationsReport],
      destination: String
  ): Unit = {
    val writerResult = new PrintWriter(new File(destination + "/results.json"))
    val writerRecommendation = new PrintWriter(
      new File(destination + "/recommendations.json")
    )

    writerResult.write(performanceData.asJson.toString)
    writerRecommendation.write(recommendationsData.asJson.toString)
    writerResult.close()
    writerRecommendation.close()

  }

  private def aggregateWithAdvertiser(
      impressions: ParSeq[Impression],
      clicks: ParSeq[Click]
  ): Map[(Int, Option[String], Int), (Int, Int, Double)] = {
    // To avoid multiple walk through impressions we will first aggregate them separately,
    // and afterwards enrich them with clicks info
    impressions
      .foldLeft(Map[(Int, Option[String], Int), (Int, Set[String])]()) {
        case (m, v) =>
          m + ((v.appId, v.countryCode, v.advertiserId) -> {
            val mv = m.getOrElse(
              (v.appId, v.countryCode, v.advertiserId),
              (0, Set[String]())
            )
            (mv._1 + 1, mv._2.+(v.id))
          })
      }
      .map { imp =>
        imp._1 -> {
          // Transformation to list needed to avoid theoretical situations where we will have
          // two different ids for same set of app_id/country_code and same performance
          val impressionsPerformance = imp._2._2.toList
            .map(i => clicksDataForImpression(i, clicks))
            .reduce(sumTuples)
          (imp._2._1, impressionsPerformance._1, impressionsPerformance._2)
        }
      }
  }

  def clicksDataForImpression(
      impId: String,
      clicks: ParSeq[Click]
  ): (Int, Double) = {
    clicks
      .filter(_.impressionId == impId)
      .foldLeft(0, 0d)((t, v) => (t._1 + 1, t._2 + v.revenue))
  }

  def sumTuples(
      tup1: (Int, Double),
      tup2: (Int, Double)
  ): (Int, Double) = {
    (tup1._1 + tup2._1, tup1._2 + tup2._2)
  }

}
