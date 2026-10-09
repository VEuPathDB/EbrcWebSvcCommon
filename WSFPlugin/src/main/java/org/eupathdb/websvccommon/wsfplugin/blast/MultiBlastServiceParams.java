package org.eupathdb.websvccommon.wsfplugin.blast;

import java.util.*;
import java.util.function.Function;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.FormatUtil.Style;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.wsf.plugin.PluginUserException;
import org.veupathdb.lib.blast.BlastTool;
import org.veupathdb.lib.blast.field.*;

/**
 * Encapsulates the processing and conversion of multi-blast service params from
 * their WDK question form representation into the JSON accepted by the
 * multi-blast service.
 * <p>
 * NOTE: This is a transcription of the logic contained in:
 * <p>
 * <pre>
 *     web-multi-blast/blob/main/src/lib/utils/params.ts
 * </pre>
 * <p>
 * The two must be kept in sync so unexpected results are not shown in the
 * multi-blast UI and so users get the same result when they export to WDK.
 *
 * @author rdoherty
 */
public class MultiBlastServiceParams {

  private static final Logger LOG = Logger.getLogger(MultiBlastServiceParams.class);

  public static final String BLAST_DATABASE_ORGANISM_PARAM_NAME = "BlastDatabaseOrganism";
  public static final String BLAST_DATABASE_TYPE_PARAM_NAME = "MultiBlastDatabaseType";
  public static final String BLAST_QUERY_SEQUENCE_PARAM_NAME = "BlastQuerySequence";
  public static final String BLAST_ALGORITHM_PARAM_NAME = "BlastAlgorithm";

  // General config for all BLAST applications
  public static final String EXPECTATION_VALUE_PARAM_NAME = "ExpectationValue";
  public static final String NUM_QUERY_RESULTS_PARAM_NAME = "NumQueryResults";
  public static final String MAX_MATCHES_QUERY_RANGE_PARAM_NAME = "MaxMatchesQueryRange";

  // General config specific to each BLAST application
  public static final String WORD_SIZE_PARAM_NAME = "WordSize";
  public static final String SCORING_MATRIX_PARAM_NAME = "ScoringMatrix";
  public static final String COMP_ADJUST_PARAM_NAME = "CompAdjust";

  // Filter and masking config
  public static final String FILTER_LOW_COMPLEX_PARAM_NAME = "FilterLowComplex";
  public static final String SOFT_MASK_PARAM_NAME = "SoftMask";
  public static final String LOWER_CASE_MASK_PARAM_NAME = "LowerCaseMask";

  // Scoring config
  public static final String GAP_COSTS_PARAM_NAME = "GapCosts";
  public static final String MATCH_MISMATCH_SCORE = "MatchMismatchScore";

  public static String[] getAllParamNames() {
    return new String[] {
      BLAST_DATABASE_TYPE_PARAM_NAME,
      BLAST_ALGORITHM_PARAM_NAME,
      BLAST_DATABASE_ORGANISM_PARAM_NAME,
      BLAST_QUERY_SEQUENCE_PARAM_NAME,
      EXPECTATION_VALUE_PARAM_NAME,
      NUM_QUERY_RESULTS_PARAM_NAME,
      MAX_MATCHES_QUERY_RANGE_PARAM_NAME,
      WORD_SIZE_PARAM_NAME,
      SCORING_MATRIX_PARAM_NAME,
      MATCH_MISMATCH_SCORE,
      GAP_COSTS_PARAM_NAME,
      COMP_ADJUST_PARAM_NAME,
      FILTER_LOW_COMPLEX_PARAM_NAME,
      SOFT_MASK_PARAM_NAME,
      LOWER_CASE_MASK_PARAM_NAME
    };
  }

