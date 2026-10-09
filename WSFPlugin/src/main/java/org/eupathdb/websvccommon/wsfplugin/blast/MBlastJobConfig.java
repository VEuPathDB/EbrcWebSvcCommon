package org.eupathdb.websvccommon.wsfplugin.blast;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.veupathdb.lib.blast.BlastTool;
import org.veupathdb.lib.blast.field.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MBlastJobConfig {
  // Static strings for cases where Jackson might not name the json property
  // correctly on its own.
  public static final String
    JSON_KEY_MAX_HSPS          = "maxHSPs"
  , JSON_KEY_EXPECT_VALUE      = "eValue"
  , JSON_KEY_LOWERCASE_MASKING = "lcaseMasking"
  ;

  private BlastTool tool;
  private Enum<?> task;
  private String query;
  private String eValue;
  private Long numAlignments;
  private Long numDescriptions;
  private Dust dust;
  private Seg seg;
  private Integer gapOpen;
  private Integer gapExtend;
  private Long reward;
  private Integer penalty;
  private ScoringMatrix matrix;
  private Short queryGeneticCode;
  private Enum<?> compBasedStats;
  private Long maxTargetSeqs;
  private Long wordSize;
  private Boolean softMasking;
  private Boolean lcaseMasking;
  private BlastReportFormat outFormat;
  private Long maxHSPs;

  @JsonGetter
  public BlastTool getTool() {
    return tool;
  }

  public MBlastJobConfig setTool(BlastTool tool) {
    this.tool = tool;
    return this;
  }

  @JsonGetter
  public Enum<?> getTask() {
    return task;
  }

  public MBlastJobConfig setTask(BlastNTask task) {
    this.task = task;
    return this;
  }

  public MBlastJobConfig setTask(TBlastNTask task) {
    this.task = task;
    return this;
  }

  public MBlastJobConfig setTask(BlastPTask task) {
    this.task = task;
    return this;
  }

  @JsonGetter
  public String getQuery() {
    return query;
  }

  public MBlastJobConfig setQuery(String query) {
    this.query = query;
    return this;
  }

  @JsonGetter(JSON_KEY_EXPECT_VALUE)
  public String getEValue() {
    return eValue;
  }

  @JsonSetter(JSON_KEY_EXPECT_VALUE)
  public MBlastJobConfig setEValue(String eValue) {
    this.eValue = eValue;
    return this;
  }

  @JsonGetter
  public Long getNumAlignments() {
    return numAlignments;
  }

  public MBlastJobConfig setNumAlignments(Long numAlignments) {
    this.numAlignments = numAlignments;
    return this;
  }

  @JsonGetter
  public Long getNumDescriptions() {
    return numDescriptions;
  }

  public MBlastJobConfig setNumDescriptions(Long numDescriptions) {
    this.numDescriptions = numDescriptions;
    return this;
  }

  @JsonGetter
  public Dust getDust() {
    return dust;
  }

  public MBlastJobConfig setDust(Dust dust) {
    this.dust = dust;
    return this;
  }

  @JsonGetter
  public Seg getSeg() {
    return seg;
  }

  public MBlastJobConfig setSeg(Seg seg) {
    this.seg = seg;
    return this;
  }

  @JsonGetter
  public Integer getGapOpen() {
    return gapOpen;
  }

  public MBlastJobConfig setGapOpen(Integer gapOpen) {
    this.gapOpen = gapOpen;
    return this;
  }

  @JsonGetter
  public Integer getGapExtend() {
    return gapExtend;
  }

  public MBlastJobConfig setGapExtend(Integer gapExtend) {
    this.gapExtend = gapExtend;
    return this;
  }

  @JsonGetter
  public Long getReward() {
    return reward;
  }

  public MBlastJobConfig setReward(Long reward) {
    this.reward = reward;
    return this;
  }

  @JsonGetter
  public Integer getPenalty() {
    return penalty;
  }

  public MBlastJobConfig setPenalty(Integer penalty) {
    this.penalty = penalty;
    return this;
  }

  @JsonGetter
  public ScoringMatrix getMatrix() {
    return matrix;
  }

  public MBlastJobConfig setMatrix(ScoringMatrix matrix) {
    this.matrix = matrix;
    return this;
  }

  @JsonGetter
  public Short getQueryGeneticCode() {
    return queryGeneticCode;
  }

  public MBlastJobConfig setQueryGeneticCode(Integer queryGeneticCode) {
    this.queryGeneticCode = queryGeneticCode == null ? null : queryGeneticCode.shortValue();
    return this;
  }

  @JsonGetter
  public Enum<?> getCompBasedStats() {
    return compBasedStats;
  }

  public MBlastJobConfig setCompBasedStats(CompBasedStatsLong compBasedStats) {
    this.compBasedStats = compBasedStats;
    return this;
  }

  @JsonGetter
  public MBlastJobConfig setCompBasedStats(CompBasedStatsShort compBasedStats) {
    this.compBasedStats = compBasedStats;
    return this;
  }

  @JsonGetter
  public Long getMaxTargetSeqs() {
    return maxTargetSeqs;
  }

  public MBlastJobConfig setMaxTargetSeqs(Long maxTargetSeqs) {
    this.maxTargetSeqs = maxTargetSeqs;
    return this;
  }

  @JsonGetter
  public Long getWordSize() {
    return wordSize;
  }

  public MBlastJobConfig setWordSize(Long wordSize) {
    this.wordSize = wordSize;
    return this;
  }

  @JsonGetter
  public Boolean getSoftMasking() {
    return softMasking;
  }

  public MBlastJobConfig setSoftMasking(Boolean softMasking) {
    this.softMasking = softMasking;
    return this;
  }

  @JsonGetter(JSON_KEY_LOWERCASE_MASKING)
  public Boolean getLowercaseMasking() {
    return lcaseMasking;
  }

  @JsonSetter(JSON_KEY_LOWERCASE_MASKING)
  public MBlastJobConfig setLowercaseMasking(Boolean lcaseMasking) {
    this.lcaseMasking = lcaseMasking;
    return this;
  }

  @JsonGetter
  public BlastReportFormat getOutFormat() {
    return outFormat;
  }

  public MBlastJobConfig setOutFormat(BlastReportFormat outFormat) {
    this.outFormat = outFormat;
    return this;
  }

  @JsonSetter(JSON_KEY_MAX_HSPS)
  public Long getMaxHSPs() {
    return maxHSPs;
  }

  @JsonGetter(JSON_KEY_MAX_HSPS)
  public MBlastJobConfig setMaxHSPs(Long maxHSPs) {
    this.maxHSPs = maxHSPs;
    return this;
  }
}
