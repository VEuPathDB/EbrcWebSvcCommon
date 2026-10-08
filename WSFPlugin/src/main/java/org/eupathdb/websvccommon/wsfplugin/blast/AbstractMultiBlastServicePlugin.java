package org.eupathdb.websvccommon.wsfplugin.blast;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.json.JsonUtil.Jackson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eupathdb.common.model.MultiBlastServiceUtil;
import org.eupathdb.common.model.ProjectMapper;
import org.eupathdb.common.service.PostValidationUserException;
import org.eupathdb.websvccommon.wsfplugin.PluginUtilities;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.runtime.ThreadUtil;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wsf.plugin.AbstractPlugin;
import org.gusdb.wsf.plugin.DelayedResultException;
import org.gusdb.wsf.plugin.Plugin;
import org.gusdb.wsf.plugin.PluginModelException;
import org.gusdb.wsf.plugin.PluginRequest;
import org.gusdb.wsf.plugin.PluginResponse;
import org.gusdb.wsf.plugin.PluginUserException;
import org.veupathdb.lib.blast.field.FormatType;

public abstract class AbstractMultiBlastServicePlugin extends AbstractPlugin {
  private static final Logger LOG = LogManager.getLogger(AbstractMultiBlastServicePlugin.class);

  protected static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  private static final int INITIAL_WAIT_TIME_MILLIS = 2 /* seconds */ * 1000;
  private static final int POLLING_INTERVAL_MILLIS = 5 /* seconds */ * 1000;
  private static final int MAX_WAIT_TIME_MILLIS = 5 /* minutes */ * 60 * 1000;
  private static final int MAX_REPORT_SIZE_BYTES = 90 /* megabytes */ * 1000 * 1000;

  private static final String CONTENT_MAX_LENGTH_EXCEEDED_STATUS =
    "bad-request";
  private static final String CONTENT_MAX_LENGTH_EXCEEDED_MESSAGE =
    "Requested report is larger than the specified max content size.";

  // field definitions in the config file
  private static final String FILE_CONFIG = "multiblast-config.xml";

  public static class BlastServiceBadRequestException extends PostValidationUserException {
    public BlastServiceBadRequestException(String message) {
      super(message);
    }
  }

  private final ResultFormatter _resultFormatter;

  public AbstractMultiBlastServicePlugin(ResultFormatter resultFormatter) {
    super(FILE_CONFIG);
    _resultFormatter = resultFormatter;
  }

  @Override
  public void initialize(PluginRequest request) throws PluginModelException {
    super.initialize(request);
    _resultFormatter.setConfig(new BlastConfig(properties));
  }

  @Override
  public String[] getRequiredParameterNames() {
    return MultiBlastServiceParams.getAllParamNames();
  }

  @Override
  public String[] getColumns(PluginRequest request) throws PluginModelException {
    return _resultFormatter.getDeclaredColumns();
  }

  @Override
  public void validateParameters(PluginRequest request) throws PluginModelException, PluginUserException {
    // WDK handles most validation; simply confirm single submitted sequence
    String sequence = request.getParams().get(MultiBlastServiceParams.BLAST_QUERY_SEQUENCE_PARAM_NAME);
    if (sequence.indexOf('>') != sequence.lastIndexOf('>')) {
      // more than one sequence
      throw new PluginUserException("Only one sequence can be submitted at a time (should have been validated by StringParam regex).");
    }
  }

