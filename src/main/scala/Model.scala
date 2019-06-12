sealed trait Model

sealed trait SourceModel extends Model {
  def impressionId: String
}

case class Impression(
    id: String,
    appId: Int,
    countryCode: Option[String],
    advertiserId: Int
) extends SourceModel {
  def impressionId: String = id
}

case class Click(
    impressionId: String,
    revenue: Double
) extends SourceModel

sealed trait ResultModel

case class PerformanceReport(
    appId: Int,
    countryCode: Option[String],
    impressions: Int,
    clicks: Int,
    revenue: Double
) extends ResultModel

case class RecommendationsReport(
  appId: Int,
  countryCode: Option[String],
  recommendedAdvertiserIds: List[Int]
) extends ResultModel

