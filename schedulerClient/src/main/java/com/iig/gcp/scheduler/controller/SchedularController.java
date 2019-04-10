package com.iig.gcp.scheduler.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iig.gcp.CustomAuthenticationProvider;
import com.iig.gcp.scheduler.dto.*;
import com.iig.gcp.scheduler.service.*;

@Controller
@SessionAttributes(value = { "user_name", "project_name", "jwt" })
public class SchedularController {

	@Value("${parent.front.micro.services}")
	private String parent_ms;

	@Autowired
	SchedularService schedularService;

	@Autowired
	private AuthenticationManager authenticationManager;

	// Master Table

	/**
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/" }, method = RequestMethod.GET)
	public ModelAndView homePage(@Valid @ModelAttribute("jsonObject") String jsonObject, ModelMap modelMap,
			HttpServletRequest request) {
		// Validate the token at the first place

		if (jsonObject == null || jsonObject.equals("")) {
			return new ModelAndView("/login");
		}

		JSONObject jObj = new JSONObject(jsonObject);
		String user_name = jObj.getString("user");
		String project_name = jObj.getString("project");
		String jwt = jObj.getString("jwt");

		try {
			JSONObject jsonModelObject = null;
			if (modelMap.get("jsonObject") == null || modelMap.get("jsonObject").equals("")) {
				return new ModelAndView("/login");
			}
			jsonModelObject = new JSONObject(modelMap.get("jsonObject").toString());

			authenticationByJWT(user_name + ":" + project_name, jsonModelObject.get("jwt").toString());
		} catch (Exception e) {
			e.printStackTrace();
			return new ModelAndView("/login");
			// redirect to Login Page
		}

		request.getSession().setAttribute("user_name", user_name);
		request.getSession().setAttribute("project_name", project_name);
		request.getSession().setAttribute("jwt", jwt);

		return new ModelAndView("/index");
	}

	@RequestMapping(value = { "/parent" }, method = RequestMethod.GET)
	public ModelAndView parentHome(ModelMap modelMap, HttpServletRequest request, Authentication auth)
			throws JSONException {
		CustomAuthenticationProvider.MyUser m = (CustomAuthenticationProvider.MyUser) SecurityContextHolder.getContext()
				.getAuthentication().getPrincipal();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("userId", m.getName());
		jsonObject.put("project", m.getProject());
		jsonObject.put("jwt", m.getJwt());
		modelMap.addAttribute("jsonObject", jsonObject.toString());
		return new ModelAndView("redirect:" + "//" + parent_ms + "/fromChild", modelMap);

	}

	private void authenticationByJWT(String name, String token) {
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(name, token);
		Authentication authenticate = authenticationManager.authenticate(authToken);
		SecurityContextHolder.getContext().setAuthentication(authenticate);
	}

	/**
	 * 
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/scheduler/viewAllJobs" }, method = RequestMethod.GET)
	public ModelAndView allJobs(ModelMap modelMap, HttpServletRequest request) {
		try {

			HashMap<String, List<MasterJobsDTO>> hsMap = new HashMap<String, List<MasterJobsDTO>>();
			hsMap.put("ALL", schedularService.allLoadJobs((String) request.getSession().getAttribute("project_name")));
			ArrayList<String> arrfeedId = schedularService
					.getFeedFromMaster((String) request.getSession().getAttribute("project_name"));
			modelMap.addAttribute("arrfeedId", arrfeedId);
			modelMap.addAttribute("allLoadJobs", hsMap.get("ALL"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ModelAndView("/schedular/viewAllJobs");
	}

	/**
	 * This method populates the View Run Statics Screen with FeedID
	 * 
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/scheduler/runstats" }, method = RequestMethod.GET)
	public ModelAndView viewRunStatics(ModelMap modelMap) {
		try {
			ArrayList<String> arrFeedId = schedularService.getFeedIdList();
			modelMap.addAttribute("feed_id", arrFeedId);
		} catch (Exception e) {
			modelMap.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/viewRunStatics");
	}

	/**
	 * This method first fetch MasterFeed data and insert into the CurrentFeed
	 * 
	 * @param feed_id
	 * @param job_id
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/scheduler/runMasterJob" }, method = RequestMethod.POST)
	public ModelAndView moveJobFromMasterToCurrent(@Valid @RequestParam("feedId") String feedId, ModelMap modelMap,
			HttpServletRequest request) {
		try {
			String message = schedularService.moveJobFromMasterToCurrentJob(feedId);
			if (message.equals("Success")) {
				modelMap.addAttribute("successString", "Job Ordered for today");
			} else {
				modelMap.addAttribute("errorStatus", "Job ordering failed");
			}
		} catch (Exception e) {
			e.printStackTrace();
			modelMap.addAttribute("errorStatus", e.getMessage());
		}
		return allJobs(modelMap, request);
	}

	/**
	 * This method suspends the job in master table.
	 * 
	 * @param feed_id
	 * @param job_id
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/scheduler/suspendMasterJob" }, method = RequestMethod.POST)
	public ModelAndView suspendJobFromMaster(@Valid @RequestParam("feedId") String feedId, ModelMap modelMap,
			HttpServletRequest request) {
		try {
			String suspendStatus = schedularService.suspendJobFromMaster(feedId);
			if (suspendStatus.equals("Success")) {
				modelMap.addAttribute("successString", "Job Suspended");
			} else {
				modelMap.addAttribute("errorStatus", "Job Suspense failure");
			}
		} catch (Exception e) {
			e.printStackTrace();
			modelMap.addAttribute("errorStatus", e.getMessage());
		}
		return allJobs(modelMap, request);
	}

	/**
	 * This method unsuspends the job in master table.
	 * 
	 * @param feed_id
	 * @param job_id
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/scheduler/unSuspendMasterJob" }, method = RequestMethod.POST)
	public ModelAndView unSuspendJobFromMaster(@Valid @RequestParam("feedId") String feedId, ModelMap modelMap,
			HttpServletRequest request) {
		try {
			String suspendStatus = schedularService.unSuspendJobFromMaster(feedId);
			if (suspendStatus.equals("Success")) {
				modelMap.addAttribute("successString", "Job Suspended");
			} else {
				modelMap.addAttribute("errorStatus", "Job Suspense failure");
			}
		} catch (Exception e) {
			e.printStackTrace();
			modelMap.addAttribute("errorStatus", e.getMessage());
		}
		return allJobs(modelMap, request);
	}

	/**
	 * This method deletes the record from MasterFeed data
	 * 
	 * @param feed_id
	 * @param job_id
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/scheduler/deleteMasterJob" }, method = RequestMethod.POST)
	public ModelAndView deleteJobFromMaster(@Valid @RequestParam("feedId") String feedId, ModelMap modelMap) {
		try {
			schedularService.deleteJobFromMaster(feedId);
			modelMap.addAttribute("successString", "Job deleted");

		} catch (Exception e) {

			modelMap.addAttribute("errorStatus", e.getMessage());

		}
		return new ModelAndView("schedular/viewAllJobs1");
	}

	/**
	 * This method populated the View Run Statics Screen with list of Job Id w.r.t.
	 * feedid
	 * 
	 * @param feed_id
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/schedule/feedIdFilter" }, method = RequestMethod.POST)
	public ModelAndView getJobIdFilter(@Valid @RequestParam("feed_id") String feed_id, ModelMap modelMap) {
		try {
			ArrayList<ArchiveJobsDTO> arrArchiveJobs = schedularService.getListOfArchievJobs(feed_id);
			ArrayList<String> arrJobId = new ArrayList<String>();
			for (ArchiveJobsDTO archiveJob : arrArchiveJobs) {
				arrJobId.add(archiveJob.getJob_id());
			}
			modelMap.addAttribute("arrJobId", arrJobId);
		} catch (Exception e) {
			e.printStackTrace();
			modelMap.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/feedIdFilter");
	}

	@RequestMapping(value = { "/schedule/jobIdFilter" }, method = RequestMethod.POST)
	public ModelAndView getAreachart(@Valid @RequestParam("job_id") String job_id,
			@Valid @RequestParam("feed_id") String feed_id, ModelMap modelMap) {
		ModelAndView model = new ModelAndView("schedular/populateChart");

		try {
			ArrayList<ArchiveJobsDTO> arrChartDetails = schedularService.getChartDetails(job_id);
			ArrayList<String> arrBatchDate = new ArrayList<String>();
			ArrayList<String> arrDuration = new ArrayList<String>();

			ObjectMapper mapper = new ObjectMapper();

			for (ArchiveJobsDTO archiveJob : arrChartDetails) {
				arrBatchDate.add(archiveJob.getBatch_date().toString());
				arrDuration.add(archiveJob.getDuration());
			}

			String json = mapper.writeValueAsString(arrBatchDate);
			modelMap.addAttribute("x", json.toString());
			modelMap.addAttribute("y", arrDuration.toString());
			List<ArchiveJobsDTO> arrArchiveTable = schedularService.getRunStats(job_id, feed_id);
			modelMap.addAttribute("allLoadJobs", arrArchiveTable);
		} catch (Exception e) {
			e.printStackTrace();
			modelMap.addAttribute("errorStatus", e.getMessage());
		}
		return model;

	}

	/**
	 * 
	 * @param frequency
	 * @param batchId
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/scheduler/frequency" }, method = RequestMethod.POST)
	public ModelAndView frequencySelect(@Valid @RequestParam("frequency") String frequency,
			@RequestParam("batchId") String batchId, ModelMap modelMap) {
		try {

			List<MasterJobsDTO> dtos = schedularService.typeLoadJobs(frequency, batchId);
			modelMap.addAttribute("allLoadJobs", dtos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ModelAndView("schedular/viewAllJobs1");
	}

	/**
	 * 
	 * @param strFrequency
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = { "/scheduler/batchid" }, method = RequestMethod.POST)

	public ModelAndView batchIdSelect(@Valid @RequestParam("frequency") String frequency,
			@RequestParam("batchId") String batchId, ModelMap modelMap) {
		try {
			List<MasterJobsDTO> dtos = schedularService.typeLoadJobs(frequency, batchId);
			modelMap.addAttribute("allLoadJobs", dtos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ModelAndView("schedular/viewAllJobs1");
	}

	// Current Table
	@RequestMapping(value = { "/scheduler/scheduledjobs" }, method = RequestMethod.GET)
	public ModelAndView allCurrentJobs(ModelMap modelMap, HttpServletRequest request) {
		try {
			HashMap<String, List<DailyJobsDTO>> hsMap = new HashMap<String, List<DailyJobsDTO>>();
			hsMap.put("ALL",
					schedularService.allCurrentJobs((String) request.getSession().getAttribute("project_name")));
			ArrayList<String> arrfeedId = schedularService
					.getFeedFromCurrent((String) request.getSession().getAttribute("project_name"));
			modelMap.addAttribute("arrfeedId", arrfeedId);
			modelMap.addAttribute("user_id", (String) request.getSession().getAttribute("user_name"));
			modelMap.addAttribute("allLoadJobs", hsMap.get("ALL"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ModelAndView("schedular/viewCurrentJobs");
	}

	@RequestMapping(value = { "/scheduler/statusfilter" }, method = RequestMethod.POST)
	public ModelAndView statusFilter(@Valid @RequestParam("status") String status,
			@RequestParam("feedid") String feedid, ModelMap modelMap) {
		try {

			List<DailyJobsDTO> dtos = schedularService.filterCurrentJobs(status, feedid);
			modelMap.addAttribute("allLoadJobs", dtos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ModelAndView("schedular/viewCurrentJobs1");
	}

	@RequestMapping(value = { "/scheduler/feedfilter" }, method = RequestMethod.POST)
	public ModelAndView feedFilter(@Valid @RequestParam("status") String status, @RequestParam("feedid") String feedid,
			ModelMap modelMap) {
		try {

			List<DailyJobsDTO> dtos = schedularService.filterCurrentJobs(status, feedid);
			modelMap.addAttribute("allLoadJobs", dtos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ModelAndView("schedular/viewCurrentJobs1");
	}
	
	

	@RequestMapping(value = { "/scheduler/runScheduleJob" }, method = RequestMethod.POST)
	public ModelAndView runScheduleJob(@Valid @RequestParam("feedId") String feedId,
			@RequestParam("jobId") String jobId, @RequestParam("batchDate") String batchDate, ModelMap modelMap) {
		try {
			String message = schedularService.runScheduleJob(feedId, jobId, batchDate);
			modelMap.addAttribute("successString", message);
		} catch (Exception e) {

			modelMap.addAttribute("errorStatus", e.getMessage());

		}
		return new ModelAndView("schedular/viewCurrentJobs1");
	}

	@RequestMapping(value = { "/scheduler/stopScheduleJob" }, method = RequestMethod.POST)
	public ModelAndView killCurrentJob(@Valid @RequestParam("feedId") String feedId,
			@RequestParam("jobId") String jobId, @RequestParam("batchDate") String batchDate, ModelMap modelMap) {
		try {
			String message = schedularService.killCurrentJob(feedId, jobId, batchDate);
			modelMap.addAttribute("successString", message);
		} catch (Exception e) {
			modelMap.addAttribute("errorStatus", e.getMessage());

		}
		return new ModelAndView("schedular/viewCurrentJobs1");
	}

	@RequestMapping(value = { "/schedular/error" }, method = RequestMethod.GET)
	public ModelAndView error(ModelMap modelMap, HttpServletRequest request) {
		return new ModelAndView("/index");
	}

	@RequestMapping(value = { "/scheduler/AddTask" }, method = RequestMethod.GET)
	public ModelAndView AddTask(ModelMap modelMap, HttpServletRequest request) {
		ArrayList<TaskLocation> dtos = new ArrayList<TaskLocation>();
		try {
			String project_id = (String) request.getSession().getAttribute("project_name");
			ArrayList<BatchDetailsDTO> batch_val1 = schedularService.getCreateBatchDetails(project_id);
			ArrayList<BatchDetailsDTO> batch_val2 = schedularService.getEditBatchDetails(project_id);
			modelMap.addAttribute("batch_val1", batch_val1);
			modelMap.addAttribute("batch_val2", batch_val2);
			ArrayList<String> tproj = schedularService
					.getGoogleProject((String) request.getSession().getAttribute("project_name"));
			modelMap.addAttribute("tproj", tproj);
			ArrayList<String> service_acc = schedularService
					.getServiceAccount((String) request.getSession().getAttribute("project_name"));
			modelMap.addAttribute("service_acc", service_acc);
			ArrayList<String> kafka_topic = schedularService.getKafkaTopic();
			modelMap.addAttribute("kafka_topic", kafka_topic);
//			dtos=schedularService.runUnixCommand("C:\\Users\\Geeta.Puri\\Downloads /b /a-d");
//			modelMap.addAttribute("script_files", dtos);
		} catch (Exception e) {
			modelMap.addAttribute("errorStatus", e.getMessage());

		}
		modelMap.addAttribute("usernm", request.getSession().getAttribute("user_name"));
		modelMap.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
		return new ModelAndView("schedular/AddTask");
	}
	
	@RequestMapping(value = "/scheduler/LoadFileLocation", method = RequestMethod.POST)
	public ModelAndView LoadFileLocation(@Valid @ModelAttribute("batch_id") String batch_id, @ModelAttribute("project_name") String project_name,
			@ModelAttribute("script") String script,@Valid @ModelAttribute("id") String id, ModelMap model, HttpServletRequest request,
			ModelMap modelMap) throws UnsupportedOperationException, Exception {
		ArrayList<TaskLocation> dtos = new ArrayList<TaskLocation>();
		dtos=schedularService.runUnixCommand(script);
		modelMap.addAttribute("script_files", dtos);
		modelMap.addAttribute("id", id);
		return new ModelAndView("schedular/LoadFileLocation");
	}
	
	@RequestMapping(value = "/scheduler/LoadBuckets", method = RequestMethod.POST)
	public ModelAndView LoadBuckets(@Valid @ModelAttribute("gcs_project") String gcs_project, @ModelAttribute("gcs_service_account") String gcs_service_account,
			@ModelAttribute("id") String id,
			 ModelMap model, HttpServletRequest request,
			ModelMap modelMap) throws UnsupportedOperationException, Exception {
		ArrayList<String> arr = schedularService.getBuckets(gcs_project,gcs_service_account);
		modelMap.addAttribute("buckets", arr);
		modelMap.addAttribute("id", id);
		return new ModelAndView("schedular/LoadBuckets");
	}
	
	@RequestMapping(value = "/scheduler/LoadGCS", method = RequestMethod.POST)
	public ModelAndView LoadGCS( @ModelAttribute("id") String id,
			 ModelMap model, HttpServletRequest request,
			ModelMap modelMap) throws UnsupportedOperationException, Exception {
		ArrayList<String> tproj = schedularService
				.getGoogleProject((String) request.getSession().getAttribute("project_name"));
		modelMap.addAttribute("tproj", tproj);
		ArrayList<String> service_acc = schedularService
				.getServiceAccount((String) request.getSession().getAttribute("project_name"));
		modelMap.addAttribute("service_acc", service_acc);
		modelMap.addAttribute("id", id);
		return new ModelAndView("schedular/LoadGCS");
	}
	
	@RequestMapping(value = "/scheduler/LoadBucketFiles", method = RequestMethod.POST)
	public ModelAndView LoadBucketFiles(@Valid @ModelAttribute("gcs_project") String gcs_project, @ModelAttribute("gcs_service_account") String gcs_service_account,
			@ModelAttribute("bucket") String bucket,@ModelAttribute("id") String id,
			 ModelMap model, HttpServletRequest request,
			ModelMap modelMap) throws UnsupportedOperationException, Exception {
		ArrayList<String> arr = schedularService.getBucketFiles(gcs_project,gcs_service_account,bucket);
		modelMap.addAttribute("bucketfiles", arr);
		modelMap.addAttribute("id", id);
		return new ModelAndView("schedular/LoadBucketFiles");
	}

	@RequestMapping(value = "/scheduler/AddTaskSave", method = RequestMethod.POST)
	public ModelAndView AddTaskSave(@Valid @ModelAttribute("x") String x, @ModelAttribute("src_val") String src_val,
			@ModelAttribute("button_type") String button_type, ModelMap model, HttpServletRequest request,
			ModelMap modelMap) throws UnsupportedOperationException, Exception {
		String resp = null;
		try {
			if (button_type.equalsIgnoreCase("create")) {
				resp = schedularService.invokeRest(x, "addScheduleData");
			} else {
				resp = schedularService.invokeRest(x, "editScheduleData");
			}

			String status0[] = resp.toString().split(":");
			String status1[] = status0[1].split(",");
			String status = status1[0].replaceAll("\'", "").trim();
			String message0 = status0[2];
			String message = message0.replaceAll("[\'}]", "").trim();
			String final_message = status + ": " + message;
			if (resp.toLowerCase().contains("success") && button_type.equalsIgnoreCase("create")) {
				modelMap.addAttribute("successString", "Success :Task added successfully");
			} else if (resp.toLowerCase().contains("success") && button_type.equalsIgnoreCase("edit")) {
				modelMap.addAttribute("successString", "Success :Task updated successfully");
			} else {
				if (final_message.contains("ORA-00001")) {
					modelMap.addAttribute("errorString", "Failed :All the job names passed must be unique");
				} else {
					modelMap.addAttribute("errorString", final_message);
				}
			}

			modelMap.addAttribute("usernm", request.getSession().getAttribute("user_name"));
			model.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			String project_id = (String) request.getSession().getAttribute("project_name");
			ArrayList<BatchDetailsDTO> batch_val1 = schedularService.getCreateBatchDetails(project_id);
			modelMap.addAttribute("batch_val1", batch_val1);
			ArrayList<BatchDetailsDTO> batch_val2 = schedularService.getEditBatchDetails(project_id);
			modelMap.addAttribute("batch_val2", batch_val2);
			ArrayList<String> kafka_topic = schedularService.getKafkaTopic();
			modelMap.addAttribute("kafka_topic", kafka_topic);
			ArrayList<String> tproj = schedularService
					.getGoogleProject((String) request.getSession().getAttribute("project_name"));
			modelMap.addAttribute("tproj", tproj);
			ArrayList<String> service_acc = schedularService
					.getServiceAccount((String) request.getSession().getAttribute("project_name"));
			modelMap.addAttribute("service_acc", service_acc);
		} catch (Exception e) {
			modelMap.addAttribute("errorString", e.getMessage());

		}
		return new ModelAndView("schedular/AddTask");
	}

	@RequestMapping(value = { "/scheduler/AddBatch" }, method = RequestMethod.GET)
	public ModelAndView AddBatch1(@Valid ModelMap model, HttpServletRequest request) {
		try {
			model.addAttribute("usernm", request.getSession().getAttribute("user_name"));
			String project_id = (String) request.getSession().getAttribute("project_name");
			ArrayList<BatchDetailsDTO> batch_val = schedularService.getBatchDetails(project_id);
			model.addAttribute("batch_val", batch_val);
			model.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			ArrayList<String> kafka_topic = schedularService.getKafkaTopic();
			model.addAttribute("kafka_topic", kafka_topic);
			// String message = schedularService.killCurrentJob(feedId, jobId, batchDate);
			// modelMap.addAttribute("successString", message);
		} catch (Exception e) {
			model.addAttribute("errorStatus", e.getMessage());

		}
		return new ModelAndView("schedular/AddBatch");
	}

	@RequestMapping(value = "/scheduler/AddBatchClick", method = RequestMethod.POST)
	public ModelAndView AddBatchClick(@Valid @ModelAttribute("x") String x, @ModelAttribute("src_val") String src_val,
			@ModelAttribute("button_type") String button_type, ModelMap model, HttpServletRequest request,
			ModelMap modelMap) throws UnsupportedOperationException, Exception {
		String message = "";
		try {
			if (button_type.equalsIgnoreCase("add")) {
				message = schedularService.invokeRest(x, "saveBatchDetails");
				if (message.toLowerCase().contains("success")) {
					modelMap.addAttribute("successString", "Batch added successfully");
				} else {
					modelMap.addAttribute("errorString", "Batch name already exits,pass a unique name");
				}
			} else {
				message = schedularService.invokeRest(x, "editBatchDetails");
				if (message.toLowerCase().contains("success")) {
					modelMap.addAttribute("successString", "Batch updated successfully");
				} else {
					modelMap.addAttribute("errorString", message);
				}
			}

			modelMap.addAttribute("usernm", request.getSession().getAttribute("user_name"));
			model.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			String project_id = (String) request.getSession().getAttribute("project_name");
			ArrayList<BatchDetailsDTO> batch_val = schedularService.getBatchDetails(project_id);
			model.addAttribute("batch_val", batch_val);
			ArrayList<String> kafka_topic = schedularService.getKafkaTopic();
			model.addAttribute("kafka_topic", kafka_topic);
		} catch (Exception e) {
			modelMap.addAttribute("errorString", e.getMessage());

		}
		return new ModelAndView("schedular/AddBatch");
	}

	@RequestMapping(value = { "/scheduler/CreateSequence" }, method = RequestMethod.GET)
	public ModelAndView CreateSequence(@Valid ModelMap modelMap, HttpServletRequest request) {
		try {
			modelMap.addAttribute("usernm", request.getSession().getAttribute("user_name"));
			modelMap.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			String project_id = (String) request.getSession().getAttribute("project_name");
			ArrayList<BatchDetailsDTO> batch_val1 = schedularService.getAdhocBatchCreateDetails(project_id);
			ArrayList<BatchDetailsDTO> batch_val2 = schedularService.getAdhocBatchEditDetails(project_id);
			modelMap.addAttribute("batch_val1", batch_val1);
			modelMap.addAttribute("batch_val2", batch_val2);

		} catch (Exception e) {
			modelMap.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/CreateSequence");
	}

	@RequestMapping(value = "/scheduler/CreateSequence1", method = RequestMethod.POST)
	public ModelAndView CreateSequence1(@Valid @ModelAttribute("batchid") String batchid,
			@Valid @ModelAttribute("project_id") String project_id, ModelMap model)
			throws ClassNotFoundException, SQLException {
		ArrayList<TaskSequenceDTO> arr;
		try {
			arr = schedularService.getJobDetails(batchid, project_id);
			model.addAttribute("daglistdto", arr);
			model.addAttribute("5", "TaskTest");
		} catch (Exception e) {
			model.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/CreateSequence1");
	}

	@RequestMapping(value = "/scheduler/EditSequence", method = RequestMethod.POST)
	public ModelAndView EditSequence(@Valid @ModelAttribute("batchid") String batchid,
			@Valid @ModelAttribute("project_id") String project_id, ModelMap model)
			throws ClassNotFoundException, SQLException {

		try {
			String xtest = "";
			xtest = schedularService.getBatchSequence(batchid, project_id, null, 0);
			model.addAttribute("xtest", xtest);

		} catch (Exception e) {
			model.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/EditSequence");
	}

	@RequestMapping(value = "/scheduler/CreateSequenceSubmit", method = RequestMethod.POST)
	public ModelAndView CreateSequenceSubmit(@Valid @ModelAttribute("x") String x, ModelMap modelMap,
			HttpServletRequest request) throws UnsupportedOperationException, Exception {
		try {
			String resp = schedularService.invokeRest(x, "sequenceSubmit");
			String status0[] = resp.toString().split(":");
			String status1[] = status0[1].split(",");
			String status = status1[0].replaceAll("\'", "").trim().replaceAll("\"", "");
			String message0 = status0[2];
			String message = message0.replaceAll("[\'}]", "").trim().replaceAll("\"", "");
			String final_message = status + ": " + message;
			if (resp.toLowerCase().contains("success")) {
				modelMap.addAttribute("successString", final_message);
			} else {
				modelMap.addAttribute("errorString", final_message);
			}
			modelMap.addAttribute("usernm", request.getSession().getAttribute("user_name"));
			modelMap.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			String project_id = (String) request.getSession().getAttribute("project_name");
			ArrayList<BatchDetailsDTO> batch_val1 = schedularService.getAdhocBatchCreateDetails(project_id);
			ArrayList<BatchDetailsDTO> batch_val2 = schedularService.getAdhocBatchEditDetails(project_id);
			modelMap.addAttribute("batch_val1", batch_val1);
			modelMap.addAttribute("batch_val2", batch_val2);
		} catch (Exception e) {
			modelMap.addAttribute("errorString", e.getMessage());
		}
		return new ModelAndView("schedular/CreateSequence");
	}
	
	
	@RequestMapping(value = "/scheduler/CreateBatchSequenceSubmit", method = RequestMethod.POST)
	public ModelAndView CreateBatchSequenceSubmit(@Valid @ModelAttribute("x") String x, ModelMap modelMap,
			HttpServletRequest request) throws UnsupportedOperationException, Exception {
		ArrayList<BatchFlowDTO> dto;
		ArrayList<TaskSequenceDTO> arr;
		try {
			String resp = schedularService.invokeRest(x, "batchSequenceSubmit");
			String status0[] = resp.toString().split(":");
			String status1[] = status0[1].split(",");
			String status = status1[0].replaceAll("\'", "").trim().replaceAll("\"", "");
			String message0 = status0[2];
			String message = message0.replaceAll("[\'}]", "").trim().replaceAll("\"", "");
			String final_message = status + ": " + message;
			if (resp.toLowerCase().contains("success")) {
				modelMap.addAttribute("successString", final_message);
			} else {
				modelMap.addAttribute("errorString", final_message);
			}
			modelMap.addAttribute("usernm", request.getSession().getAttribute("user_name"));
			modelMap.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			String project_id = (String) request.getSession().getAttribute("project_name");
			dto=schedularService.getBatchFlow(project_id);
			modelMap.addAttribute("flow1", dto);
			arr = schedularService.getBatchDetails1( project_id,"R");
			modelMap.addAttribute("reg1", arr);
		} catch (Exception e) {
			modelMap.addAttribute("errorString", e.getMessage());
		}
		return new ModelAndView("schedular/CreateBatchSequence");
	}

	@RequestMapping(value = "scheduler/BatchEdit", method = RequestMethod.POST)
	public ModelAndView BatchEdit(@Valid @ModelAttribute("batch_id") String batch_id,
			@Valid @ModelAttribute("project_id") String project_id, ModelMap model, HttpServletRequest request)
			throws UnsupportedOperationException, Exception {
		try {
			String adhoc_flag = "";
			String regular_flag = "";
			String event_flag = "";

			BatchTableDetailsDTO batchArr = schedularService.extractBatchDetails(batch_id, project_id);
			if (batchArr.getSCHEDULE_TYPE().contains("R") && batchArr.getDAILY_FLAG() != null) {
				adhoc_flag = "The batch is regular type and scheduled everyday at " + batchArr.getJOB_SCHEDULE_TIME();
			} else if (batchArr.getSCHEDULE_TYPE().contains("R") && batchArr.getHOURLY_FLAG() != null) {
				adhoc_flag = "The batch is regular type and scheduled every hour at " + batchArr.getJOB_SCHEDULE_TIME();
			}
			else if (batchArr.getSCHEDULE_TYPE().contains("R") && batchArr.getWEEKLY_FLAG() != null) {
				adhoc_flag = "The batch is regular type and scheduled every week at " + batchArr.getJOB_SCHEDULE_TIME()
						+ " on " + batchArr.getWEEK_RUN_DAY();
			} else if (batchArr.getSCHEDULE_TYPE().contains("R") && batchArr.getMONTHLY_FLAG() != null) {
				adhoc_flag = "The batch is regular type and scheduled every month at " + batchArr.getJOB_SCHEDULE_TIME()
						+ " on " + batchArr.getMONTH_RUN_DAY();
			} else if (batchArr.getSCHEDULE_TYPE().contains("O") && batchArr.getArgument_4() == null) {
				adhoc_flag = "The batch is adhoc type and scheduled everyday at 00:00";
			} else if (batchArr.getSCHEDULE_TYPE().contains("F") && batchArr.getArgument_4() != null) {
				adhoc_flag = "The batch is event based type and scheduled everyday at 00:00 with filewatcher as "
						+ batchArr.getArgument_4();
			} else if (batchArr.getSCHEDULE_TYPE().contains("K") && batchArr.getArgument_4() != null) {
				adhoc_flag = "The batch is event based type and scheduled everyday at 00:00 with Kafka topic as "
						+ batchArr.getArgument_4();
			} else if (batchArr.getSCHEDULE_TYPE().contains("A") && batchArr.getArgument_4() != null) {
				adhoc_flag = "The batch is event based type and scheduled everyday at 00:00 with API value as "
						+ batchArr.getArgument_4();
			}
			model.addAttribute("adhoc_flag", adhoc_flag);
			model.addAttribute("regular_flag", regular_flag);
			model.addAttribute("event_flag", event_flag);
			model.addAttribute("batchArr", batchArr);
			ArrayList<String> kafka_topic = schedularService.getKafkaTopic();
			model.addAttribute("kafka_topic", kafka_topic);
		} catch (Exception e) {
			model.addAttribute("errorString", e.getMessage());
		}
		return new ModelAndView("schedular/BatchEdit");
	}

	@RequestMapping(value = "scheduler/LoadBatchJobs", method = RequestMethod.POST)
	public ModelAndView LoadBatchJobs(@Valid @ModelAttribute("batch") String batch_id,
			@Valid @ModelAttribute("project") String project_id, ModelMap model, HttpServletRequest request)
			throws UnsupportedOperationException, Exception {
		try {
			ArrayList<String> job_id1 = schedularService.getBatchJobs(batch_id, project_id);
			model.addAttribute("job_id1", job_id1);
		} catch (Exception e) {
			model.addAttribute("errorString", e.getMessage());
		}
		return new ModelAndView("schedular/LoadBatchJobs");
	}

	@RequestMapping(value = "scheduler/EditJob", method = RequestMethod.POST)
	public ModelAndView EditJob(@Valid @ModelAttribute("batch") String batch_id,
			@Valid @ModelAttribute("project") String project_id, @Valid @ModelAttribute("job_id") String job_id,
			ModelMap model, HttpServletRequest request) throws UnsupportedOperationException, Exception {
		try {
			AdhocJobDTO jobArr = schedularService.extractBatchJobDetails(batch_id, project_id, job_id);
			String script1 = jobArr.getCommand().substring(0, jobArr.getCommand().lastIndexOf('/') + 1);
			String command1 = jobArr.getCommand().substring(jobArr.getCommand().lastIndexOf('/') + 1);
			model.addAttribute("script1", script1);
			model.addAttribute("command1", command1);
			model.addAttribute("jobArr", jobArr);
		} catch (Exception e) {
			model.addAttribute("errorString", e.getMessage());
		}
		return new ModelAndView("schedular/EditJob");
	}

	@RequestMapping(value = { "scheduler/ArchiveJobs"}, method = RequestMethod.GET)
	public ModelAndView ArchiveJobs(ModelMap modelMap,HttpServletRequest request) {
		/*try {
			HashMap<String, List<MasterJobDTO>> hsMap = new HashMap<String, List<MasterJobDTO>>();
			hsMap.put("ALL", adminService.allLoadJobs((String)request.getSession().getAttribute("project_name")));
		ArrayList<String> arrfeedId = adminService.getFeedFromMaster((String)request.getSession().getAttribute("project_name"));
		modelMap.addAttribute("arrfeedId", arrfeedId);
		modelMap.addAttribute("allLoadJobs", hsMap.get("ALL"));
	} catch (Exception e) {
		e.printStackTrace();*/
	
