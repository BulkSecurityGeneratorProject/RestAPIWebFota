package com.hillrom.vest.web.rest;


import static com.hillrom.vest.config.Constants.LOG_DIRECTORY;
import static com.hillrom.vest.config.Constants.MATCH_STRING;

import java.awt.print.Pageable;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import net.minidev.json.JSONObject;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillrom.vest.domain.Announcements;
import com.hillrom.vest.exceptionhandler.HillromException;
import com.hillrom.vest.repository.TimsUserRepository;
import com.hillrom.vest.service.TimsInputReaderService;
import com.hillrom.vest.service.TimsService;
import com.hillrom.vest.web.rest.util.PaginationUtil;


@RestController
@RequestMapping("/api")
public class TimsResource {

	@Inject
	private TimsService timsService;
	
	@Inject
	private TimsInputReaderService timsInputReaderService;

	@Inject
	private TimsUserRepository timsUserRepository;
	
	

	/**
     * GET  /listLogDirectory
     */
	@RequestMapping(value="/listLogDirectory", method=RequestMethod.GET,produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> listLogDirectory(
			@RequestParam(value = "page", required = false) Integer offset,
			@RequestParam(value = "per_page", required = false) Integer limit,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "toDate", required = false) String toDate,
			@RequestParam(value = "fromDate", required = false) String fromDate) {

		try{
			System.out.println("Before list log dir");
			List<String> returnVal = timsService.listLogDirectory(LOG_DIRECTORY, MATCH_STRING,toDate,fromDate);
			System.out.println("End list log dir");
			
			DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
			Calendar cal = Calendar.getInstance();
			List<Object> valueObj = new LinkedList<>();
			for (String grepValue : returnVal) {
				HashMap<String, String> hmap = new HashMap<String, String>();
				String[] grepVal = grepValue.split(",");
				System.out.println("Converted String array : grepVal= "+grepVal[2]+"; status ="+status+":"+grepVal.length);
				
				if (grepVal[2].equalsIgnoreCase(status)
						|| grepVal[2].equalsIgnoreCase("Both")) {
					System.out.println("Inside the status If check ");
					String modDate = grepVal[3];
					Date date = new Date(Long.valueOf(modDate));
					//System.out.println("modified date:"+modDate);
					
					//Date date = (Date)formatter.parse(d2);					
					cal.setTime(date);
					System.out.println("Date:"+date);
					String formatedDate = cal.get(Calendar.DATE)+"-"+(cal.get(Calendar.MONTH)+1) +"-"+cal.get(Calendar.YEAR);
					System.out.println("Modfied Date :"+formatedDate);
					System.out.println("Boolean value:"+intervalsCheck(toDate, fromDate, formatedDate ));
					if (!intervalsCheck(toDate, fromDate, formatedDate )) {
						hmap.put("file", grepVal[0]);
						hmap.put("path", grepVal[1]);
						hmap.put("status", grepVal[2]);
						hmap.put("lastMod", grepVal[3]);
						valueObj.add(hmap);
						
						System.out.println("Map object :"+valueObj);
					}
				}
				
				System.out.println("Map list object :"+valueObj);
			}
            int firstResult = PaginationUtil.generatePageRequest(offset, limit).getOffset();
    		int maxResults = firstResult + PaginationUtil.generatePageRequest(offset, limit).getPageSize();
    		List<Object> valueObjSubList = new ArrayList<>();
    		if (firstResult < valueObj.size()) {
    			maxResults = maxResults > valueObj.size() ? valueObj.size() : maxResults;
    			valueObjSubList = valueObj.subList(firstResult, maxResults);
    		}

            Page<Object> page = new PageImpl<Object>(valueObjSubList,
            		PaginationUtil.generatePageRequest(offset, limit), Long.valueOf(valueObj.size()));

			HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/listLogDirectory", offset, limit);
			return new ResponseEntity<>(page, headers, HttpStatus.OK);
          
		}catch(Exception ex){
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}			
	}
	
	private boolean intervalsCheck(String toDate, String fromDate,
			String modifiedDate) {
		return ((fromDate.compareTo(modifiedDate) >= 0 && (toDate
				.compareTo(modifiedDate) >= 0)));

	}

