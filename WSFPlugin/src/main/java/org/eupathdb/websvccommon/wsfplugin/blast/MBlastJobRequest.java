package org.eupathdb.websvccommon.wsfplugin.blast;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

public class MBlastJobRequest {
  public static final String
    JSON_KEY_IS_PRIMARY = "isPrimary";

  private String site;
  private List<JobTarget> targets;
  private MBlastJobConfig config;
  private String description;
  private Integer maxResults;
  private Long maxResultSize;
  private Integer maxSequences;
  private boolean isPrimary;

  public String getSite() {
    return site;
  }

  public MBlastJobRequest setSite(String site) {
    this.site = site;
    return this;
  }

  @JsonGetter
  public List<JobTarget> getTargets() {
    return targets;
  }

  public MBlastJobRequest setTargets(List<JobTarget> targets) {
    this.targets = targets;
    return this;
  }

  public MBlastJobConfig getConfig() {
    return config;
  }

  public MBlastJobRequest setConfig(MBlastJobConfig config) {
    this.config = config;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public MBlastJobRequest setDescription(String description) {
    this.description = description;
    return this;
  }

  @JsonGetter
  public Integer getMaxResults() {
    return maxResults;
  }

  public MBlastJobRequest setMaxResults(Integer maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  @JsonGetter
  public Long getMaxResultSize() {
    return maxResultSize;
  }

  public MBlastJobRequest setMaxResultSize(Long maxResultSize) {
    this.maxResultSize = maxResultSize;
    return this;
  }

  @JsonGetter
  public Integer getMaxSequences() {
    return maxSequences;
  }

  public MBlastJobRequest setMaxSequences(Integer maxSequences) {
    this.maxSequences = maxSequences;
    return this;
  }

  @JsonGetter(JSON_KEY_IS_PRIMARY)
  public boolean isPrimary() {
    return isPrimary;
  }

  @JsonSetter(JSON_KEY_IS_PRIMARY)
  public MBlastJobRequest setPrimary(boolean primary) {
    isPrimary = primary;
    return this;
  }

  public static class JobTarget {
    private String organism;
    private String target;

    public JobTarget() {}

    public JobTarget(String organism, String target) {
      this.organism = organism;
      this.target = target;
    }

    public String getOrganism() {
      return organism;
    }

    public JobTarget setOrganism(String organism) {
      this.organism = organism;
      return this;
    }

    public String getTarget() {
      return target;
    }

    public JobTarget setTarget(String target) {
      this.target = target;
      return this;
    }
  }
}