		return new ModelAndView("scheduler/ArchiveJobs");
	}
	
	
	@RequestMapping(value = "/scheduler/CreateBatchSequence", method = RequestMethod.GET)
	public ModelAndView CreateBatchSequence(@Valid ModelMap modelMap,ModelMap model,HttpServletRequest request)
			throws ClassNotFoundException, SQLException {
		ArrayList<BatchFlowDTO> dto=null;
		ArrayList<TaskSequenceDTO> arr;
		
		try {
			modelMap.addAttribute("usernm", request.getSession().getAttribute("user_name"));
			modelMap.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			String project_id = (String) request.getSession().getAttribute("project_name");
			dto=schedularService.getBatchFlow(project_id);
			model.addAttribute("flow1", dto);
			arr = schedularService.getBatchDetails1( project_id,"R");
			model.addAttribute("reg1", arr);
		} catch (Exception e) {
			model.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/CreateBatchSequence");
	}
	
	@RequestMapping(value = "/scheduler/BatchEditSequence1", method = RequestMethod.POST)
	public ModelAndView BatchEditSequence1(@Valid @ModelAttribute("flowname") String flowname,@Valid @ModelAttribute("project_id") String project_id,@Valid ModelMap modelMap,ModelMap model,HttpServletRequest request)
			throws ClassNotFoundException, SQLException {
		BatchFlowDTO dto=new BatchFlowDTO();
		ArrayList<TaskSequenceDTO> arr;
		try {
			modelMap.addAttribute("usernm", request.getSession().getAttribute("user_name"));
			modelMap.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			dto=schedularService.getBatchFlowDetails(project_id,flowname);
			String xtest=dto.getBATCH_SEQUENCE();
			String[] flowdetails=xtest.split("\\|");
			String pattern="";
			int j=0;
			for(int i=0; i< flowdetails.length;i++) {
				pattern+=flowdetails[i].split("\\,")[0]+","+j+"|";
				j=j+10;
			}
			String schedule_type=dto.getBATCH_TYPE();
			if(schedule_type.equalsIgnoreCase("RE")) {
				schedule_type="E";
			}
			arr = schedularService.getBatchDetails1( project_id,schedule_type);	
			model.addAttribute("daglistdto", arr);
			modelMap.addAttribute("xtest", pattern);
			
		} catch (Exception e) {
			model.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/BatchEditSequence1");
	}
	
	
	
	@RequestMapping(value = "/scheduler/CreateBatchSequence2", method = RequestMethod.POST)
	public ModelAndView CreateBatchSequence(@Valid ModelMap modelMap,@Valid @ModelAttribute("schtype") String schtype,ModelMap model,HttpServletRequest request)
			throws ClassNotFoundException, SQLException {
		
		ArrayList<TaskSequenceDTO> arr;
		try {
			modelMap.addAttribute("project", (String) request.getSession().getAttribute("project_name"));
			String project_id = (String) request.getSession().getAttribute("project_name");
			if(schtype.equalsIgnoreCase("RE")) {
				schtype="E";
			}
			arr = schedularService.getBatchDetails1( project_id,schtype);
			model.addAttribute("daglistdto", arr);
			model.addAttribute("5", "TaskTest");
		} catch (Exception e) {
			model.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/CreateBatchSequence2");
	}
		
	@RequestMapping(value = "/scheduler/CreateBatchSequence3", method = RequestMethod.POST)
	public ModelAndView CreateBatchSequence1(@Valid @ModelAttribute("batch_id") String batch_id,
			@Valid @ModelAttribute("project_id") String project_id,ModelMap model, HttpServletRequest request)
					throws UnsupportedOperationException, Exception  {
		ArrayList<BatchSequenceDTO> arr;
		
		try {
			arr = schedularService.getBatchDetails2(batch_id, project_id);
			model.addAttribute("daglistdto1", arr);
			model.addAttribute("5", "TaskTest");
		} catch (Exception e) {
			model.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/CreateBatchSequence3");
	}
	
	@RequestMapping(value = "/scheduler/BatchEditSequence2", method = RequestMethod.POST)
	public ModelAndView BatchEditSequence2(@Valid @ModelAttribute("batch_id") String batch_id,
			@Valid @ModelAttribute("project_id") String project_id,@Valid @ModelAttribute("flowname") String flowname,ModelMap model, HttpServletRequest request)
					throws UnsupportedOperationException, Exception  {
		ArrayList<BatchSequenceDTO> arr;
		BatchFlowDTO dto=new BatchFlowDTO();
		try {
			arr = schedularService.getBatchDetails2(batch_id, project_id);
			dto=schedularService.getBatchFlowDetails(project_id,flowname);
			String sequence=dto.getBATCH_SEQUENCE();
			ArrayList<BatchSequenceDTO> dtos=schedularService.getBatchSequence(sequence);
			String xtest=dto.getBATCH_SEQUENCE();
			String[] flowdetails=xtest.split("\\|");
			String pattern="";
			int j=0;
			for(int i=0; i< flowdetails.length;i++) {
				pattern+=flowdetails[i].split("\\,")[0]+","+j+"|";
				j=j+10;
			}
			model.addAttribute("daglistdto2", dtos);
			model.addAttribute("daglistdto1", arr);
			model.addAttribute("5", "TaskTest");
			model.addAttribute("xtest", pattern);
		} catch (Exception e) {
			model.addAttribute("errorStatus", e.getMessage());
		}
		return new ModelAndView("schedular/BatchEditSequence2");
	}
	
	
	
}
