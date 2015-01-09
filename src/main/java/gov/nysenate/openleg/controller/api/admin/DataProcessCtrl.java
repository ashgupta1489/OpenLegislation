package gov.nysenate.openleg.controller.api.admin;

import com.google.common.collect.Range;
import gov.nysenate.openleg.client.response.base.BaseResponse;
import gov.nysenate.openleg.client.response.base.ListViewResponse;
import gov.nysenate.openleg.client.response.base.ViewObjectResponse;
import gov.nysenate.openleg.client.response.error.ErrorCode;
import gov.nysenate.openleg.client.response.error.ErrorResponse;
import gov.nysenate.openleg.client.view.process.DataProcessRunDetailView;
import gov.nysenate.openleg.client.view.process.DataProcessRunView;
import gov.nysenate.openleg.controller.api.base.BaseCtrl;
import gov.nysenate.openleg.controller.api.base.InvalidRequestParamEx;
import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.base.PaginatedList;
import gov.nysenate.openleg.model.base.Environment;
import gov.nysenate.openleg.model.process.DataProcessRun;
import gov.nysenate.openleg.service.process.DataProcessLogService;
import gov.nysenate.openleg.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static gov.nysenate.openleg.controller.api.base.BaseCtrl.BASE_ADMIN_API_PATH;
import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(value = BASE_ADMIN_API_PATH + "/process", method = RequestMethod.GET)
public class DataProcessCtrl extends BaseCtrl
{
    @Autowired private Environment env;
    @Autowired private DataProcessLogService processLogs;

    /**
     * Data Process Runs API
     * ---------------------
     *
     * Get the process runs that occurred within a given data/time range.
     * Usage;
     * (GET) /api/3/admin/process/runs
     * (GET) /api/3/admin/process/runs/{from}
     * (GET) /api/3/admin/process/runs/{from}/{to}
     *
     * Where 'from' and 'to' are date times.
     *
     * Optional Params: full (boolean) - If true, returns process runs with no activity as well
     *                  detail (boolean) - If true, returns the first hundred or so units for each run.
     *                  limit, offset (int) - Paginate through the runs.
     *
     * Expected Output: DataProcessRunDetailView if 'detail' = true, DataProcessRunView otherwise.
     */

    /**
     * Gets the process runs from the past week.
     * @see #getRunsDuring(String, String, WebRequest)
     */
    @RequestMapping("/runs")
    public BaseResponse getRecentRuns(WebRequest request) throws InvalidRequestParamEx {
        return getRunsDuring(LocalDateTime.now().minusDays(7), LocalDateTime.now(), request);
    }

    /**
     * Gets the process runs from a given date time.
     * @see #getRunsDuring(String, String, WebRequest)
     */
    @RequestMapping("/runs/{from}")
    public BaseResponse getRunsFrom(@PathVariable String from, WebRequest request) throws InvalidRequestParamEx {
        LocalDateTime fromDateTime = parseISODateTime(from, "from");
        return getRunsDuring(fromDateTime, LocalDateTime.now(), request);
    }

    @RequestMapping("/runs/{from}/{to}")
    public BaseResponse getRunsDuring(@PathVariable String from, @PathVariable String to, WebRequest request)
                                      throws InvalidRequestParamEx {
        LocalDateTime fromDateTime = parseISODateTime(from, "from");
        LocalDateTime toDateTime = parseISODateTime(to, "to");
        return getRunsDuring(fromDateTime, toDateTime, request);
    }

    private BaseResponse getRunsDuring(LocalDateTime fromDateTime, LocalDateTime toDateTime, WebRequest request) {
        LimitOffset limOff = getLimitOffset(request, 100);
        boolean full = getBooleanParam(request, "full", false);
        boolean detail = getBooleanParam(request, "detail", false);

        PaginatedList<DataProcessRun> runs = processLogs.getRuns(Range.closedOpen(fromDateTime, toDateTime), limOff, !full);
        return ListViewResponse.of(runs.getResults().stream()
            .map(run -> (detail)
                    ? new DataProcessRunDetailView(run, processLogs.getUnits(run.getProcessId(), LimitOffset.HUNDRED))
                    : new DataProcessRunView(run))
            .collect(toList()),
            runs.getTotal(), runs.getLimOff());
    }

    /**
     * Single Data Process Run API
     * ---------------------------
     *
     * Get a single data process run via the process id (int).
     * Usage: (GET) /api/3/admin/process/runs/{id}
     *
     * Optional Params: limit, offset (int) - Paginate through the units associated with this run.
     *
     * Expected Output: DataProcessRunDetailView
     */
    @RequestMapping("/runs/{id:[0-9]+}")
    public BaseResponse getRuns(@PathVariable int id, WebRequest webRequest) {
        LimitOffset limOff = getLimitOffset(webRequest, 100);
        Optional<DataProcessRun> run = processLogs.getRun(id);
        if (run.isPresent()) {
            return new ViewObjectResponse<>(
                new DataProcessRunDetailView(run.get(), processLogs.getUnits(run.get().getProcessId(), limOff)));
        }
        else {
            return new ErrorResponse(ErrorCode.PROCESS_RUN_NOT_FOUND);
        }
    }
}