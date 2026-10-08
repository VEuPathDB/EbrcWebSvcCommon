package org.eupathdb.websvccommon.wsfplugin.blast;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.veupathdb.lib.blast.field.FormatType;

import javax.validation.constraints.NotNull;

public class MBlastReportRequest {
  public static final String
    JSON_KEY_JOB_ID = "jobID";

  private final String jobId;
  private String description;
  private BlastReportFormat.FormatType format;
  private String fieldDelim;

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

  public BlastReportFormat.FormatType getFormat() {
    return format;
  }

  public MBlastReportRequest setFormat(FormatType format) {
    this.format = BlastReportFormat.FormatType.fromFormat(format);
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
