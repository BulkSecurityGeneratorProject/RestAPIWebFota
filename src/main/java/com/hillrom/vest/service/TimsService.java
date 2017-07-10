package com.hillrom.vest.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.StoredProcedureQuery;

import net.minidev.json.JSONObject;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hillrom.vest.domain.Announcements;
import com.hillrom.vest.domain.PatientDevicesAssoc;
import com.hillrom.vest.exceptionhandler.HillromException;
import com.hillrom.vest.repository.AnnouncementsPermissionRepository;
import com.hillrom.vest.repository.AnnouncementsRepository;
import com.hillrom.vest.repository.PatientDevicesAssocRepository;
import com.hillrom.vest.repository.PatientInfoRepository;
import com.hillrom.vest.repository.PatientVestDeviceRepository;
import com.hillrom.vest.repository.TimsUserRepository;
import com.hillrom.vest.repository.monarch.PatientMonarchDeviceRepository;
import com.hillrom.vest.security.AuthoritiesConstants;
import com.hillrom.vest.service.util.DateUtil;
import com.hillrom.vest.util.RelationshipLabelConstants;
import com.hillrom.vest.web.rest.dto.AnnouncementsDTO;
import com.hillrom.vest.web.rest.dto.ClinicVO;
import com.hillrom.vest.web.rest.dto.PatientInfoDTO;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Service
@Transactional
public class TimsService {

	private final Logger log = LoggerFactory.getLogger("com.hillrom.vest.tims");
	
	
	@Inject
	private TimsUserRepository timsUserRepository;
	
	@Inject
	private PatientDevicesAssocRepository patientDevicesAssocRepository;

	@Inject
	private PatientInfoService patientInfoService;

	@Inject
	private PatientInfoRepository patientInfoRepository;

	@Inject
	private PatientVestDeviceRepository patientVestDeviceRepository;
	
	@Inject
	private PatientMonarchDeviceRepository patientMonarchDeviceRepository;
	

	 DateTimeFormatter dobFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
    /* DateTimeFormatter deviceAssocdateFormat = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");*/
 
	
		/**
		 * 
		 * @param
		 * @return
		 * @throws HillromException
		 */
		public void insertIntoProtocolDataTempTable(String patient_id,
                									String type,
									                int treatments_per_day,
									                String treatment_label,
									                int min_minutes_per_treatment,
									                int max_minutes_per_treatment,
									                int min_frequency,
									                int max_frequency,
									                int min_pressure,
									                int max_pressure,
									                int to_be_inserted,
									                String user_id) throws HillromException{	
			try{
					timsUserRepository.insertIntoProtocolDataTempTable(patient_id,
													type,
									                treatments_per_day,
									                treatment_label,
									                min_minutes_per_treatment,
									                max_minutes_per_treatment,
									                min_frequency,
									                max_frequency,
									                min_pressure,
									                max_pressure,
									                to_be_inserted,
									                user_id);
			}catch(Exception ex){
				throw new HillromException("Error While invoking Stored Procedure " , ex);
			}
		}
	 
	 /**
	 * 
	 * @param
	 * @return
	 * @throws HillromException
	 */
	public void createPatientProtocolMonarch(PatientInfoDTO patientInfoDTO) throws HillromException{	
		try{
				timsUserRepository.createPatientProtocolMonarch(patientInfoDTO.getProtocol_type_key(),
						 patientInfoDTO.getOperation_type(),
						 patientInfoDTO.getPatient_id(),
						 patientInfoDTO.getCreated_by());
		}catch(Exception ex){
			throw new HillromException("Error While invoking Stored Procedure " , ex);
		}
	}
	
	
	
	/**
	 * 
	 * @param
	 * @return
	 * @throws HillromException
	 */
	public void createPatientProtocol(PatientInfoDTO patientInfoDTO) throws HillromException{	
		try{
			timsUserRepository.createPatientProtocol(patientInfoDTO.getProtocol_type_key(),
												 patientInfoDTO.getOperation_type(),
												 patientInfoDTO.getPatient_id(),
												 patientInfoDTO.getCreated_by());
		}catch(Exception ex){
			throw new HillromException("Error While invoking Stored Procedure " , ex);
		}
	}

  //Start of my code
	/**
	 * 
	 * @param
	 * @return
	 * @throws HillromException
	 */
	public void managePatientDevice(PatientInfoDTO patientInfoDTO) throws HillromException{		
		
		try{
			timsUserRepository.managePatientDevice(patientInfoDTO.getOperation_type(), 
												patientInfoDTO.getPatient_id(), 
												patientInfoDTO.getOld_serial_number(), 
												patientInfoDTO.getNew_serial_number(),
												patientInfoDTO.getBluetooth_id(), 
												patientInfoDTO.getHub_id());
		}catch(Exception ex){
			throw new HillromException("Error While invoking Stored Procedure " , ex);
		}
	}