  @Override
  protected int execute(PluginRequest request, PluginResponse response)
      throws PluginModelException, PluginUserException, DelayedResultException {

    // get the WDK model
    WdkModel wdkModel = PluginUtilities.getWdkModel(request);

    // set up project mapper
    try {
      ProjectMapper projectMapper = ProjectMapper.getMapper(wdkModel);
      _resultFormatter.setProjectMapper(projectMapper);
    } catch (WdkModelException ex) {
      LOG.error("WdkModelException: {}", String.valueOf(ex));
      throw new PluginModelException(ex);
    }

    // get the required authentication header for this user
    TwoTuple<String,String> authHeader = Plugin.getServiceAuthorizationHeader(
        request.getContext().get(Utilities.CONTEXT_KEY_BEARER_TOKEN_STRING));

    // find base URL for multi-blast service
    String multiBlastServiceUrl = MultiBlastServiceUtil.getMultiBlastServiceUrl(
        PluginUtilities.getWdkModel(request), PluginModelException::new);

    // retrieve project ID
    String projectId = wdkModel.getProjectId();

    // use passed params to POST new job request to blast service
    var newJobRequestJson = new MBlastJobRequest()
      .setSite(projectId)
      .setMaxResultSize(0L)
      .setMaxSequences(1)
      .setPrimary(false)
      .setConfig(buildNewBlastConfig(request.getParams()))
      .setTargets(buildBlastTargetList(request.getParams()));

    String jobId = createJob(newJobRequestJson, multiBlastServiceUrl, authHeader);

    // start timer on wait time
    Timer t = new Timer();

    // wait a short interval for blast service to look job up in cache and assign complete status
    ThreadUtil.sleep(INITIAL_WAIT_TIME_MILLIS);

    // query the job status (if results in cache, should return complete immediately)
    // keep going until job complete or max wait time expired
    while (!isJobComplete(multiBlastServiceUrl, jobId, authHeader)) {

      // if max wait time reached, throw delayed result exception
      if (t.getElapsed() > (MAX_WAIT_TIME_MILLIS)) {
        throw new DelayedResultException();
      }

      // sleep until ready to poll again
      ThreadUtil.sleep(POLLING_INTERVAL_MILLIS);
    }

    // create a new "pairwise" report for this job
    var newReportRequestJson = new MBlastReportRequest(jobId)
      .setFormat(new BlastReportFormat(FormatType.Pairwise));

    String reportId = createReport(newReportRequestJson, multiBlastServiceUrl, authHeader);

    // query the report status (if results in cache, should return complete immediately)
    // keep going until report complete or max wait time expired
    while (!isReportComplete(multiBlastServiceUrl, reportId, authHeader)) {

      // if max wait time reached, throw delayed result exception
      if (t.getElapsed() > (MAX_WAIT_TIME_MILLIS)) {
        throw new DelayedResultException();
      }

      // sleep until ready to poll again
      ThreadUtil.sleep(POLLING_INTERVAL_MILLIS);
    }

    // job and report complete; gather remaining prerequisites
    RecordClass recordClass = PluginUtilities.getRecordClass(request);
    String dbType = request.getParams().get(MultiBlastServiceParams.BLAST_DATABASE_TYPE_PARAM_NAME);
    String[] orderedColumns = request.getOrderedColumns();

    // write results to plugin response
    writeResults(multiBlastServiceUrl, reportId, authHeader, response, wdkModel, recordClass, dbType, orderedColumns);

    return 0;
  }

  protected MBlastJobConfig buildNewBlastConfig(Map<String, String> params) throws PluginUserException {
    return MultiBlastServiceParams.buildNewJobRequestConfig(params);
  }

  protected List<MBlastJobRequest.JobTarget> buildBlastTargetList(Map<String, String> params) {
    return MultiBlastServiceParams.buildNewJobRequestTargetList(params);
  }

  private void writeResults(
    String multiBlastServiceUrl,
    String reportId,
    TwoTuple<String,String> authHeader,
    PluginResponse pluginResponse,
    WdkModel wdkModel,
    RecordClass recordClass,
    String dbType,
    String[] orderedColumns
  ) throws PluginModelException, PluginUserException {

    // define request data
    String downloadReportUrl = multiBlastServiceUrl + "/reports/" + reportId + "/files/report.txt?download=false";

    LOG.info("Requesting multi-blast report results at {}", downloadReportUrl);

    // make job report request
    try {

      var mblastResponse = HTTP_CLIENT.send(
        HttpRequest.newBuilder(URI.create(downloadReportUrl))
          .header(authHeader.getFirst(), authHeader.getSecond())
          .header("Content-Max-Length", String.valueOf(MAX_REPORT_SIZE_BYTES))
          .build(),
        HttpResponse.BodyHandlers.ofInputStream()
      );

      if (mblastResponse.statusCode() != 200) {
        var responseJson = Jackson.readTree(mblastResponse.body());

        if (
          CONTENT_MAX_LENGTH_EXCEEDED_STATUS.equals(getString(responseJson, "status")) &&
          CONTENT_MAX_LENGTH_EXCEEDED_MESSAGE.equals(getString(responseJson, "message"))
        ) {
          throw new BlastServiceBadRequestException(
            "We're sorry, but we cannot handle BLAST results larger than " +
              MAX_REPORT_SIZE_BYTES/1000000 + "MB. \nIf you see the option to download your result, you may do so. \nTo reduce the result size, you " +
              "could decrease V=B or the Expectation value, turn on the Low " +
              "Complexity filter, or decrease the number of target organisms selected.");
        }

        throw new PluginModelException("Unexpected response from multi-blast " +
          "service while fetching report results (reportId=" + reportId + "): " +
          mblastResponse.statusCode() + FormatUtil.NL + responseJson);
      }

      // request appears to be successful; read, parse and write result stream data into plugin response
      try (var resultStream = mblastResponse.body()) {
        var message = _resultFormatter.formatResult(
          pluginResponse,
          orderedColumns,
          resultStream,
          recordClass,
          dbType,
          wdkModel
        );
        pluginResponse.setMessage(message);
      }
    }
    catch (InterruptedException | IOException e) {
      throw new PluginModelException("Unable to read response body from service response.", e);
    }
  }

