package com.hillrom.vest.web.rest;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

import net.minidev.json.JSONObject;

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
import com.hillrom.vest.domain.Clinic;
import com.hillrom.vest.domain.PatientProtocolData;
import com.hillrom.vest.exceptionhandler.HillromException;
import com.hillrom.vest.repository.ClinicRepository;
import com.hillrom.vest.repository.PredicateBuilder;
import com.hillrom.vest.security.AuthoritiesConstants;
import com.hillrom.vest.service.ClinicService;
import com.hillrom.vest.util.ExceptionConstants;
import com.hillrom.vest.util.MessageConstants;
import com.hillrom.vest.web.rest.dto.ClinicDTO;
import com.hillrom.vest.web.rest.util.PaginationUtil;
import com.mysema.query.types.expr.BooleanExpression;

/**
 * REST controller for managing Clinic.
 */
@RestController
@RequestMapping("/api")
public class ClinicResource {

    private final Logger log = LoggerFactory.getLogger(ClinicResource.class);

    @Inject
    private ClinicRepository clinicRepository;
    
    @Inject
    private ClinicService clinicService;

    /**
     * POST  /clinics -> Create a new clinic.
     */
    @RequestMapping(value = "/clinics",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> create(@RequestBody ClinicDTO clinicDTO) {
        log.debug("REST request to save Clinic : {}", clinicDTO);
        JSONObject jsonObject = clinicService.createClinic(clinicDTO);
        if (jsonObject.containsKey("ERROR")) {
        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.CREATED);
        }
    }

    /**
     * PUT  /clinics -> Updates an existing clinic.
     */
    @RequestMapping(value = "/clinics/{id}",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> update(@PathVariable String id, @RequestBody ClinicDTO clinicDTO) {
        log.debug("REST request to update Clinic : {}", clinicDTO);
        JSONObject jsonObject = clinicService.updateClinic(id, clinicDTO);
        if (jsonObject.containsKey("ERROR")) {
        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.CREATED);
        }
    }

    /**
     * GET  /clinics -> get all the clinics.
     */
    @RequestMapping(value = "/clinics",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Clinic>> getAll(@RequestParam(value = "page" , required = false) Integer offset,
                                  @RequestParam(value = "per_page", required = false) Integer limit,
                                  @RequestParam(value = "filter",required = false) String filter)
        throws URISyntaxException {
    	PredicateBuilder<Clinic> clinicPredicatebuilder = new PredicateBuilder<Clinic>(Clinic.class,"clinic");
        if (filter != null) {
            Pattern pattern = Pattern.compile("(\\w+?)(:|<|>)(\\w+?),");
            Matcher matcher = pattern.matcher(filter + ",");
            while (matcher.find()) {
            	clinicPredicatebuilder.with(matcher.group(1), matcher.group(2), matcher.group(3));
            }
        }
        BooleanExpression exp = clinicPredicatebuilder.build();
        Page<Clinic> page = clinicRepository.findAll(exp,PaginationUtil.generatePageRequest(offset, limit));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/clinics", offset, limit);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /clinics/:id -> get the "id" clinic.
     */
    @RequestMapping(value = "/clinics/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<JSONObject> get(@PathVariable String id) {
        log.debug("REST request to get Clinic : {}", id);
        JSONObject jsonObject = new JSONObject();
        try {
    		Clinic clinic = clinicService.getClinicInfo(id);
	    	if (Objects.isNull(clinic)) {
	        	jsonObject.put("ERROR", ExceptionConstants.HR_548);
	        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
	        } else {
	        	jsonObject.put("message", MessageConstants.HR_223);
	        	jsonObject.put("clinic", clinic);
	        	if(clinic.isParent())
	        		jsonObject.put("childClinics", clinicService.getChildClinics(id));
	            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
	        }
    	} catch(HillromException hre){
    		jsonObject.put("ERROR", hre.getMessage());
    		return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
    	}
    }

    /**
     * DELETE  /clinics/:id -> delete the "id" clinic.
     */
    @RequestMapping(value = "/clinics/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.ACCT_SERVICES})
    public ResponseEntity<JSONObject> delete(@PathVariable String id) {
    	log.debug("REST request to delete Clinic : {}", id);
    	JSONObject jsonObject = clinicService.deleteClinic(id);
        if (jsonObject.containsKey("ERROR")) {
        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
        }
    }
    
    /**
     * GET  /clinics -> search clinis.
     * @throws URISyntaxException 
     */
    @RequestMapping(value = "/clinics/search",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Clinic>> search(@RequestParam(value = "searchString")String searchString,
    		@RequestParam(value = "page" , required = false) Integer offset,
            @RequestParam(value = "per_page", required = false) Integer limit,
            @RequestParam(value = "sort_by", required = false) String sortBy,
            @RequestParam(value = "asc",required = false) Boolean isAscending) throws URISyntaxException {
    	 String queryString = new StringBuilder().append("%").append(searchString).append("%").toString();
    	 Map<String,Boolean> sortOrder = new HashMap<>();
    	 if(sortBy != null  && !sortBy.equals("")) {
    		 isAscending =  (isAscending != null)?  isAscending : true;
    		 sortOrder.put(sortBy, isAscending);
    	 }
    	 Page<Clinic> page = clinicRepository.findBy(queryString,PaginationUtil.generatePageRequest(offset, limit, sortOrder));
         HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/clinics/search", offset, limit);
         return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
    
    /**
     * GET  /clinics/:id/hcp -> get the hcp users for the clinic with id :id.
     */
    @RequestMapping(value = "/clinics/hcp",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<JSONObject> getHCPUsers(@RequestParam(value = "filter",required = false) String filter) {
        log.debug("REST request to get Clinic : {}", filter);
        List<String> idList = new ArrayList<>();
        String[] idSet = filter.split(",");
        for(String id : idSet){
        	idList.add(id.split(":")[1]);
        }
        JSONObject jsonObject = clinicService.getHCPUsers(idList);
        if (jsonObject.containsKey("ERROR")) {
        	return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<JSONObject>(jsonObject, HttpStatus.OK);
        }
    }
    
}