	/**
	 * 
	 * @param
	 * @return
	 * @throws HillromException
	 */
	
	
	public void managePatientDeviceAssociation(PatientInfoDTO patientInfoDTO) throws HillromException{	
		
		try{
			timsUserRepository.managePatientDeviceAssociation(patientInfoDTO.getOperation_type(),
														  patientInfoDTO.getPatient_id(),
														  patientInfoDTO.getDevice_type(),
														  patientInfoDTO.getIs_active(),
														  patientInfoDTO.getSerial_num(),
														  patientInfoDTO.getTims_cust(),
														  patientInfoDTO.getOld_patient_id(),
														  patientInfoDTO.getTrain_dt(),
														  patientInfoDTO.getDx1(),
														  patientInfoDTO.getDx2(),
														  patientInfoDTO.getDx3(),
														  patientInfoDTO.getDx4(),
														  patientInfoDTO.getGarment_type(),
														  patientInfoDTO.getGarment_size(),
														  patientInfoDTO.getGarment_color());
		}catch(Exception ex){
			throw new HillromException("Error While invoking Stored Procedure " , ex);
		}
	}

	
	/**This need to remove not required 
	 * 
	 * @param
	 * @return
	 * @throws HillromException
	 */
	
	public JSONObject managePatientUser(PatientInfoDTO patientInfoDTO) throws HillromException {	

		try{
		
		return timsUserRepository.managePatientUser(patientInfoDTO.getOperation_type(), 
													patientInfoDTO.getTims_cust(), 
													patientInfoDTO.getHub_id(), 
													patientInfoDTO.getBluetooth_id(), 
													patientInfoDTO.getSerial_num(), 
													patientInfoDTO.getTitle(),
													patientInfoDTO.getFirst_nm(), 
													patientInfoDTO.getMiddle_nm(), 
													patientInfoDTO.getLast_nm(), 
													patientInfoDTO.getDob().toString(dobFormat), 
													patientInfoDTO.getEmail(), 
													patientInfoDTO.getZip_cd(), 
													patientInfoDTO.getPrimary_phone(), 
													patientInfoDTO.getMobile_phone(), 
													patientInfoDTO.getGender(), 
													patientInfoDTO.getLang_key(), 
													patientInfoDTO.getAddress(), 
													patientInfoDTO.getCity(), 
													patientInfoDTO.getState(), 
													null, // Following fields no longer being used from this table 
													null, 
													null, 
													null, 
													null);
		}catch(Exception ex){
			throw new HillromException("Error While invoking Stored Procedure " , ex);
		}
		
		
	}
	
	
	
	public void managePatientDeviceMonarch(PatientInfoDTO patientInfoDTO) throws HillromException{		
		
		try{
			timsUserRepository.managePatientDeviceMonarch(patientInfoDTO.getOperation_type(), 
				patientInfoDTO.getPatient_id(), 
				patientInfoDTO.getOld_serial_number(), 
				patientInfoDTO.getNew_serial_number());
		}catch(Exception ex){
			throw new HillromException("Error While invoking Stored Procedure " , ex);
		}
	}
	
	
	
	public String retrieveLogData(String logfilePath)throws HillromException
    {
	   
	   String content = "";
	   String line = "";
	   BufferedReader reader = null;
	   
	  try{
		reader = new BufferedReader(new FileReader(logfilePath));
		line = reader.readLine();
	   	  content +=  line;
	   	  while ((line = reader.readLine()) != null)
	   	  {
	   	     content += "\n" + line;
	   	    	    	     
	   	  }
	   	
	  }
	  catch(IOException e)
	  {
		log.error("Exception Occured"+e.getMessage());
	  }
	  finally
	  {
		  if(reader!=null)
		  {
	    try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		  }
	  return content;
		
    }
	
	public List<String> listLogDirectory(String logfilePath, String matchStr) throws HillromException {
		
		File folder = new File(logfilePath);
		File[] listOfFiles = folder.listFiles();
		
		List<String> returnLogFiles = new LinkedList<>();
		
		for (File file : listOfFiles) {
		    if (file.isFile()) {
		    	try {
			    	log.debug(file.getName());

	                Runtime rt = Runtime.getRuntime();
	                String[] cmd = { "/bin/sh", "-c", "grep -c '"+matchStr+"' '"+logfilePath+file.getName()+"' " };
	                Process proc = rt.exec(cmd);
	                BufferedReader is = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	                String line;
	                Date lastModifiedDate = new Date(file.lastModified());
	                while ((line = is.readLine()) != null) {
	                    returnLogFiles.add(file.getName()+","+(Integer.parseInt(line)>0?"Success":"Failure")+","+lastModifiedDate);
	                }			    	
			    	
		    	}catch(Exception ex){
		    		ex.printStackTrace();
		    	}
		    }
		}
		
		return returnLogFiles;

    }
	

	
	
	public String getDeviceTypeFromRecord(Map fileRecords, int position){
		PatientInfoDTO patientInfoDTO = (PatientInfoDTO) fileRecords.get(position);
		return patientInfoDTO.getDevice_type();
	}
	
	public boolean isSerialNoExistInPatientdeviceAssocVest(String serialNumber){
		
		
		if(patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"VEST").isPresent()){
			log.debug("Checking isSerialNoExistInPatientdeviceAssocVest ");
			return true;
		}
			
		
		return false;
	}
	
