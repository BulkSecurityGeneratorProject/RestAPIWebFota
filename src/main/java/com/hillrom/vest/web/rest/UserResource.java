package com.hillrom.vest.web.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import com.hillrom.vest.domain.Notification;
import com.hillrom.vest.domain.PatientCompliance;
import com.hillrom.vest.domain.PatientProtocolData;
import com.hillrom.vest.domain.PatientVestDeviceData;
import com.hillrom.vest.domain.PatientVestDeviceHistory;
import com.hillrom.vest.domain.ProtocolConstants;
import com.hillrom.vest.domain.TherapySession;
import com.hillrom.vest.domain.User;
import com.hillrom.vest.exceptionhandler.HillromException;
import com.hillrom.vest.repository.NotificationRepository;
import com.hillrom.vest.repository.PatientComplianceRepository;
import com.hillrom.vest.repository.PatientVestDeviceDataRepository;
import com.hillrom.vest.repository.TherapySessionRepository;
import com.hillrom.vest.repository.UserRepository;
import com.hillrom.vest.repository.UserSearchRepository;
import com.hillrom.vest.security.AuthoritiesConstants;
import com.hillrom.vest.security.SecurityUtils;
import com.hillrom.vest.service.AdherenceCalculationService;
import com.hillrom.vest.service.PatientHCPService;
import com.hillrom.vest.service.PatientProtocolService;
import com.hillrom.vest.service.PatientVestDeviceService;
import com.hillrom.vest.service.TherapySessionService;
import com.hillrom.vest.service.UserService;
import com.hillrom.vest.service.util.CsvUtil;
import com.hillrom.vest.util.ExceptionConstants;
import com.hillrom.vest.util.MessageConstants;
import com.hillrom.vest.web.rest.dto.PatientUserVO;
import com.hillrom.vest.web.rest.dto.ProtocolDTO;
import com.hillrom.vest.web.rest.dto.StatisticsVO;
import com.hillrom.vest.web.rest.dto.TherapyDataVO;
import com.hillrom.vest.web.rest.dto.TreatmentStatisticsVO;
import com.hillrom.vest.web.rest.util.PaginationUtil;
/**
 * REST controller for managing users.
 */
@RestController
@RequestMapping("/api")
public class UserResource {

	private final Logger log = LoggerFactory.getLogger(UserResource.class);

	@Inject
	private UserRepository userRepository;
	
	@Inject
	private UserSearchRepository userSearchRepository;
	
	@Inject
	private UserService userService;
	
	@Inject
	private PatientVestDeviceService patientVestDeviceService;

	@Inject
	private PatientProtocolService patientProtocolService;

	@Inject
	private TherapySessionService therapySessionService;
	
	@Inject
	private AdherenceCalculationService adherenceCalculationService;
	
	@Inject
	private PatientComplianceRepository complianceRepository;
	
	@Inject
	private NotificationRepository notificationRepository;

	@Inject
	private TherapySessionRepository therapySessionRepository;

	@Inject
	private PatientVestDeviceDataRepository deviceDataRepository;
	
	@Inject
    private PatientHCPService patientHCPService;
	
	/**
	 * GET /users -> get all users.
	 */
	@RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	
	public List<User> getAll() {
		log.debug("REST request to get all Users");
		return userRepository.findAll();
	}

