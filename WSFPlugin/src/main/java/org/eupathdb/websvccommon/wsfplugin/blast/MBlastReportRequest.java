package org.eupathdb.websvccommon.wsfplugin.blast;

import com.fasterxml.jackson.annotation.JsonGetter;

import javax.validation.constraints.NotNull;

public class MBlastReportRequest {
  public static final String
    JSON_KEY_JOB_ID = "jobID";

  private final String jobId;
  private String description;
  private BlastReportFormat format;
  private String            fieldDelim;

  public MBlastReportRequest(@NotNull String jobId) {
    this.jobId = jobId;
  }

  @JsonGetter(JSON_KEY_JOB_ID)
  public String getJobId() {
    return jobId;
  }

  public String getDescription() {
    return description;
  }

  public MBlastReportRequest setDescription(String description) {
    this.description = description;
    return this;
  }

  public BlastReportFormat getFormat() {
    return format;
  }

  public MBlastReportRequest setFormat(BlastReportFormat format) {
    this.format = format;
    return this;
  }

  public String getFieldDelim() {
    return fieldDelim;
  }

  public MBlastReportRequest setFieldDelim(String fieldDelim) {
    this.fieldDelim = fieldDelim;
    return this;
  }
}