  /**
   * Makes a request to the multi-blast service to check the status of the job
   * with the passed ID.  Returns whether job is complete or still running. If
   * job status is "errored", throws a PluginModelException with the description.
   * <p>
   * NOTE: If the job if found to be "expired", it will be rerun
   *
   * @param multiBlastServiceUrl blast service base URL
   * @param jobId job whose status to fetch
   * @return true if job is complete, else false (if still running)
   * @throws PluginModelException if job has errored
   */
  private static boolean isJobComplete(String multiBlastServiceUrl, String jobId, TwoTuple<String,String> authHeader) throws PluginModelException {
    String jobIdEndpointUrl = multiBlastServiceUrl + "/jobs/" + jobId;
    LOG.info("Requesting multi-blast job status at {}", jobIdEndpointUrl);

    // make job status request
    try {
      var response = HTTP_CLIENT.send(
        HttpRequest.newBuilder(URI.create(jobIdEndpointUrl))
          .header(authHeader.getFirst(), authHeader.getSecond())
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() != 200)
        throw new PluginModelException("Unexpected response from multi-blast " +
          "service while checking job status (jobId=" + jobId + "): " +
          response.statusCode() + FormatUtil.NL + response.body());

      // parse response and analyze

      var responseObj = Jackson.readTree(response.body());
      return switch (getString(responseObj, "status", "")) {
        case "queued", "in-progress" -> false;
        case "expired" -> {
          rerunJob(multiBlastServiceUrl, jobId, authHeader);
          yield false;
        }
        case "completed" -> true;
        case "errored" -> throw new PluginModelException(
          "Multi-blast service job failed: " + responseObj.get("description"));
        default -> throw new PluginModelException(
          "Multi-blast service job status endpoint returned unrecognized status value: " +
          responseObj.get("status"));
      };
    }
    catch (InterruptedException | IOException e) {
      throw new PluginModelException("Unable to read response body from service response.", e);
    }
  }

  /**
   * Makes a request to the multi-blast service to check the status of the report
   * with the passed ID.  Returns whether report is complete or still running. If
   * report status is "errored", throws a PluginModelException with the description.
   * <p>
   * NOTE: If the report if found to be "expired", it will be rerun
   *
   * @param multiBlastServiceUrl blast service base URL
   * @param reportId report whose status to fetch
   * @return true if report is complete, else false (if still running)
   * @throws PluginModelException if report has errored
   */
  private static boolean isReportComplete(String multiBlastServiceUrl, String reportId, TwoTuple<String,String> authHeader) throws PluginModelException {
    String reportIdEndpointUrl = multiBlastServiceUrl + "/reports/" + reportId;
    LOG.info("Requesting multi-blast report status at {}", reportIdEndpointUrl);

    // make job status request
    try {

      var response = HTTP_CLIENT.send(
        HttpRequest.newBuilder(URI.create(reportIdEndpointUrl)).build(),
        HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() != 200) {
        throw new PluginModelException("Unexpected response from multi-blast " +
          "service while checking report status (reportId=" + reportId + "): " +
          response.statusCode() + FormatUtil.NL + response.body());
      }

      // parse response and analyze
      var responseObj = Jackson.readTree(response.body());
      return switch (getString(responseObj, "status", "")) {
        case "queued", "in-progress" -> false;
        case "expired" -> {
          rerunReport(multiBlastServiceUrl, reportId, authHeader);
          yield false;
        }
        case "completed" -> true;
        case "errored" -> throw new PluginModelException(
          "Multi-blast service report failed. This is usually a temporary network problem, please try later. " +
            "In the meantime if you see a Download dropdown menu, you might be able to get the result.");
        default -> throw new PluginModelException(
          "Multi-blast service report status endpoint returned unrecognized status value: " + responseObj.get(
            "status"));
      };
    }
    catch (InterruptedException | IOException e) {
      throw new PluginModelException("Unable to read response body from service response.", e);
    }
  }