	/**
	 * GET /users/:login -> get the "login" user.
	 */
	@RequestMapping(value = "/users/{email}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	
	ResponseEntity<User> getUser(@PathVariable String email) {
		log.debug("REST request to get User : {}", email);
		return userRepository.findOneByEmail(email)
				.map(user -> new ResponseEntity<>(user, HttpStatus.OK))
				.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	@RequestMapping(value = "/user/patient/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	
	public ResponseEntity<?> searchHcp(
			@RequestParam(required = true, value = "searchString") String searchString,
			@RequestParam(value = "page", required = false) Integer offset,
			@RequestParam(value = "per_page", required = false) Integer limit,
			@RequestParam(value = "sort_by", required = false) String sortBy,
			@RequestParam(value = "asc", required = false) Boolean isAscending)
			throws URISyntaxException {
		if(searchString.endsWith("_")){
 		   searchString = searchString.replace("_", "\\\\_");
		}
		String queryString = new StringBuilder("'%").append(searchString)
				.append("%'").toString();
		Map<String, Boolean> sortOrder = new HashMap<>();
		if (StringUtils.isNotBlank(sortBy)) {
			isAscending = (isAscending != null) ? isAscending : true;
			if(sortBy.equalsIgnoreCase("email"))
				sortOrder.put("user." + sortBy, isAscending);
			else	
				sortOrder.put(sortBy, isAscending);
		}
		Page<PatientUserVO> page = userSearchRepository.findPatientBy(
				queryString, PaginationUtil.generatePageRequest(offset, limit),
				sortOrder);
		HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
				page, "/user/patient/search", offset, limit);
		return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);

	}

	@RequestMapping(value = "/user/{id}/patient", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	
	public ResponseEntity<PatientUserVO> getPatientUser(@PathVariable Long id) {
		log.debug("REST request to get PatientUser : {}", id);
		Optional<PatientUserVO> patientUser = userService.getPatientUser(id);
		if(patientUser.isPresent()){
			return new ResponseEntity<>(patientUser.get(),
					HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	/**
     * PUT  /patient/:id/linkvestdevice -> link vest device with patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/linkvestdevice",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> linkVestDeviceWithPatient(@PathVariable Long id, @RequestBody Map<String, Object> deviceData) {
    	log.debug("REST request to link vest device with patient user : {}", id);
        JSONObject jsonObject = new JSONObject();
		try {
			Object responseObj = patientVestDeviceService.linkVestDeviceWithPatient(id, deviceData);
			if (responseObj instanceof User) {
				jsonObject.put("ERROR", ExceptionConstants.HR_572);
				jsonObject.put("user", (User) responseObj);
				return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
			} else {
				jsonObject.put("message", MessageConstants.HR_282);
				jsonObject.put("user", (PatientVestDeviceHistory) responseObj);
				return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
			}
		} catch (HillromException e) {
			jsonObject.put("ERROR",e.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
		}
    }
    
    /**
     * GET  /patient/:id/vestdevice -> get linked vest device with patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/vestdevice",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES, AuthoritiesConstants.PATIENT})
    public ResponseEntity<JSONObject> getLinkedVestDeviceWithPatient(@PathVariable Long id) {
    	log.debug("REST request to link vest device with patient user : {}", id);
    	JSONObject jsonObject = new JSONObject();
		try {
			List<PatientVestDeviceHistory> deviceList = patientVestDeviceService.getLinkedVestDeviceWithPatient(id);
			if(deviceList.isEmpty()){
     			jsonObject.put("message",MessageConstants.HR_281); //No device linked with patient.
     		} else {
     			jsonObject.put("message", MessageConstants.HR_282);//Vest devices linked with patient fetched successfully.
     			jsonObject.put("deviceList", deviceList);
     		}
			return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
		} catch (HillromException e) {
			jsonObject.put("ERROR",e.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
		}
    }
    
    /**
     * DELETE  /patient/:id/deactivatevestdevice/:serialNumber -> deactivate vest device with {serialNumber} from patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/deactivatevestdevice/{serialNumber}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> deactivateVestDeviceFromPatient(@PathVariable Long id, @PathVariable String serialNumber) {
    	log.debug("REST request to deactivate vest device with serial number {} from patient user : {}", serialNumber, id);
    	JSONObject jsonObject = new JSONObject();
    	try {
			String message = patientVestDeviceService.deactivateVestDeviceFromPatient(id, serialNumber);
			if (StringUtils.isBlank(message)) {
				jsonObject.put("ERROR", ExceptionConstants.HR_573);
	        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
	        } else {
	        	jsonObject.put("message", message);
	            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
	        }
		} catch (HillromException e) {
			jsonObject.put("ERROR", e.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
		}
    }
	
	@RequestMapping(value="/user/{id}/changeSecurityQuestion",method=RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JSONObject> updateSecurityQuestion(@PathVariable Long id,@RequestBody(required=true)Map<String,String> params){
		log.debug("REST request to update Security Question and Answer {}",id,params);
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = userService.updateSecurityQuestion(id,params);
			jsonObject.put("message", MessageConstants.HR_295);
		} catch (HillromException e) {
			jsonObject.put("ERROR",e.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject,HttpStatus.BAD_REQUEST);
		}
		if(jsonObject.containsKey("ERROR")){
			return new ResponseEntity<JSONObject>(jsonObject,HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<JSONObject>(jsonObject,HttpStatus.OK);
	}
	
	/**
     * POST  /patient/:id/protocol -> add protocol with patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/protocol",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> addProtocolToPatient(@PathVariable Long id, @RequestBody ProtocolDTO protocolDTO) {
    	log.debug("REST request to add protocol with patient user : {}", id);
    	JSONObject jsonObject = new JSONObject();
    	try {
    		List<PatientProtocolData> protocolList = patientProtocolService.addProtocolToPatient(id, protocolDTO);
	    	if (protocolList.isEmpty()) {
	        	jsonObject.put("message", ExceptionConstants.HR_559);
	        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_241);
	        	jsonObject.put("protocol", protocolList);
	            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.CREATED);
	        }
    	} catch(HillromException hre){
    		jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
    	}
    }
    
    /**
     * PUT  /patient/:id/protocol -> update protocol with patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/protocol",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> updateProtocolToPatient(@PathVariable Long id, @RequestBody List<PatientProtocolData> ppdList) {
    	log.debug("REST request to update protocol with patient user : {}", id);
    	JSONObject jsonObject = new JSONObject();
    	try {
    		List<PatientProtocolData> protocolList = patientProtocolService.updateProtocolToPatient(id, ppdList);
	    	if (protocolList.isEmpty()) {
	        	jsonObject.put("ERROR", ExceptionConstants.HR_560);
	        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_242);
	        	jsonObject.put("protocol", protocolList);
	            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
	        }
    	} catch(HillromException hre){
    		jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
    	}
    }
    
    /**
     * GET  /patient/:id/protocol -> get all protocol for patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/protocol",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> getAllProtocolsAssociatedWithPatient(@PathVariable Long id) {
    	log.debug("REST request to get protocol for patient user : {}", id);
    	JSONObject jsonObject = new JSONObject();
    	try {
    		List<PatientProtocolData> protocolList = patientProtocolService.getAllProtocolsAssociatedWithPatient(id);
    		if (protocolList.isEmpty()) {
	        	jsonObject.put("message", MessageConstants.HR_245);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_243);
	        	jsonObject.put("protocol", protocolList);
	        }
    		return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
    	} catch(HillromException hre){
    		jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
    	}
    }
    
    /**
     * GET  /patient/:id/protocol/:protocolId -> get protocol details with {protocolId} for patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/protocol/{protocolId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> getProtocolDetails(@PathVariable Long id, @PathVariable String protocolId) {
    	log.debug("REST request to get protocol details with {} for patient user : {}", protocolId, id);
    	JSONObject jsonObject = new JSONObject();
    	try {
    		List<PatientProtocolData> protocolList = patientProtocolService.getProtocolDetails(id, protocolId);
    		if (protocolList.isEmpty()) {
	        	jsonObject.put("ERROR", ExceptionConstants.HR_551);
	        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_243);
	        	jsonObject.put("protocol", protocolList);
	            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
	        }
    	} catch(HillromException hre){
    		jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
    	}
    }
    
    /**
     * DELETE  /patient/:id/protocol -> delete protocol for patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/protocol/{protocolId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> deleteProtocolForPatient(@PathVariable Long id, @PathVariable String protocolId) {
    	log.debug("REST request to delete protocol for patient user : {}", id);
    	JSONObject jsonObject = new JSONObject();
    	try {
	    	String message = patientProtocolService.deleteProtocolForPatient(id, protocolId);
	        if (Objects.isNull(message)) {
	        	jsonObject.put("message", MessageConstants.HR_245);
	        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
	        } else {
	        	jsonObject.put("message", message);
	            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
	        }
    	} catch(HillromException hre){
    		jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
    	}
    }
    
    
    @RequestMapping(value = "/users/{id}/therapyData",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JSONObject> getTherapyByPatientUserIdAndDate(@PathVariable Long id,
    		@RequestParam(value="from",required=false)@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate from,
    		@RequestParam(value="to",required=false)@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate to,
    		@RequestParam(required=false)String groupBy,
    		@RequestParam(value="date",required=false)@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate date){
    	JSONObject jsonObject = new JSONObject();
    	if(Objects.nonNull(date)){
    		List<TherapySession> therapySessions = therapySessionService.findByPatientUserIdAndDate(id, date);
    		if(therapySessions.size() > 0){
    			ProtocolConstants protocol = adherenceCalculationService.getProtocolByPatientUserId(id);
    			jsonObject.put("recommended", protocol);
    			jsonObject.put("actual", therapySessions);
    		}
    		return new ResponseEntity<>(jsonObject,HttpStatus.OK);
    	}else if(Objects.nonNull(from) && Objects.nonNull(to) && Objects.nonNull(groupBy) ){
    		List<TherapyDataVO> therapyData = therapySessionService.findByPatientUserIdAndDateRange(id, from, to, groupBy);
    		if(therapyData.size() > 0){
    			ProtocolConstants protocol = adherenceCalculationService.getProtocolByPatientUserId(id);
    			jsonObject.put("recommended", protocol);
    			jsonObject.put("actual", therapyData);
    		}
    		return new ResponseEntity<>(jsonObject,HttpStatus.OK);
    	}else{
    		jsonObject.put("ERROR", "Required Params missing : [date or from&to&groupBy]");
    		return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
    	}
    }
    
    @RequestMapping(value = "/users/{id}/compliance",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientCompliance> getComplianceScoreByPatientUserIdAndDate(@PathVariable Long id,
    		@RequestParam(value="date",required=false)Long timestamp){
    	LocalDate date = null;
    	if(Objects.isNull(timestamp)){
    		date = LocalDate.now();
    	}else{
    		date = LocalDate.fromDateFields(new Date(timestamp));
    	}
    	PatientCompliance compliance = complianceRepository.findByPatientUserIdAndDate(id, date);
    	if(Objects.nonNull(compliance))
    		return new ResponseEntity<>(compliance,HttpStatus.OK);
    	else
    		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/users/{id}/notifications",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Notification>> getNotificationsByPatientUserId(@PathVariable Long id,
    		@RequestParam(value="from",required=false)Long from,
    		@RequestParam(value="to",required=false)Long to,
    		@RequestParam(value = "page" , required = false) Integer offset,
            @RequestParam(value = "per_page", required = false) Integer limit) throws URISyntaxException{
    	
    	LocalDate fromDate = Objects.isNull(from) ? LocalDate.now().minusDays(1) : LocalDate.fromDateFields(new Date(from));
    	LocalDate toDate = Objects.isNull(to) ? LocalDate.now() : LocalDate.fromDateFields(new Date(to));
    	Pageable pageable = PaginationUtil.generatePageRequest(offset, limit);
    	Page<Notification> page = notificationRepository.findByPatientUserIdAndDateBetweenAndIsAcknowledged(id, fromDate,toDate,false, pageable);
    	HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/users/"+id+"/notifications", offset, limit);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/users/{userId}/notifications/{id}",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JSONObject> acknowledgeNotification(@PathVariable Long userId,
    		@PathVariable Long id,@RequestBody(required=true)Map<String,String> params){
    	JSONObject json = new JSONObject();
    	Optional<Notification> notificationFromDB = Optional.of(notificationRepository.findOne(id));
	    	if(notificationFromDB.isPresent()){
	    		Notification notification = notificationFromDB.get();
	    		if(notification.getPatientUser().getId().equals(userId) 
	    				&& SecurityUtils.getCurrentLogin().equalsIgnoreCase(notification.getPatientUser().getEmail())){
	    			boolean isAcknowledged  = "TRUE".equalsIgnoreCase(params.get("isAcknowledged")) ? true : false;
	    			notification.setAcknowledged(isAcknowledged);
	    			notificationRepository.save(notification);
	    			json.put("notification", notification);
	    			return new ResponseEntity<>(json,HttpStatus.OK);
	    		}else{
	    			json.put("ERROR", ExceptionConstants.HR_403);
	    			return new ResponseEntity<>(json,HttpStatus.FORBIDDEN);
	    		}
	    	}else{
	    		json.put("ERROR", ExceptionConstants.HR_591);
	    		return new ResponseEntity<>(json,HttpStatus.NOT_FOUND);
	    	}    
    	}
    
    /**
     * PUT  /user/:id/notifications -> update HRM notification setting for user  {id}.
     */
    @RequestMapping(value = "/users/{id}/notificationsetting",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.PATIENT})
    public ResponseEntity<JSONObject> updateHRMNotification(@PathVariable Long id, @RequestBody Map<String, Boolean> paramsMap) {
    	JSONObject json = new JSONObject();
    	try {
			json.put("user", userService.setHRMNotificationSetting(id, paramsMap));
			return new ResponseEntity<>(json,HttpStatus.OK);
		} catch (HillromException e) {
			json.put("ERROR", e.getMessage());
			return new ResponseEntity<>(json,HttpStatus.NOT_FOUND);
		}
    }
    
    @RequestMapping(value = "/users/{id}/missedTherapyCount",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JSONObject> getMissedTherapyCount(@PathVariable Long id){
    	JSONObject json = new JSONObject();
    	json.put("count",therapySessionService.getMissedTherapyCountByPatientUserId(id));
    	return new ResponseEntity<JSONObject>(json, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/users/{id}/exportTherapyData",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TherapySession>> exportTherapyData(@PathVariable Long id,
    		@RequestParam(value="from",required=true)Long fromTimeStamp,
    		@RequestParam(value="to",required=false)Long toTimeStamp){
    	LocalDate to = Objects.nonNull(toTimeStamp) ? LocalDate.fromDateFields(new Date(toTimeStamp)) : LocalDate.now();
    	List<TherapySession> therapySessions = therapySessionRepository.findByPatientUserIdAndDateRange(id, LocalDate.fromDateFields(new Date(fromTimeStamp)), to);
    	return new ResponseEntity<>(therapySessions,HttpStatus.OK);
    }
    
    @RequestMapping(value = "/users/{id}/exportTherapyDataCSV",
            method = RequestMethod.GET,
            produces = "text/csv")
    public void exportTherapyDataCSV(@PathVariable Long id,
    		@RequestParam(value="from",required=true)Long fromTimeStamp,
    		@RequestParam(value="to",required=false)Long toTimeStamp,
    		HttpServletResponse response) throws UnsupportedEncodingException, IOException{
    	LocalDate to = Objects.nonNull(toTimeStamp) ? LocalDate.fromDateFields(new Date(toTimeStamp)) : LocalDate.now();
    	List<TherapySession> therapySessions = therapySessionRepository.findByPatientUserIdAndDateRange(id, LocalDate.fromDateFields(new Date(fromTimeStamp)), to);
    	ICsvBeanWriter beanWriter = null;
    	CellProcessor[] processors = CsvUtil.getCellProcessorForTherapySessionData();
    	try {
            beanWriter = new CsvBeanWriter(response.getWriter(),
                    CsvPreference.STANDARD_PREFERENCE);
			String[] header = CsvUtil.getHeaderValuesForTherapySessionCSV();
			String[] headerMapping = CsvUtil.getHeaderMappingForTherapySessionData();
            if(therapySessions.size() > 0 ){
            	beanWriter.writeHeader(header);
                for (TherapySession session : therapySessions) {
                    beanWriter.write(session, headerMapping,processors);
                }
            }else{
            	response.setStatus(204);
            }
        } catch (Exception ex) {
        	response.setStatus(500);
        } finally {
            if (beanWriter != null) {
                try {
                    beanWriter.close();
                } catch (IOException ex) {
                	response.setStatus(500);
                }
            }
        }
    }

    @RequestMapping(value = "/users/{id}/exportVestDeviceData",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> exportVestDeviceData(
			@PathVariable Long id,
			@RequestParam(value="from",required=true)Long from,
			@RequestParam(value="to",required=false)Long to) {
		to = Objects.nonNull(to)?to:new Date().getTime();
		List<PatientVestDeviceData> vestDeviceData = deviceDataRepository.findByPatientUserIdAndTimestampBetween(id, from, to);
		return new ResponseEntity<>(vestDeviceData,HttpStatus.OK);
	}
	
	@RequestMapping(value = "/users/{id}/exportVestDeviceDataCSV",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
	public void exportVestDeviceDataCSV(
			@PathVariable Long id,
			@RequestParam(value="from",required=true)Long from,
			@RequestParam(value="to",required=false)Long to,
			HttpServletResponse response) {
		to = Objects.nonNull(to)?to:new Date().getTime();
		List<PatientVestDeviceData> vestDeviceData = deviceDataRepository.findByPatientUserIdAndTimestampBetween(id, from, to);
		ICsvBeanWriter beanWriter = null;
    	CellProcessor[] processors = CsvUtil.getCellProcessorForVestDeviceData();
    	try {
            beanWriter = new CsvBeanWriter(response.getWriter(),
                    CsvPreference.STANDARD_PREFERENCE);
			String[] header = CsvUtil.getHeaderValuesForVestDeviceDataCSV();
			String[] headerMapping = CsvUtil.getHeaderMappingForVestDeviceData();
            if(vestDeviceData.size() > 0 ){
            	beanWriter.writeHeader(header);
                for (PatientVestDeviceData deviceData : vestDeviceData) {
                    beanWriter.write(deviceData, headerMapping,processors);
                }
            }else{
            	response.setStatus(204);
            }
        } catch (Exception ex) {
        	response.setStatus(500);
        } finally {
            if (beanWriter != null) {
                try {
                    beanWriter.close();
                } catch (IOException ex) {
                	response.setStatus(500);
                }
            }
        }
	}
	
	/**
     * GET  /users/:userId/clinics/:clinicId/statistics -> get the patient statistics for clinic associated with user.
     */
    @RequestMapping(value = "/users/{userId}/clinics/{clinicId}/statistics",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.HCP, AuthoritiesConstants.CLINIC_ADMIN})
    public ResponseEntity<?> getPatientStatisticsForClinicAssociated(@PathVariable Long userId, @PathVariable String clinicId) {
        log.debug("REST request to get patient statistics for clinic {} associated with User : {}", clinicId, userId);
        JSONObject jsonObject = new JSONObject();
        try {
        	LocalDate date = LocalDate.now();
        	Map<String, Object> statitics = patientHCPService.getTodaysPatientStatisticsForClinicAssociatedWithHCP(userId, clinicId, date);
	        if (statitics.isEmpty()) {
	        	jsonObject.put("message", ExceptionConstants.HR_584);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_297);
	        	jsonObject.put("statitics", statitics);
	        }
	        return new ResponseEntity<>(jsonObject, HttpStatus.OK);
        } catch (HillromException hre){
        	jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<>(jsonObject, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * GET  /users/:userId/clinics/:clinicId/patients -> get the patient list filter by metric type for clinic associated with user.
     */
    @RequestMapping(value = "/users/{userId}/clinics/{clinicId}/patients",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.HCP, AuthoritiesConstants.CLINIC_ADMIN})
    public ResponseEntity<?> getPatientsFilterByMetricTypeForClinicAssociated(@PathVariable Long userId, @PathVariable String clinicId,
    		@RequestParam(value = "filterBy",required = false) String filterBy) {
        log.debug("REST request to get patient list filter by metric type for clinic {} associated with User : {}", clinicId, userId);
        JSONObject jsonObject = new JSONObject();
        try {
        	LocalDate date = LocalDate.now();
        	List<PatientCompliance> patientList = patientHCPService.getPatientListFilterByMetricForClinicAssociated(userId, clinicId, date, filterBy);
	        if (patientList.isEmpty()) {
	        	jsonObject.put("message", ExceptionConstants.HR_585);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_213);
	        	jsonObject.put("patientList", patientList);
	        }
	        return new ResponseEntity<>(jsonObject, HttpStatus.OK);
        } catch (HillromException hre){
        	jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<>(jsonObject, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * GET  /users/:userId/clinics/:clinicId/patients/noevents -> get the patient list with no events for clinic associated with user.
     */
    @RequestMapping(value = "/users/{userId}/clinics/{clinicId}/patients/noevents",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.HCP, AuthoritiesConstants.CLINIC_ADMIN})
    public ResponseEntity<?> getPatientsWithNoEventsForClinicAssociated(@PathVariable Long userId, @PathVariable String clinicId) {
        log.debug("REST request to get patient list with no event for clinic {} associated with User : {}", clinicId, userId);
        JSONObject jsonObject = new JSONObject();
        try {
        	LocalDate date = LocalDate.now();
        	List<PatientUserVO> patientList = patientHCPService.getPatientsWithNoEventsForClinicAssociated(userId, clinicId, date);
	        if (patientList.isEmpty()) {
	        	jsonObject.put("message", ExceptionConstants.HR_585);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_213);
	        	jsonObject.put("patientList", patientList);
	        }
	        return new ResponseEntity<>(jsonObject, HttpStatus.OK);
        } catch (HillromException hre){
        	jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<>(jsonObject, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET  /users/:hcpId/clinics/:clinicId/cumulativeStatistics -> get the patient statistics for clinic associated with hcp user.
     */
    @RequestMapping(value = "/users/{hcpId}/clinics/{clinicId}/cumulativeStatistics",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.HCP})
    public ResponseEntity<?> getPatientsCumulativeStatisticsForClinicAssociatedWithHCP(@PathVariable Long hcpId, @PathVariable String clinicId,
    		@RequestParam(value="from",required=true)@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate from,
    		@RequestParam(value="to",required=true)@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate to,
    		@RequestParam(required=true)String groupBy) {
        log.debug("REST request to get patients cumulative statistics for clinic {} associated with HCP : {}", clinicId, hcpId,from,to,groupBy);
        JSONObject jsonObject = new JSONObject();
        try {
        	Collection<StatisticsVO> statiticsCollection = patientHCPService.getCumulativePatientStatisticsForClinicAssociatedWithHCP(hcpId,clinicId,from,to,groupBy);
	        if (statiticsCollection.isEmpty()) {
	        	jsonObject.put("message", ExceptionConstants.HR_584);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_297);
	        	jsonObject.put("cumulativeStatitics", statiticsCollection);
	        }
	        return new ResponseEntity<>(jsonObject, HttpStatus.OK);
        } catch (HillromException hre){
        	jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<>(jsonObject, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * GET  /users/:hcpId/clinics/:clinicId/cumulativeStatistics -> get the patient statistics for clinic associated with hcp user.
     */
    @RequestMapping(value = "/users/{hcpId}/clinics/{clinicId}/treatmentStatistics",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.HCP})
    public ResponseEntity<?> getPatientsTreatmentStatisticsForClinicAssociatedWithHCP(@PathVariable Long hcpId, @PathVariable String clinicId,
    		@RequestParam(value="from",required=true)@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate from,
    		@RequestParam(value="to",required=true)@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate to,
    		@RequestParam(required=true)String groupBy) {
        log.debug("REST request to get patients treatement statistics for clinic {} associated with HCP : {}", clinicId, hcpId,from,to,groupBy);
        JSONObject jsonObject = new JSONObject();
        try {
        	Collection<TreatmentStatisticsVO> statiticsCollection = patientHCPService.getTreatmentStatisticsForClinicAssociatedWithHCP(hcpId,clinicId,from,to,groupBy);
	        if (statiticsCollection.isEmpty()) {
	        	jsonObject.put("message", ExceptionConstants.HR_584);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_297);
	        	jsonObject.put("treatmentStatitics", statiticsCollection);
	        }
	        return new ResponseEntity<>(jsonObject, HttpStatus.OK);
        } catch (HillromException hre){
        	jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<>(jsonObject, HttpStatus.BAD_REQUEST);
        }
    }

}
