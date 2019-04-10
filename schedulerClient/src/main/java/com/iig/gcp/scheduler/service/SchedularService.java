package com.iig.gcp.scheduler.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.validation.Valid;

import com.iig.gcp.scheduler.dto.*;

public interface SchedularService {

	// Current Table
	List<DailyJobsDTO> allCurrentJobs(String project) throws Exception;

	ArrayList<String> getFeedFromCurrent(String project) throws Exception;

	List<DailyJobsDTO> filterCurrentJobs(String status, String feedId) throws Exception;

	HashMap<String, ArrayList<String>> allCurrentJobsGroupByFeedId() throws Exception;

	String runScheduleJob(@Valid String feedId, String jobId, String batchDate) throws Exception;

	String killCurrentJob(@Valid String feedId, String jobId, String batchDate) throws Exception;

	String invokeRest(String json, String url) throws UnsupportedOperationException, Exception;

	// Adhoc Scheduling
	ArrayList<BatchDetailsDTO> getBatchDetails(String project_id) throws Exception;

	ArrayList<BatchDetailsDTO> getCreateBatchDetails(String project_id) throws Exception;

	ArrayList<BatchDetailsDTO> getEditBatchDetails(String project_id) throws Exception;

	BatchTableDetailsDTO extractBatchDetails(String batch_id, String project_id) throws Exception;

	ArrayList<TaskSequenceDTO> getJobDetails(String batch_id, String project_id) throws Exception;

	ArrayList<DailyJobsDTO> getAdhocJobDetails(String batch_id, String project_id, String job_name) throws Exception;

	String getBatchSequence(String batch_id, String project_id, String job_name, int i) throws Exception;

	ArrayList<String> getKafkaTopic() throws Exception;

	ArrayList<String> getBatchJobs(String batch_id, String project_id) throws Exception;;

	AdhocJobDTO extractBatchJobDetails(String batch_id, String project_id, String job_id) throws Exception;

	ArrayList<BatchDetailsDTO> getAdhocBatchCreateDetails(String project_id) throws Exception;

	ArrayList<BatchDetailsDTO> getAdhocBatchEditDetails(String project_id) throws Exception;

	public ArrayList<String> getGoogleProject(String project_id) throws Exception;

	public ArrayList<String> getServiceAccount(String project_id) throws Exception;

	// Archive table
	ArrayList<String> getFeedIdList() throws Exception;

	ArrayList<ArchiveJobsDTO> getListOfArchievJobs(@Valid String feed_id) throws Exception;

	ArrayList<ArchiveJobsDTO> getChartDetails(@Valid String job_id) throws Exception;

	List<ArchiveJobsDTO> getRunStats(@Valid String job_id, @Valid String feed_id) throws Exception;

	// Master Table
	ArrayList<String> getFeedFromMaster(String project) throws Exception;

	List<MasterJobsDTO> allLoadJobs(String project) throws Exception;

	List<MasterJobsDTO> typeLoadJobs(String frequency, String batchId) throws Exception;

	// MasterJobsDTO orderJobFromMaster(String feedId,String jobId) throws
	// ClassNotFoundException, SQLException, ParseException;
	String moveJobFromMasterToCurrentJob(String feedId) throws ClassNotFoundException, SQLException, Exception;

	String deleteJobFromMaster(String feedId) throws Exception;

	String suspendJobFromMaster(String feedId) throws ClassNotFoundException, SQLException, Exception;

	String unSuspendJobFromMaster(@Valid String feedId) throws Exception;

	ArrayList<TaskSequenceDTO> getBatchDetails1(String project_id, String schtype) throws Exception;

	ArrayList<BatchSequenceDTO> getBatchDetails2(@Valid String batch_id, @Valid String project_id) throws Exception;

	// Batch Scheduling
	ArrayList<BatchFlowDTO> getBatchFlow(String project_id) throws Exception;

	BatchFlowDTO getBatchFlowDetails(String project_id, String flowname) throws Exception;

	ArrayList<BatchSequenceDTO> getBatchSequence(String sequence) throws Exception;

	ArrayList<TaskLocation> runUnixCommand(String script_location) throws Exception;

	ArrayList<String> getBuckets(String google_project, String service_account) throws Exception;

	ArrayList<String> getBucketFiles(String google_project, String service_account, String bucket) throws Exception;

}