  private static String createJob(
    MBlastJobRequest newJobRequestBody,
    String multiBlastServiceUrl,
    TwoTuple<String,String> authHeader
  ) throws PluginModelException {
    var jobsEndpointUrl = multiBlastServiceUrl + "/jobs";
    var requestBody = JsonUtil.toJsonNode(newJobRequestBody).toString();

    LOG.info("Requesting new multi-blast job at {} with JSON body: {}", jobsEndpointUrl, requestBody);

    // make new job request
    try {
      var response = HTTP_CLIENT.send(
        HttpRequest.newBuilder(URI.create(jobsEndpointUrl))
          .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .setHeader(authHeader.getFirst(), authHeader.getSecond())
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() == 200)
        return getString(Jackson.readTree(response.body()), "jobID");

      if (isClientError(response))
        throw new BlastServiceBadRequestException(
            "Multi-Blast service job request returned " + response.statusCode() + NL + response.body());

      // other error
      throw new PluginModelException("Unexpected response from multi-blast " +
          "service while requesting new job: " + response.statusCode() + NL + response.body());
    }
    catch (InterruptedException | IOException e) {
      throw new PluginModelException("Unable to read response body from service response.", e);
    }
  }

  private static boolean isClientError(HttpResponse<?> response) {
    return response.statusCode() >= 400 && response.statusCode() < 500;
  }

  private static String createReport(
    MBlastReportRequest newReportRequestBody,
    String multiBlastServiceUrl,
    TwoTuple<String,String> authHeader
  ) throws PluginModelException {
    var reportsEndpointUrl = multiBlastServiceUrl + "/reports";
    var requestBody = JsonUtil.toJsonNode(newReportRequestBody).toString();

    LOG.info(
      "Requesting new multi-blast report at {} with JSON body: {}",
      reportsEndpointUrl,
      requestBody
    );

    // make new report request
    try {
      var response = HTTP_CLIENT.send(
        HttpRequest.newBuilder(URI.create(reportsEndpointUrl))
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .header(HttpHeaders.CONTENT_TYPE, "application/json")
          .header(authHeader.getFirst(), authHeader.getSecond())
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() == 200)
        return getString(Jackson.readTree(response.body()), "reportID");

      throw new PluginModelException("Unexpected response from multi-blast " +
          "service while requesting new report: " + response.statusCode() + NL + response.body());
    }
    catch (InterruptedException | IOException e) {
      throw new PluginModelException("Unable to read response body from service response.", e);
    }
  }

  private static void rerunJob(String multiBlastServiceUrl, String jobId, TwoTuple<String,String> authHeader) throws PluginModelException {
    String jobsIdEndpointUrl = multiBlastServiceUrl + "/jobs/" + jobId;
    LOG.info("Rerunning expired multi-blast job at {} with job id {}", jobsIdEndpointUrl, jobId);

    // make rerun job request
    try {
      var response = HTTP_CLIENT.send(
        HttpRequest.newBuilder(URI.create(jobsIdEndpointUrl))
          .POST(HttpRequest.BodyPublishers.noBody())
          .header(authHeader.getFirst(), authHeader.getSecond())
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() != 200 && response.statusCode() != 204) {
        throw new PluginModelException("Unexpected response from multi-blast " +
            "service while rerunning job: " + response.statusCode() + NL + response.body());
      }
    }
    catch (InterruptedException | IOException e) {
      throw new PluginModelException("Unable to read response body from service response.", e);
    }
  }

  private static void rerunReport(String multiBlastServiceUrl, String reportId, TwoTuple<String,String> authHeader) throws PluginModelException {
    String reportsIdEndpointUrl = multiBlastServiceUrl + "/reports/" + reportId;
    LOG.info("Rerunning expired multi-blast report at {} with report id {}", reportsIdEndpointUrl, reportId);

    // make rerun report request
    try {
      var response = HTTP_CLIENT.send(
        HttpRequest.newBuilder(URI.create(reportsIdEndpointUrl))
          .POST(HttpRequest.BodyPublishers.noBody())
          .header(authHeader.getFirst(), authHeader.getSecond())
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() != 200 && response.statusCode() != 204) {
        throw new PluginModelException("Unexpected response from multi-blast " +
          "service while rerunning report: " + response.statusCode() + NL + response.body());
      }
    }
    catch (InterruptedException | IOException e) {
      throw new PluginModelException("Unable to read response body from service response.", e);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static String getString(JsonNode json, String key, String fallback) {
    var res = getString(json, key);
    return res == null ? fallback : res;
  }

  private static String getString(JsonNode json, String key) {
    var node = json.get(key);
    return node == null ? null : node.textValue();
  }
}
