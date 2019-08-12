package edu.umass.cs.automan.adapters.googleads.ads

import java.util.UUID

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v2.enums.AdGroupAdStatusEnum.AdGroupAdStatus
import com.google.ads.googleads.v2.common.ExpandedTextAdInfo
import com.google.ads.googleads.v2.enums.PolicyApprovalStatusEnum.PolicyApprovalStatus
import com.google.ads.googleads.v2.resources.{AdGroupAd, Ad => GoogleAd}
import com.google.ads.googleads.v2.services.{AdGroupAdOperation, GoogleAdsRow, SearchGoogleAdsRequest}
import com.google.ads.googleads.v2.utils.ResourceNames
import com.google.common.collect.ImmutableList
import com.google.protobuf.StringValue

import scala.collection.JavaConverters._
import scala.io.StdIn.readLine
import edu.umass.cs.automan.core.logging._

class Ad(googleAdsClient: GoogleAdsClient, accountId: Long, adGroupId: Long, title: String, subtitle: String, description: String, url: String, qID: UUID) {
  if(title.length > 30)
    do {println("Ad title too long. Enter a new ad title (30 chars or less): ")} while (readLine().length() > 30)
  if(subtitle.length > 30)
    do {println("Ad subtitle too long. Enter a new ad subtitle (30 chars or less): ")} while (readLine().length() > 30)
  if(title.length > 90)
    do {println("Ad description too long. Enter a new ad description (90 chars or less): ")} while (readLine().length() > 90)

  private val client = googleAdsClient.getLatestVersion.createAdGroupAdServiceClient()

  // Creates the information within the ad i.e. title, subtitle, description
  private val expandedTextAdInfo = ExpandedTextAdInfo.newBuilder
    .setHeadlinePart1(StringValue.of(title))
    .setHeadlinePart2(StringValue.of(subtitle))
    .setDescription(StringValue.of(description))
    .build()

  // Wraps the info in an Ads.Ad object
  private val ad = GoogleAd.newBuilder
    .setExpandedTextAd(expandedTextAdInfo)
    .addFinalUrls(StringValue.of(url))
    .build()

  // Builds the final ad representation within ad group
  private val adGroupAd = AdGroupAd.newBuilder
    .setAdGroup(StringValue.of(ResourceNames.adGroup(accountId, adGroupId)))
    .setAd(ad)
    .build()

  private val operations = AdGroupAdOperation.newBuilder.setCreate(adGroupAd).build()
  private val response = client.mutateAdGroupAds(accountId.toString, ImmutableList.of(operations))
  private val id = client.getAdGroupAd(response.getResults(0).getResourceName).getAd.getId.getValue

  DebugLog("Created ad " + title + " to ad group with ID " + adGroupId, LogLevelInfo(), LogType.ADAPTER, qID)

  //Saves resource name
  private val adResourceName = response.getResults(0).getResourceName
  client.shutdown()

  /**
    * Delete the Google ad associated with this class
    */
  def delete(): Unit = {
    val sc = googleAdsClient.getLatestVersion.createAdGroupAdServiceClient()

    val rmOp = AdGroupAdOperation.newBuilder.setRemove(adResourceName).build()
    sc.mutateAdGroupAds(accountId.toString, ImmutableList.of(rmOp))

    sc.shutdown()
    DebugLog("Deleted ad " + title, LogLevelInfo(), LogType.ADAPTER, qID)
  }

  /**
    * Gets whether this ad is enabled. If false, ad is paused or removed
    * @return True if ad is enabled, false if paused or removed
    */
  def is_enabled: Boolean = {
    query("ad_group_ad.status","ad_group_ad").head.getAdGroupAd.getStatus == AdGroupAdStatus.ENABLED
  }

  /**
    * Checks whether ad has passed review and is approved to run. If false, ad will not run
    * @return True if ad is approved, false if awaiting approval or rejected
    */
  def is_approved: Boolean = {
    query("ad_group_ad.policy_summary","ad_group_ad").head.getAdGroupAd.getPolicySummary.getApprovalStatus == PolicyApprovalStatus.APPROVED
  }

  def query(field: String, resource: String): Iterable[GoogleAdsRow] = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT $field FROM $resource WHERE ad_group_ad.ad.id = $id AND customer.id = $accountId"

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountId.toString)
      .setPageSize(1)
      .setQuery(searchQuery)
      .build()
    val response: Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    gasc.shutdown()
    response
  }
}