	public boolean isSerialNoExistInPatientdeviceAssocMonarch(String serialNumber){
		
		if(patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"MONARCH").isPresent()){
			log.debug("Checking isSerialNoExistInPatientdeviceAssocMonarch ");
			return true;
		}
		
		return false;
	}
	
	public boolean isHillromIdExistInPatientInfo(String hillromId){
		
		if(patientInfoService.findOneByHillromId(hillromId).isPresent()){
			log.debug("Checking isHillromIdExistInPatientInfo ");
				return true;
		}
		
		return false;
	}
	
	public boolean isHillromIdExistInPatientDeviceAssocVest(String hillromId){
		
		if(patientDevicesAssocRepository.findByHillromIdAndDeviceType(hillromId,"VEST").isPresent()){
				log.debug("Checking isHillromIdExistInPatientDeviceAssocVest ");
				return true;
		}
		
		return false;
	}
	
	public boolean isHillromIdExistInPatientDeviceAssocMonarch(String hillromId){
		
		if(patientDevicesAssocRepository.findByHillromIdAndDeviceType(hillromId,"MONARCH").isPresent()){
				log.debug("Checking isHillromIdExistInPatientDeviceAssocMonarch ");
				return true;
		}
		
		return false;
	}
	
	public boolean isHillromIdHasVestDeviceInPatientDeviceAssoc(String hillromId){
		
		if((patientDevicesAssocRepository.findByHillromId(hillromId).isPresent()) 
			&& (patientDevicesAssocRepository.findByHillromId(hillromId).get().getDeviceType().equalsIgnoreCase("VEST"))){
				log.debug("Checking isHillromIdHasVestDeviceInPatientDeviceAssoc ");
				return true;
		}
		
		return false;
	}
	
	public boolean isHillromIdHasMonarchDeviceInPatientDeviceAssoc(String hillromId){
		
		if((patientDevicesAssocRepository.findByHillromId(hillromId).isPresent()) 
			&& (patientDevicesAssocRepository.findByHillromId(hillromId).get().getDeviceType().equalsIgnoreCase("MONARCH"))){
				log.debug("Checking isHillromIdHasMonarchDeviceInPatientDeviceAssoc ");
				return true;
		}
		
		return false;
	}
	

	public boolean isCurrentSerialNumberOwnedByShellVest(String serialNumber){
		
		if((patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"VEST").isPresent()) ) {
			if((patientInfoRepository.findOneById(patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"VEST").get().getPatientId()).getFirstName().equalsIgnoreCase("Hill-Rom")) ){
				log.debug("Checking isCurrentSerialNumberOwnedByShellVest ");	
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isCurrentSerialNumberOwnedByShellMonarch(String serialNumber){
		
		if((patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"MONARCH").isPresent()) ) {
				String patientId = patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"MONARCH").get().getPatientId();	
				if((patientInfoRepository.findOneById(patientId).getFirstName().equalsIgnoreCase("Monarch")) && (patientInfoRepository.findOneById(patientId).getLastName().equalsIgnoreCase("Hill-Rom")) ){
					log.debug("Checking isCurrentSerialNumberOwnedByShellMonarch ");	
					return true;
				}
		}
		
		return false;
	}
	
	public boolean isCurrentSerialNumberOwnedByDifferentPatientVest(String serialNumber,String hillromId){
		
		if(!patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"VEST").get().getHillromId().equalsIgnoreCase(hillromId)) {
				log.debug("Checking isCurrentSerialNumberOwnedByDifferentPatientVest ");
				return true;
		}
		
		return false;
	}
	
	public boolean isCurrentSerialNumberOwnedByDifferentPatientMonarch(String serialNumber,String hillromId){
		
		if(!patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"MONARCH").get().getHillromId().equalsIgnoreCase(hillromId)){ 
			log.debug("Checking isCurrentSerialNumberOwnedByDifferentPatientMonarch ");	
			return true;
		}
		
		return false;
	}

	public boolean isCurrentSerialNumberOwnedByCurrentHillromIdVest(String serialNumber,String hillromId){
		
		if(patientDevicesAssocRepository.findOneBySerialNumberAndHillromIdAndDeviceType(serialNumber,hillromId,"VEST").isPresent()){
				log.debug("Checking isCurrentSerialNumberOwnedByCurrentHillromIdVest ");
				return true;		
		}
		return false;
	}
	
	public boolean isCurrentSerialNumberOwnedByCurrentHillromIdMonarch(String serialNumber,String hillromId){
		
		if(patientDevicesAssocRepository.findOneBySerialNumberAndHillromIdAndDeviceType(serialNumber,hillromId,"MONARCH").isPresent()) {
			log.debug("Checking isCurrentSerialNumberOwnedByCurrentHillromIdMonarch ");
			return true;	
		}

		return false;
	}

	public boolean isOwnerExistsForCurrentSerialNumberVest(String serialNumber){
		
		if(patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"VEST").isPresent()) {
				log.debug("Checking isOwnerExistsForCurrentSerialNumberVest ");
				return true;
		}
		
		return false;
	}
	
	public boolean isOwnerExistsForCurrentSerialNumberMonarch(String serialNumber){
		
		if(patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(serialNumber,"MONARCH").isPresent()){ 
				log.debug("Checking isOwnerExistsForCurrentSerialNumberMonarch ");
				return true;
		}
		
		return false;
	}
	
	
	// All Cases start below <ScenarioName>Vest
	
	public boolean CASE1_NeitherPatientNorDeviceExist_VEST(PatientInfoDTO patientInfoDTO){
		
		
		if((!isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (!isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))){
			log.debug("Inside CASE1_NeitherPatientNorDeviceExist_VEST ");
			//managePatientUser(CREATE)
			//managaPatientDevice(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocol()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				JSONObject returnValues =  managePatientUser(patientInfoDTO);
				patientInfoDTO.setPatient_id(returnValues.get("return_patient_id").toString());
				patientInfoDTO.setPatient_user_id(returnValues.get("return_user_id").toString());
				
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDevice(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocol(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}				
			
			return true;
		}
		
		return false;
		
	}
	
	public boolean CASE2_PatientExistsWithNODevice_VEST(PatientInfoDTO patientInfoDTO){
		
		
		
		if((!isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isHillromIdExistInPatientDeviceAssocVest(patientInfoDTO.getTims_cust())) ){
			
			log.debug("Inside CASE2_PatientExistsWithNODevice_VEST ");
			//managaPatientDevice(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocol()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDevice(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				

				patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
				patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
						filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocol(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}	
			
			return true;
		}
		
		return false;
		
		
	}
	
	public boolean CASE3_PatientHasMonarchAddVisivest_VEST(PatientInfoDTO patientInfoDTO){
		
		
		if((!isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (isHillromIdExistInPatientDeviceAssocVest(patientInfoDTO.getTims_cust())) && (!isHillromIdHasVestDeviceInPatientDeviceAssoc(patientInfoDTO.getTims_cust())) ){
			
			log.debug("Inside CASE3_PatientHasMonarchAddVisivest_VEST ");
			//managaPatientDevice(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocol()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDevice(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
				patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
						filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocol(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}	
			
			return true;
			
		}
		
		return false;
		
	}
	
	public boolean CASE4_PatientHasDifferentVisivestSwap_VEST(PatientInfoDTO patientInfoDTO){
		
		if((!isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (isHillromIdExistInPatientDeviceAssocVest(patientInfoDTO.getTims_cust())) && (isHillromIdHasVestDeviceInPatientDeviceAssoc(patientInfoDTO.getTims_cust())) ){
			
			log.debug("Inside CASE4_PatientHasDifferentVisivestSwap_VEST ");
			//managaPatientDevice(UPDATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocol()
			try{
				patientInfoDTO.setOperation_type("UPDATE");
				patientInfoDTO.setOld_serial_number(patientDevicesAssocRepository.findByHillromIdAndDeviceType(patientInfoDTO.getTims_cust(), "VEST").get().getSerialNumber());
				patientInfoDTO.setNew_serial_number(patientInfoDTO.getSerial_num());
				managePatientDevice(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
				patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
						filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocol(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}		
			
			return true;
			
			
		}
		
		return false;		
	}
	

	public boolean CASE5_DeviceOwnedByShell_VEST(PatientInfoDTO patientInfoDTO){
		
		if((isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (!isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& isCurrentSerialNumberOwnedByShellVest(patientInfoDTO.getSerial_num()) ){
			
			log.debug("Inside CASE5_DeviceOwnedByShell_VEST ");
			//managePatientUser(UPDATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocol()
			

			try{
				patientInfoDTO.setOperation_type("UPDATE");
				JSONObject returnValues = managePatientUser(patientInfoDTO);
				patientInfoDTO.setPatient_id(returnValues.get("return_patient_id").toString());
				patientInfoDTO.setPatient_user_id(returnValues.get("return_user_id").toString());
				
				patientInfoDTO.setOperation_type("UPDATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocol(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}
			
			return true;
		}
		
		return false;		
	}
	
	public boolean CASE6_DeviceOwnedByDifferentPatient_VEST(PatientInfoDTO patientInfoDTO){
		
		if((isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (!isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isCurrentSerialNumberOwnedByShellVest(patientInfoDTO.getSerial_num())) 
				&& (isCurrentSerialNumberOwnedByDifferentPatientVest(patientInfoDTO.getSerial_num(), patientInfoDTO.getTims_cust())) ){
		
			log.debug("Inside CASE6_DeviceOwnedByDifferentPatient_VEST ");
			//managaPatientDevice(INACTIVATE)
			//managePatientUser(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocol()
			try{
				String patient_id_of_serial_number_to_inactivate = patientDevicesAssocRepository.findOneBySerialNumberAndDeviceType(patientInfoDTO.getSerial_num(),"VEST").get().getPatientId();
				patientInfoDTO.setPatient_id(patient_id_of_serial_number_to_inactivate);
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				patientInfoDTO.setNew_serial_number(null);
				patientInfoDTO.setOperation_type("INACTIVATE");
				managePatientDevice(patientInfoDTO);

				patientInfoDTO.setOperation_type("CREATE");
				JSONObject returnValues =   managePatientUser(patientInfoDTO);
				patientInfoDTO.setPatient_id(returnValues.get("return_patient_id").toString());
				patientInfoDTO.setPatient_user_id(returnValues.get("return_user_id").toString());
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocol(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}			
			
			return true;
		}
		
		return false;		
	}
	
	public boolean CASE7_DeviceIsOrphanPatientDoesNotExist_VEST(PatientInfoDTO patientInfoDTO){
		
		if((isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (!isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isCurrentSerialNumberOwnedByShellVest(patientInfoDTO.getSerial_num())) 
				&& (!isCurrentSerialNumberOwnedByDifferentPatientVest(patientInfoDTO.getSerial_num(),patientInfoDTO.getTims_cust() )) ){

			log.debug("Inside CASE7_DeviceIsOrphanPatientDoesNotExist_VEST ");	
			//managePatientUser(CREATE)
			//managaPatientDevice(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocol()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				JSONObject returnValues = managePatientUser(patientInfoDTO);
				patientInfoDTO.setPatient_id(returnValues.get("return_patient_id").toString());
				patientInfoDTO.setPatient_user_id(returnValues.get("return_user_id").toString());
				
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDevice(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocol(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}	
			
			return true;
		}
		
		return false;		
	}
	
	

	public boolean CASE8_DeviceIsOrphanButPatientExist_VEST(PatientInfoDTO patientInfoDTO){
		
		if((isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isCurrentSerialNumberOwnedByCurrentHillromIdVest(patientInfoDTO.getSerial_num(),patientInfoDTO.getTims_cust())) 
				&& (isOwnerExistsForCurrentSerialNumberVest(patientInfoDTO.getSerial_num() )) ){

			log.debug("Inside CASE8_DeviceIsOrphanButPatientExist_VEST ");	
			//managaPatientDevice(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocol()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDevice(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
				patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
						filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocol(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}
			
			return true;
		}
		
		return false;		
	}
	
	

	
	public boolean DeviceOwnedByDifferentPatient_VEST(PatientInfoDTO patientInfoDTO){
		
		if((isSerialNoExistInPatientdeviceAssocVest(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isCurrentSerialNumberOwnedByCurrentHillromIdVest(patientInfoDTO.getSerial_num(),patientInfoDTO.getTims_cust())) 
				&& (!isOwnerExistsForCurrentSerialNumberVest(patientInfoDTO.getSerial_num() )) ){
			log.debug("Inside DeviceOwnedByDifferentPatient_VEST ");
			//managePatientDevice(INACTIVATE)
			
			try{
				patientInfoDTO.setOperation_type("INACTIVATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				patientInfoDTO.setNew_serial_number(null);
				managePatientDevice(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}
			

			
			return true;
		}
		
		return false;		
	}


	
	public boolean CASE9_PatientHasDifferentVisivestSwap_VEST(PatientInfoDTO patientInfoDTO){
		
		
		if(DeviceOwnedByDifferentPatient_VEST(patientInfoDTO)){
			
			if( (isHillromIdExistInPatientDeviceAssocVest(patientInfoDTO.getTims_cust())) && (isHillromIdHasVestDeviceInPatientDeviceAssoc(patientInfoDTO.getTims_cust())) ){
				log.debug("Inside CASE9_PatientHasDifferentVisivestSwap_VEST ");
	
				//managePatientDevice(UPDATE)
				//managePatientDeviceAssociation(CREATE)
				//createPatientProtocol()
				
				//Question : How do you know whether to pass a Normal or Custom protocol key ?
				try{
					patientInfoDTO.setOperation_type("UPDATE");
					patientInfoDTO.setOld_serial_number(patientDevicesAssocRepository.findByHillromIdAndDeviceType(patientInfoDTO.getTims_cust(), "VEST").get().getSerialNumber());
					patientInfoDTO.setNew_serial_number(patientInfoDTO.getSerial_num());
					managePatientDevice(patientInfoDTO);
					
					patientInfoDTO.setOperation_type("CREATE");
					managePatientDeviceAssociation(patientInfoDTO);
					
					patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
					patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
							filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
					insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
					patientInfoDTO.setOperation_type("Insert");
					createPatientProtocol(patientInfoDTO);
				}catch(Exception ex){
					ex.printStackTrace();
					return false;
				}
				
				return true;
			}
			
			return false;
		}
		
		return false;
		
		
	}
	
	public boolean CASE10_PatientHasMonarchAddVisivest_VEST(PatientInfoDTO patientInfoDTO){
		
		
		
		if(DeviceOwnedByDifferentPatient_VEST(patientInfoDTO)){
			
			if( (isHillromIdExistInPatientDeviceAssocVest(patientInfoDTO.getTims_cust())) && (!isHillromIdHasVestDeviceInPatientDeviceAssoc(patientInfoDTO.getTims_cust())) ){
				
				log.debug("Inside CASE10_PatientHasMonarchAddVisivest_VEST ");
				//managePatientDevice(CREATE)
				//managePatientDeviceAssociation(CREATE)
				//createPatientProtocol()
				
				try{
					patientInfoDTO.setOperation_type("CREATE");
					patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
					managePatientDevice(patientInfoDTO);
					
					patientInfoDTO.setOperation_type("CREATE");
					managePatientDeviceAssociation(patientInfoDTO);
					
					patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
					patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
							filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
					insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
					patientInfoDTO.setOperation_type("Insert");
					createPatientProtocol(patientInfoDTO);
				}catch(Exception ex){
					ex.printStackTrace();
					return false;
				}
				
				return true;
			}
			
			return false;
		
		}
		
		return false;
		
	}
	
	public boolean CASE11_PatientExistsWithNODevice_VEST(PatientInfoDTO patientInfoDTO){
		
		
		
		if(DeviceOwnedByDifferentPatient_VEST(patientInfoDTO)){
			
			if (!isHillromIdExistInPatientDeviceAssocVest(patientInfoDTO.getTims_cust())) {
				
				log.debug("Inside CASE11_PatientExistsWithNODevice_VEST ");
				//managePatientDevice(CREATE)
				//managePatientDeviceAssociation(CREATE)
				//createPatientProtocol()
				try{
					patientInfoDTO.setOperation_type("CREATE");
					patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
					managePatientDevice(patientInfoDTO);
					
					patientInfoDTO.setOperation_type("CREATE");
					managePatientDeviceAssociation(patientInfoDTO);
					
					patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
					patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
							filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
					insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
					patientInfoDTO.setOperation_type("Insert");
					createPatientProtocol(patientInfoDTO);
				}catch(Exception ex){
					ex.printStackTrace();
					return false;
				}
				
				return true;
			}
			
			return false;
		
		}
		return false;	
	}
	
	// All Cases start below  <ScenarioName>Monarch
	
	public boolean CASE1_NeitherPatientNorDeviceExist_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((!isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (!isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))){
			log.debug("Inside CASE1_NeitherPatientNorDeviceExist_MONARCH ");
			//managePatientUser(CREATE)
			//managaPatientDeviceMonarch(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocolMonarch()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				JSONObject returnValues = managePatientUser(patientInfoDTO);
				patientInfoDTO.setPatient_id(returnValues.get("return_patient_id").toString());
				patientInfoDTO.setPatient_user_id(returnValues.get("return_user_id").toString());
				
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDeviceMonarch(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocolMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}				
			
			return true;
		}
		
		return false;
		
	}
	
	public boolean CASE2_PatientExistsWithNODevice_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((!isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isHillromIdExistInPatientDeviceAssocMonarch(patientInfoDTO.getTims_cust())) ){
			
			log.debug("Inside CASE2_PatientExistsWithNODevice_MONARCH ");
			//managaPatientDeviceMonarch(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocolMonarch()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDeviceMonarch(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
				patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
						filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocolMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}	
			
			return true;
		}
		
		return false;
		
		
	}
	
	public boolean CASE3_PatientHasVisivestAddMonarch_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((!isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (isHillromIdExistInPatientDeviceAssocMonarch(patientInfoDTO.getTims_cust())) && (!isHillromIdHasMonarchDeviceInPatientDeviceAssoc(patientInfoDTO.getTims_cust())) ){
			
			log.debug("Inside CASE3_PatientHasVisivestAddMonarch_MONARCH ");
			//managaPatientDeviceMonarch(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocolMonarch()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDeviceMonarch(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
				patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
						filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocolMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}	
			
			return true;
			
		}
		
		return false;
		
	}
	
	public boolean CASE4_PatientHasDifferentMonarchSwap_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((!isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (isHillromIdExistInPatientDeviceAssocMonarch(patientInfoDTO.getTims_cust())) && (isHillromIdHasMonarchDeviceInPatientDeviceAssoc(patientInfoDTO.getTims_cust())) ){
			
			log.debug("Inside CASE4_PatientHasDifferentMonarchSwap_MONARCH ");
			//managaPatientDeviceMonarch(UPDATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocolMonarch()
			try{
				patientInfoDTO.setOperation_type("UPDATE");
				patientInfoDTO.setOld_serial_number(patientDevicesAssocRepository.findByHillromIdAndDeviceType(patientInfoDTO.getTims_cust(), "MONARCH").get().getSerialNumber());
				patientInfoDTO.setNew_serial_number(patientInfoDTO.getSerial_num());
				managePatientDeviceMonarch(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
				patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
						filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocolMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}		
			
			return true;
			
			
		}
		
		return false;		
	}
	

	public boolean CASE5_DeviceOwnedByShell_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (!isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& isCurrentSerialNumberOwnedByShellMonarch(patientInfoDTO.getSerial_num()) ){
			
			log.debug("Inside CASE5_DeviceOwnedByShell_MONARCH ");
			//managePatientUser(UPDATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocolMonarch()
			

			try{
				patientInfoDTO.setOperation_type("UPDATE");
				JSONObject returnValues = managePatientUser(patientInfoDTO);
				patientInfoDTO.setPatient_id(returnValues.get("return_patient_id").toString());
				patientInfoDTO.setPatient_user_id(returnValues.get("return_user_id").toString());
				
				patientInfoDTO.setOperation_type("UPDATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocolMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}
			
			return true;
		}
		
		return false;		
	}
	
	public boolean CASE6_DeviceOwnedByDifferentPatient_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (!isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isCurrentSerialNumberOwnedByShellMonarch(patientInfoDTO.getSerial_num())) 
				&& (isCurrentSerialNumberOwnedByDifferentPatientMonarch(patientInfoDTO.getSerial_num(),patientInfoDTO.getTims_cust() )) ){
			log.debug("Inside CASE6_DeviceOwnedByDifferentPatient_MONARCH ");	
			//managaPatientDeviceMonarch(INACTIVATE)
			//managePatientUser(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocolMonarch()
			try{
				patientInfoDTO.setOperation_type("INACTIVATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				patientInfoDTO.setNew_serial_number(null);
				managePatientDeviceMonarch(patientInfoDTO);

				patientInfoDTO.setOperation_type("CREATE");
				JSONObject returnValues = managePatientUser(patientInfoDTO);
				patientInfoDTO.setPatient_id(returnValues.get("return_patient_id").toString());
				patientInfoDTO.setPatient_user_id(returnValues.get("return_user_id").toString());
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocolMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}			
			
			return true;
		}
		
		return false;		
	}
	
	public boolean CASE7_DeviceIsOrphanPatientDoesNotExist_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (!isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isCurrentSerialNumberOwnedByShellMonarch(patientInfoDTO.getSerial_num())) 
				&& (!isCurrentSerialNumberOwnedByDifferentPatientMonarch(patientInfoDTO.getSerial_num(),patientInfoDTO.getTims_cust() )) ){

			log.debug("Inside CASE7_DeviceIsOrphanPatientDoesNotExist_MONARCH ");	
			//managePatientUser(CREATE)
			//managaPatientDeviceMonarch(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocolMonarch()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				JSONObject returnValues = managePatientUser(patientInfoDTO);
				patientInfoDTO.setPatient_id(returnValues.get("return_patient_id").toString());
				patientInfoDTO.setPatient_user_id(returnValues.get("return_user_id").toString());
				
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDeviceMonarch(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocolMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}	
			
			return true;
		}
		
		return false;		
	}
	
	

	public boolean CASE8_DeviceIsOrphanButPatientExist_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isCurrentSerialNumberOwnedByCurrentHillromIdMonarch(patientInfoDTO.getSerial_num(),patientInfoDTO.getTims_cust())) 
				&& (isOwnerExistsForCurrentSerialNumberMonarch(patientInfoDTO.getSerial_num() )) ){

			log.debug("Inside CASE8_DeviceIsOrphanButPatientExist_MONARCH ");	
			//managaPatientDeviceMonarch(CREATE)
			//managaPatientDeviceAssociation(CREATE)
			//createPatientProtocolMonarch()
			try{
				patientInfoDTO.setOperation_type("CREATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				managePatientDeviceMonarch(patientInfoDTO);
				
				patientInfoDTO.setOperation_type("CREATE");
				managePatientDeviceAssociation(patientInfoDTO);
				
				patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
				patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
						filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
				insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
				patientInfoDTO.setOperation_type("Insert");
				createPatientProtocolMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}
			
			return true;
		}
		
		return false;		
	}
	
	

	
	public boolean DeviceOwnedByDifferentPatient_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if((isSerialNoExistInPatientdeviceAssocMonarch(patientInfoDTO.getSerial_num())) && (isHillromIdExistInPatientInfo(patientInfoDTO.getTims_cust()))
				&& (!isCurrentSerialNumberOwnedByCurrentHillromIdMonarch(patientInfoDTO.getSerial_num(),patientInfoDTO.getTims_cust())) 
				&& (!isOwnerExistsForCurrentSerialNumberMonarch(patientInfoDTO.getSerial_num() )) ){
			log.debug("Inside DeviceOwnedByDifferentPatient_MONARCH ");
			//managePatientDeviceMonarch(INACTIVATE)
			
			try{
				patientInfoDTO.setOperation_type("INACTIVATE");
				patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
				patientInfoDTO.setNew_serial_number(null);
				managePatientDeviceMonarch(patientInfoDTO);
			}catch(Exception ex){
				ex.printStackTrace();
				return false;
			}
			

			
			return true;
		}
		
		return false;		
	}


	
	public boolean CASE9_PatientHasDifferentMonarchSwap_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if(DeviceOwnedByDifferentPatient_MONARCH(patientInfoDTO)){
		
			if( (isHillromIdExistInPatientDeviceAssocMonarch(patientInfoDTO.getTims_cust())) && (isHillromIdHasMonarchDeviceInPatientDeviceAssoc(patientInfoDTO.getTims_cust())) ){
				
				log.debug("Inside CASE9_PatientHasDifferentMonarchSwap_MONARCH ");
				//managePatientDeviceMonarch(UPDATE)
				//managePatientDeviceAssociation(CREATE)
				//createPatientProtocolMonarch()
				
				//Question : How do you know whether to pass a Normal or Custom protocol key ?
				try{
					patientInfoDTO.setOperation_type("UPDATE");
					patientInfoDTO.setOld_serial_number(patientDevicesAssocRepository.findByHillromIdAndDeviceType(patientInfoDTO.getTims_cust(), "MONARCH").get().getSerialNumber());
					patientInfoDTO.setNew_serial_number(patientInfoDTO.getSerial_num());
					managePatientDeviceMonarch(patientInfoDTO);
					
					patientInfoDTO.setOperation_type("CREATE");
					managePatientDeviceAssociation(patientInfoDTO);
					
					patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
					patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
							filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
					insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
					patientInfoDTO.setOperation_type("Insert");
					createPatientProtocolMonarch(patientInfoDTO);
				}catch(Exception ex){
					ex.printStackTrace();
					return false;
				}
				
				return true;
			}
			
			return false;
		}
		
		return false;
		
		
	}
	
	public boolean CASE10_PatientHasVisivestAddMonarch_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if(DeviceOwnedByDifferentPatient_MONARCH(patientInfoDTO)){
		
			if( (isHillromIdExistInPatientDeviceAssocMonarch(patientInfoDTO.getTims_cust())) && (!isHillromIdHasMonarchDeviceInPatientDeviceAssoc(patientInfoDTO.getTims_cust())) ){
				
				log.debug("Inside CASE10_PatientHasVisivestAddMonarch_MONARCH ");
				//managePatientDeviceMonarch(CREATE)
				//managePatientDeviceAssociation(CREATE)
				//createPatientProtocolMonarch()
				
				try{
					patientInfoDTO.setOperation_type("CREATE");
					patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
					managePatientDeviceMonarch(patientInfoDTO);
					
					patientInfoDTO.setOperation_type("CREATE");
					managePatientDeviceAssociation(patientInfoDTO);
					
					patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
					patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
							filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
					insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
					patientInfoDTO.setOperation_type("Insert");
					createPatientProtocolMonarch(patientInfoDTO);
				}catch(Exception ex){
					ex.printStackTrace();
					return false;
				}
				
				return true;
			}
			
			return false;
		
		}
		
		return false;
		
	}
	
	public boolean CASE11_PatientExistsWithNODevice_MONARCH(PatientInfoDTO patientInfoDTO){
		
		
		
		if(DeviceOwnedByDifferentPatient_MONARCH(patientInfoDTO)){
		
			if (!isHillromIdExistInPatientDeviceAssocMonarch(patientInfoDTO.getTims_cust())) {
				
				log.debug("Inside CASE11_PatientExistsWithNODevice_MONARCH ");
				//managePatientDeviceMonarch(CREATE)
				//managePatientDeviceAssociation(CREATE)
				//createPatientProtocolMonarch()
				try{
					patientInfoDTO.setOperation_type("CREATE");
					patientInfoDTO.setOld_serial_number(patientInfoDTO.getSerial_num());
					managePatientDeviceMonarch(patientInfoDTO);
					
					patientInfoDTO.setOperation_type("CREATE");
					managePatientDeviceAssociation(patientInfoDTO);
					
					patientInfoDTO.setPatient_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getId());
					patientInfoDTO.setPatient_user_id(patientInfoService.findOneByHillromId(patientInfoDTO.getTims_cust()).get().getUserPatientAssoc().stream().
							filter(userPatientAssoc -> RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())).collect(Collectors.toList()).get(0).getUser().getId().toString());
					insertIntoProtocolDataTempTable(patientInfoDTO.getPatient_id(),"Normal",2,null,5,20,10,14,1,10,0,patientInfoDTO.getPatient_user_id());
					patientInfoDTO.setOperation_type("Insert");
					createPatientProtocolMonarch(patientInfoDTO);
				}catch(Exception ex){
					ex.printStackTrace();
					return false;
				}
				
				return true;
			}
			
			return false;
		
		}
		return false;	
	}

}



