package edu.umass.cs.automan.adapters.googleads.enums

import com.google.ads.googleads.v2.enums.AgeRangeTypeEnum.AgeRangeType
import com.google.ads.googleads.v2.enums.GenderTypeEnum.GenderType

// Google Codes and Formats:
// https://developers.google.com/adwords/api/docs/appendix/codes-formats
trait Qualification {
  def Value: Int = -1
}

//----------- LANGUAGE ---------------------------------------------------
trait Language extends Qualification

case object ARABIC extends Language {
  override def Value = 1019
}

case object CHINESE_SIMPLIFIED extends Language {
  override def Value = 1017
}

case object ENGLISH extends Language {
  override def Value = 1000
}

case object FRENCH extends Language {
  override def Value = 1002
}

case object GERMAN extends Language {
  override def Value = 1001
}

case object INDONESIAN extends Language {
  override def Value = 1025
}

case object JAPANESE extends Language {
  override def Value = 1005
}

case object ITALIAN extends Language {
  override def Value = 1004
}

case object RUSSIAN extends Language {
  override def Value = 1031
}

case object SPANISH extends Language {
  override def Value = 1003
}
//----------- GENDER -----------------------------------------------------
// returns gender to exclude
trait Gender extends Qualification

case object MALE extends Gender {
  override def Value: Int = GenderType.FEMALE_VALUE
}

case object FEMALE extends Gender {
  override def Value: Int = GenderType.MALE_VALUE
}
//----------- AGE RANGE --------------------------------------------------
trait AgeRange extends Qualification

case object _18_24 extends AgeRange {
  override def Value: Int = AgeRangeType.AGE_RANGE_18_24_VALUE
}

case object _25_34 extends AgeRange {
  override def Value: Int = AgeRangeType.AGE_RANGE_25_34_VALUE
}

case object _35_44 extends AgeRange {
  override def Value: Int = AgeRangeType.AGE_RANGE_35_44_VALUE
}

case object _45_54 extends AgeRange {
  override def Value: Int = AgeRangeType.AGE_RANGE_45_54_VALUE
}

case object _55_64 extends AgeRange {
  override def Value: Int = AgeRangeType.AGE_RANGE_65_UP_VALUE
}

case object _64_UP extends AgeRange {
  override def Value: Int = AgeRangeType.AGE_RANGE_65_UP_VALUE
}
//----------- COUNTRY ----------------------------------------------------
trait Country extends Qualification

case object AUSTRALIA extends Country {
  override def Value = 2036
}

case object CANADA extends Country {
  override def Value = 2124
}

case object CHINA extends Country {
  override def Value = 2156
}

case object FRANCE extends Country {
  override def Value = 2250
}

case object INDIA extends Country {
  override def Value = 2356
}

case object INDONESIA extends Country {
  override def Value = 2360
}

case object MEXICO extends Country {
  override def Value = 2484
}

case object PAKISTAN extends Country {
  override def Value = 2586
}

case object RUSSIA extends Country {
  override def Value = 2643
}

case object SPAIN extends Country {
  override def Value = 2724
}

case object UK extends Country {
  override def Value = 2826
}

case object US extends Country {
  override def Value = 2840
}