package org.eupathdb.websvccommon.wsfplugin.blast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public record BlastReportFormat(@JsonProperty FormatType format) {
  public BlastReportFormat(org.veupathdb.lib.blast.field.FormatType format) {
    this(FormatType.values()[format.ordinal()]);
  }

  enum FormatType {
    Pairwise("pairwise"),
    QueryAnchoredWithIdentities("query-anchored-with-identities"),
    QueryAnchoredWithoutIdentities("query-anchored-without-identities"),
    FlagQueryAnchoredWithIdentities("flat-query-anchored-with-identities"),
    FlatQueryAnchoredWithoutIdentities("flat-query-anchored-without-identities"),
    XML("xml"),
    Tabular("tabular"),
    TabularWithComments("tabular-with-comments"),
    TextASN1("text-asn-1"),
    BinaryASN1("binary-asn-1"),
    CSV("csv"),
    ArchiveASN1("archive-asn-1"),
    SeqAlignJSON("seqalign-json"),
    MultifileJSON("multi-file-json"),
    MultifileXML2("multi-file-xml2"),
    SingleFileJSON("single-file-json"),
    SingleFileXML2("single-file-xml2"),
    SAM("sam"),
    OrganismReport("organism-report");

    public final String jsonName;

    FormatType(String jsonName) {
      this.jsonName = jsonName;
    }

    @JsonValue
    public String getJsonName() {
      return jsonName;
    }
  }
}