  /**
   * Converts the internal values of the WDK multiblast query params into
   * a JSON object passed to the multi-blast service to configure a new job for
   * a single input sequence.
   *
   * @param params internal values of params
   * @return json object to be passed as "config" to multi-blast service
   */
  public static MBlastJobConfig buildNewJobRequestConfig(Map<String, String> params) throws PluginUserException {
    LOG.info("Converting the following param values to JSON: " + FormatUtil.prettyPrint(params, Style.MULTI_LINE));

    var selectedTool = Objects.requireNonNull(ifParamNotNull(params, BLAST_ALGORITHM_PARAM_NAME, BlastTool::fromString));

    var requestConfig = buildBaseRequestConfig(params)
      .setTool(selectedTool);

    if (selectedTool != BlastTool.TBlastX) {
      var gapCostsStr = getNormalizedParamValue(params, GAP_COSTS_PARAM_NAME);
      var gapCostsPair = paramValueToIntPair(gapCostsStr);

      requestConfig
        .setGapOpen(gapCostsPair.getFirst())
        .setGapExtend(gapCostsPair.getSecond());
    }

    if (selectedTool == BlastTool.BlastN) {
      var matchMismatchStr = getNormalizedParamValue(params, MATCH_MISMATCH_SCORE);
      var rewardPenaltyPair = paramValueToIntPair(matchMismatchStr);

      return requestConfig
        .setTask(BlastNTask.BlastN)
        .setDust(ifParamNotNull(params, FILTER_LOW_COMPLEX_PARAM_NAME, Dust::fromString))
        .setReward(rewardPenaltyPair.getFirst().longValue())
        .setPenalty(rewardPenaltyPair.getSecond());
    }

    requestConfig
      .setMatrix(ifParamNotNull(params, SCORING_MATRIX_PARAM_NAME, ScoringMatrix::fromString))
      .setSeg(ifParamNotNull(params, FILTER_LOW_COMPLEX_PARAM_NAME, Seg::fromString));

    if (selectedTool == BlastTool.TBlastX)
      return requestConfig.setQueryGeneticCode(1);

    requestConfig.setCompBasedStats(ifParamNotNull(params, COMP_ADJUST_PARAM_NAME, CompBasedStatsLong::fromString));

    return switch (selectedTool) {
      case BlastP  -> requestConfig.setTask(BlastPTask.BlastP);
      case TBlastN -> requestConfig.setTask(TBlastNTask.TBlastN);
      case BlastX  -> requestConfig.setQueryGeneticCode(1);
      default      -> throw new PluginUserException("The tool type '" + selectedTool + "' is unsupported");
    };
  }

  /**
   * Converts the internal values of the WDK multiblast query params into
   * a JSON array passed to the multi-blast service which specifies the
   * databases the job should target
   *
   * @param params internal values of params
   * @return json array to be passed as "targets" to multi-blast service
   */
  public static List<MBlastJobRequest.JobTarget> buildNewJobRequestTargetList(Map<String, String> params) {
    var organismsStr = params.get(BLAST_DATABASE_ORGANISM_PARAM_NAME);
    var wdkTargetType = params.get(BLAST_DATABASE_TYPE_PARAM_NAME);

    var organisms = organismsStr.split(",");

    // FIXME This is a carryover of some hardcoding from
    // ApiCommonWebService's EuPathBlastCommandFormatter.
    // We should explore more permanent solutions.
    var blastTargetType = "PopSet".equals(wdkTargetType)
      ? "Isolates"
      : wdkTargetType;

    return Arrays.stream(organisms)
      .filter(organism -> !(organism.length() <= 3))
      .map(leafOrganism -> new MBlastJobRequest.JobTarget(leafOrganism, leafOrganism + blastTargetType))
      .toList();
  }

  private static MBlastJobConfig buildBaseRequestConfig(Map<String, String> params) {
    var requestConfig = new MBlastJobConfig()
      .setQuery(getNormalizedParamValue(params, BLAST_QUERY_SEQUENCE_PARAM_NAME))
      .setEValue(getNormalizedParamValue(params, EXPECTATION_VALUE_PARAM_NAME))
      .setMaxTargetSeqs(ifParamNotNull(params, NUM_QUERY_RESULTS_PARAM_NAME, Long::parseLong))
      .setWordSize(ifParamNotNull(params, WORD_SIZE_PARAM_NAME, Long::parseLong))
      .setSoftMasking(ifParamNotNull(params, SOFT_MASK_PARAM_NAME, Boolean::parseBoolean))
      .setLowercaseMasking(ifParamNotNull(params, LOWER_CASE_MASK_PARAM_NAME, Boolean::parseBoolean))
      .setOutFormat(new BlastReportFormat(FormatType.SingleFileBlastJSON));

    var maxMatches = ifParamNotNull(params, MAX_MATCHES_QUERY_RANGE_PARAM_NAME, Long::parseLong);

    if (maxMatches != null && maxMatches >= 1)
      requestConfig.setMaxHSPs(maxMatches);

    return requestConfig;
  }

  private static String getNormalizedParamValue(Map<String, String> params, String paramName) {
    return params.get(paramName).replaceAll("^'|'$", "");
  }

  private static <T> T ifParamNotNull(Map<String, String> params, String key, Function<String, T> fn) {
    var param = params.get(key);
    return param == null ? null : fn.apply(param.replaceAll("^'|'$", ""));
  }

  private static TwoTuple<Integer, Integer> paramValueToIntPair(String paramValue) {
    var pairStrValues = paramValue.split(",", 2);

    return new TwoTuple<>(
      Integer.parseInt(pairStrValues[0]),
      Integer.parseInt(pairStrValues[1])
    );
  }
}
