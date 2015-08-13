package com.hillrom.vest.web.rest;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

import net.minidev.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
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

import com.codahale.metrics.annotation.Timed;
import com.hillrom.vest.domain.User;
import com.hillrom.vest.repository.UserRepository;
import com.hillrom.vest.repository.UserSearchRepository;
import com.hillrom.vest.security.AuthoritiesConstants;
import com.hillrom.vest.service.PatientVestDeviceService;
import com.hillrom.vest.service.UserService;
import com.hillrom.vest.web.rest.dto.PatientUserVO;
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

	/**
	 * GET /users -> get all users.
	 */
	@RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public List<User> getAll() {
		log.debug("REST request to get all Users");
		return userRepository.findAll();
	}

	/**
	 * GET /users/:login -> get the "login" user.
	 */
	@RequestMapping(value = "/users/{email}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	ResponseEntity<User> getUser(@PathVariable String email) {
		log.debug("REST request to get User : {}", email);
		return userRepository.findOneByEmail(email)
				.map(user -> new ResponseEntity<>(user, HttpStatus.OK))
				.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	@RequestMapping(value = "/user/patient/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public ResponseEntity<?> searchHcp(
			@RequestParam(required = true, value = "searchString") String searchString,
			@RequestParam(value = "page", required = false) Integer offset,
			@RequestParam(value = "per_page", required = false) Integer limit,
			@RequestParam(value = "sort_by", required = false) String sortBy,
			@RequestParam(value = "asc", required = false) Boolean isAscending)
			throws URISyntaxException {
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
	@Timed
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
    @Timed
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> linkVestDeviceWithPatient(@PathVariable Long id, @RequestBody Map<String, String> deviceData) {
    	log.debug("REST request to link vest device with patient user : {}", id);
        JSONObject jsonObject = patientVestDeviceService.linkVestDeviceWithPatient(id, deviceData);
        if (jsonObject.containsKey("ERROR")) {
        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
        }
    }
    
    /**
     * GET  /patient/:id/vestdevice -> get linked vest device with patient {id}.
     */
    @RequestMapping(value = "/patient/{id}/vestdevice",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> getLinkedVestDeviceWithPatient(@PathVariable Long id) {
    	log.debug("REST request to link vest device with patient user : {}", id);
        JSONObject jsonObject = patientVestDeviceService.getLinkedVestDeviceWithPatient(id);
        if (jsonObject.containsKey("ERROR")) {
        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
        }
    }
}