	/**
     * POST  /insertIntoProtocolDataTempTable
     */
	@RequestMapping(value="/insertIntoProtocolDataTempTable", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createpatientprotocolmonarch(){

		JSONObject jsonObject = new JSONObject();
		System.out.println("I am here in call of createpatientprotocolmonarch ");
		try{
				
				
			timsUserRepository.insertIntoProtocolDataTempTable("HR2015000002", "Normal", 2, "3", 5, 20, 10, 14, 1, 10, 1, "214");
			  jsonObject.put("timsMsg", "Record in protocol data temp table created successfully");
			  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
		}catch(Exception ex){
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}		
		
	}

	/**
     * POST  /createpatientprotocol
     */
	@RequestMapping(value="/createpatientprotocol", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createpatientprotocol(){

		JSONObject jsonObject = new JSONObject();
		System.out.println("I am here in call of createpatientprotocol ");
		
		try{
			  //timsService.createPatientProtocol("Normal","Insert","HR2017000606","App");
			  jsonObject.put("timsMsg", "createPatientProtocol stored procedure executed successfully");
			  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
		}catch(Exception ex){
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}		
		
	}
	
 //Start of my code	
	/**
     * POST  /managaPatientDevice
     */
	@RequestMapping(value="/managaPatientDevice", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> managaPatientDevice(){

		JSONObject jsonObject = new JSONObject();
		System.out.println("I am here in call of managaPatientDevice ");
		try{ 
			   //timsService.managePatientDevice("CREATE","HR2015000002","64-00132","64-00132",
				//	   "00:06:66:62:4B:06","QUALC00100017682");
			   
			   /* timsService.managaPatientDevice("INACTIVATE","HR2015000002","64-00132","64-00132",
					         "00:06:66:62:4B:06","QUALC00100017682");
				  timsService.managaPatientDevice("operationType","inPatientId","inPatientoldDeviceSerialNumber","inPatientNewDeviceSerialNumber",
			       "inPatientBluetoothId","inPatientHubId");
						  */
			  jsonObject.put("timsMsg", "managaPatientDevice stored procedure executed successfully");
			  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
		}catch(Exception ex){
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}		
		
	}
	
	/**
     * POST  /managaPatientDevice
     */
	@RequestMapping(value="/managaPatientDeviceAssociation", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> managaPatientDeviceAssociation(){
		
		DateTimeFormatter inpatientTrainingDate = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");
		DateTime inpatientTrainingDate1= inpatientTrainingDate.parseDateTime("05/01/2017 11:22:11");
		
		JSONObject jsonObject = new JSONObject();
		System.out.println("I am here in call of managaPatientDeviceAssociation ");
		try{
			/* timsService.managaPatientDeviceAssociation("operationType","inpatientPatientId","inpatientDeviceType","inpatientDeviceIsActive",
														 "inPatientBluetoothId","inpatientHillromId","inpatientOldId",inpatientTrainingDate1,
														 "inpatientDiagnosisCode1","inpatientDiagnosisCode","inpatientDiagnosisCode3","inpatientDiagnosisCode4",
														 "inpatientGarmentType","inpatientGarmentSize","inpatientGarmentColor");
														 */
			  jsonObject.put("timsMsg", "managaPatientDeviceAssociation stored procedure executed successfully");
			  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
		}catch(Exception ex){
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}		
		
	}
	
	
	/**
     * POST  /managePatientUser
     */
	@RequestMapping(value="/managePatientUser", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> managePatientUser(){

		JSONObject jsonObject = new JSONObject();
		String outPatientId = "";
		String outUserId = "";
		try{	
			

			/*
			JSONObject jsonReturnObject = timsService.managePatientUser("CREATE",
					    					"99188",
					    					null,
					    					null,
					    					"RP991PP892", 
					    					"Jr",
					    					"Steve",
					    					"G",
					    					"Gumerman",
					    					"1943-12-01",
					    					"stevegg@gmail.com",
					    					"60148",
					    					"222-333-4444",
					    					"333-444-5555",
					    					"M",
					    					"En",
					    					"1300 First St",
					    					"Logan",
					    					"MA",
					    					"2015-12-03T23:12:12",
					    					null,
					    					null,
					    					null,
					    					null);*/
			  //jsonObject.put("returnValues",jsonReturnObject);
			  jsonObject.put("timsMsg", "managePatientUser stored procedure executed successfully");
			  
			  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
		}catch(Exception ex){
			ex.printStackTrace();
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}		
		
	}
	
	
		@RequestMapping(value="/managePatientDeviceMonarch", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
		public ResponseEntity<?> managePatientDeviceMonarch(){

			JSONObject jsonObject = new JSONObject();
			System.out.println("I am here in call of managePatientDeviceMonarch ");
			try{		  
				 // timsService.managePatientDeviceMonarch("CREATE",
				//		  									"HR2017000802",
				//		  									"RP003PP333",
				//		  									null);
				  jsonObject.put("timsMsg", "managePatientDeviceMonarch stored procedure executed successfully");
				  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
			}catch(Exception ex){
				jsonObject.put("ERROR", ex.getMessage());
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		
		
		@RequestMapping(value = "/retrieveLogData/logs", method = RequestMethod.POST , produces = MediaType.APPLICATION_JSON_VALUE)
		   public ResponseEntity<?> retrieveLogData( @RequestBody JSONObject logfilePath ,HttpServletRequest request) {
				 
			     	String logFileContent = ""; 
			     	
			     	JSONObject jsonObject = new JSONObject();
			 		
			    try {
			  	 
			  	 try {
			  		 
			  		logFileContent = timsService.retrieveLogData(logfilePath.get("logfilePath").toString());
			  		jsonObject.put("logFileContent", logFileContent);
				  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
					} catch (HillromException e) {
						// TODO Auto-generated catch block
						
						
						jsonObject.put("ERROR", e.getMessage());
						
						return new ResponseEntity<>(jsonObject ,HttpStatus.BAD_REQUEST);
					}
			  	 	    	  
			  	 
			  	 	        
			    } catch (Exception ex) {
			   	 
			   	 jsonObject.put("ERROR", ex.getMessage());
			  			   		
						return new ResponseEntity<>(jsonObject ,HttpStatus.BAD_REQUEST);
			    }

			}

		
	//Use case specific implementation
			@RequestMapping(value="/patientExistsWithNoDevice", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
			public ResponseEntity<?> patientExistsWithNoDevice(){

				JSONObject jsonObject = new JSONObject();
				/*String serialNofromExcel= "abc";
				String hillromId="123";
				String currentHillRomID="XYZ";
				timsService.isSerialNoExistinPatientdeviceAssoc();
				timsService.istHillromIdExistinPatientInfo();
				timsService.isDeviceExistforCurrentHillRominPatDeviceAss();*/
				/*if(timsService.isSerialNoExistinPatientdeviceAssoc()==false && timsService.istHillromIdExistinPatientInfo()==true && timsService.isDeviceExistforCurrentHillRominPatDeviceAss()==false){
					timsService.comonProccallforPatientwithnoDeviceVestSwapandMonarcadd();
				}*/
				System.out.println("I am here in call of patientExistsWithNoDevice ");
				try{		  
					 /* timsService.managePatientDeviceMonarch("operationTypeIndicator","inPatientId","inPatientoldDeviceSerialNumber","inPatientNewDeviceSerialNumber");
					  jsonObject.put("timsMsg", "managaPatientUser stored procedure executed successfully");*/
					  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
				}catch(Exception ex){
					jsonObject.put("ERROR", ex.getMessage());
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}	
			}
				@RequestMapping(value="/neitherPatientnorDeviceExist", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
				public ResponseEntity<?> neitherPatientnorDeviceExist(){

					JSONObject jsonObject = new JSONObject();
					/*String serialNofromExcel= "abc";
					String hillromId="123";
					String currentHillRomID="XYZ";
					timsService.isSerialNoExistinPatientdeviceAssoc();
					timsService.istHillromIdExistinPatientInfo();
					timsService.isDeviceExistforCurrentHillRominPatDeviceAss();*/
					/*if(timsService.isSerialNoExistinPatientdeviceAssoc()==false && timsService.isDeviceExistforCurrentHillRominPatDeviceAss()==false){
						timsService.comonProccallforPatientwithnoDeviceVestSwapandMonarcadd();
					}*/
					System.out.println("I am here in call of neitherPatientnorDeviceExist ");
					try{		  
						  
						/*timsService.managePatientDeviceMonarch("operationTypeIndicator","inPatientId","inPatientoldDeviceSerialNumber","inPatientNewDeviceSerialNumber");
						jsonObject.put("timsMsg", "managaPatientUser stored procedure executed successfully");  */
						return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
					}catch(Exception ex){
						jsonObject.put("ERROR", ex.getMessage());
						return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
					}
	}







	@RequestMapping(value="/executeTIMSJob", method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> executeTIMSJob(){

		JSONObject jsonObject = new JSONObject();

		try{		  
			  timsInputReaderService.ExecuteTIMSJob();
			  jsonObject.put("timsMsg", "managaPatientUser stored procedure executed successfully");
			  return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);			
		}catch(Exception ex){
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}		
	}

}


