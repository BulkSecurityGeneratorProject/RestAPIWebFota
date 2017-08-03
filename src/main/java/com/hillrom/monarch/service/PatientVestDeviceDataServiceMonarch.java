package com.hillrom.monarch.service;

import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.CRC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.CRC_FIELD_NAME;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_ADDRESS;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_DATA;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_DATA_FIELD_NAME;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_LTE;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_BT;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_MODEL;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_SERIAL_NUMBER;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_SN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_VER;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEVICE_WIFI;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEV_SN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEV_VER;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DEV_WIFI;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DURATION_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.DURATION_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.END_BATTERY_LEVEL_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.END_BATTERY_LEVEL_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.END_TIME_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.END_TIME_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.EVENT_CODE_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.EVENT_CODE_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.EVENT_LOG_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.EVENT_LOG_START_POS;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.EVENT_TIMESTAMP_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.EVENT_TIMESTAMP_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.FRAG_CURRENT;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.FRAG_CURRENT_FIELD_NAME;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.FRAG_TOTAL;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.FRAG_TOTAL_FIELD_NAME;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.FREQUENCY_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.FREQUENCY_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.HMR_SECONDS_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.HMR_SECONDS_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.HUB_ID;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.HUB_RECEIVE_TIME;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.INTENSITY_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.INTENSITY_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.NUMBER_OF_EVENTS_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.NUMBER_OF_EVENTS_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.NUMBER_OF_PODS_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.NUMBER_OF_PODS_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.SESSION_INDEX_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.SESSION_INDEX_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.START_BATTERY_LEVEL_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.START_BATTERY_LEVEL_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.START_TIME_LEN;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.START_TIME_LOC;
import static com.hillrom.vest.config.PatientVestDeviceRawLogModelConstants.TWO_NET_PROPERTIES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import net.minidev.json.JSONObject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.hillrom.monarch.repository.PatientMonarchDeviceDataRepository;
import com.hillrom.monarch.web.rest.dto.TherapyDataMonarchVO;
import com.hillrom.vest.domain.ChargerData;
import com.hillrom.vest.domain.PatientVestDeviceDataMonarch;
import com.hillrom.vest.domain.PingPongPing;
import com.hillrom.vest.domain.TherapySessionMonarch;
import com.hillrom.vest.exceptionhandler.HillromException;
import com.hillrom.vest.repository.ChargerDataRepository;
import com.hillrom.vest.repository.PingPongPingRepository;
import com.hillrom.vest.service.MailService;
import com.hillrom.vest.service.util.ParserUtil;
import com.hillrom.vest.service.util.RandomUtil;
import com.hillrom.vest.web.rest.PatientVestDeviceDataResource;

@Service
public class PatientVestDeviceDataServiceMonarch {
	
	private final Logger log = LoggerFactory.getLogger(PatientVestDeviceDataResource.class);
	
	@Inject
	private JobLauncher jobLauncher;
	
	@Inject
	private ApplicationContext applicationContext;

	@Inject
	private MailService mailService;

	// Added lated
	@Inject
	private ChargerDataRepository chargerDataRepository;

	@Inject
	private PingPongPingRepository pingPongPingRepository;
	
	@Inject
	private JobLauncher jobLauncherMonarch;
	
	@Inject
	private ApplicationContext applicationContextMonarch;
	// Added lated
	
	@Inject
	private PatientMonarchDeviceDataRepository patientMonarchDeviceDataRepository;



	public String decodeData(final String rawMessage){
		byte[] decoded = java.util.Base64.getDecoder().decode(rawMessage);
		
        String sout = "";
        for(int i=0;i<decoded.length;i++) {
        	int val = decoded[i] & 0xFF;
        	sout = sout + val + " ";
        }
        
        log.debug("Input Byte Array :"+sout);

		String decoded_string = new String(decoded);
		log.error("Decoded value is " + decoded_string);
		return decoded_string;
	}

	public ExitStatus saveData(final String rawData) throws Exception {
		log.debug("saveData has been called , rawData length",rawData.length());
		
		validateRequest(rawData);
		
		Job addNewDataIngestionJobMonarch = applicationContextMonarch.getBean("processMonarchTherapySessionsAndCompliance", Job.class);
		JobParameters jobParametersMonarch = new JobParametersBuilder()
		.addLong("TIME", System.currentTimeMillis())
		.addString("rawData", rawData)
		.toJobParameters();
		JobExecution jobExecution = jobLauncherMonarch.run(addNewDataIngestionJobMonarch, jobParametersMonarch);
		log.debug("Job triggered @ Time ",System.currentTimeMillis());
		ExitStatus exitStatus = jobExecution.getExitStatus();
		// Sending mail Notification on Job Status (ON Success), this code should be removed later
		log.debug("Job triggered @ Time ",exitStatus);
		if(ExitStatus.COMPLETED.equals(exitStatus)){
			mailService.sendStatusOnDataIngestionRequest(rawData, exitStatus.getExitCode(), !ExitStatus.COMPLETED.equals(exitStatus), "");
		}
		return exitStatus;
	}

	private void validateRequest(final String rawData) throws HillromException {
		String decodedData = decodeData(rawData);
		JSONObject qclJsonData = ParserUtil.getChargerJsonDataFromRawMessage(decodedData);
		String reqParams[] = new String[]{DEVICE_MODEL,DEVICE_SN,
				DEVICE_WIFI,DEVICE_LTE,DEVICE_BT,DEVICE_VER,FRAG_TOTAL,FRAG_CURRENT,DEVICE_DATA,CRC};
		if(Objects.isNull(qclJsonData) || qclJsonData.keySet().isEmpty()){
			//throw new HillromException("Missing Params : "+String.join(",",reqParams));
			throw new HillromException("Missing Params");
		}else if(Objects.nonNull(qclJsonData)){
			//JSONObject allProperties = (JSONObject) qclJsonData.getOrDefault(TWO_NET_PROPERTIES, new JSONObject());
			List<String> missingParams = RandomUtil.getDifference(Arrays.asList(reqParams), new ArrayList<String>(qclJsonData.keySet()));
			
			// To check either WIFI / LTE / BT is available
			if( missingParams.contains(DEVICE_WIFI) && !missingParams.contains(DEVICE_LTE) && missingParams.contains(DEVICE_BT)){
				missingParams.remove(DEVICE_WIFI);
				missingParams.remove(DEVICE_BT);
			}else if(missingParams.contains(DEVICE_LTE) && !missingParams.contains(DEVICE_WIFI) && missingParams.contains(DEVICE_BT)){
				missingParams.remove(DEVICE_LTE);
				missingParams.remove(DEVICE_BT);
			}else if(missingParams.contains(DEVICE_LTE) && missingParams.contains(DEVICE_WIFI) && !missingParams.contains(DEVICE_BT)){
				missingParams.remove(DEVICE_LTE);
				missingParams.remove(DEVICE_WIFI);
			}
			
			if(missingParams.size() > 0)
				//throw new HillromException("Missing Params : "+String.join(",",missingParams));
				throw new HillromException("Missing Params");
		}
		if(!calculateCRC(rawData)){
			throw new HillromException("CRC Validation Failed");
		}
	}

	private boolean calculateCRC(String base64String)
	{	
		log.error("Inside  calculateCRC : " ,base64String);
	  
		int nCheckSum = 0;

		byte[] decoded = java.util.Base64.getDecoder().decode(base64String);
    
		int nDecodeCount = 0;
	    for ( ; nDecodeCount < (decoded.length-2); nDecodeCount++ )
	    {
	    	int nValue = (decoded[nDecodeCount] & 0xFF);
	    	nCheckSum += nValue;
	    }    
	    System.out.format("Inverted Value = %d [0X%x] \r\n" ,nCheckSum,nCheckSum);
    
	    while ( nCheckSum >  65535 )
	    {
	    	nCheckSum -= 65535;
	    }
	    
	    int nMSB = decoded[nDecodeCount+1] & 0xFF;
	    int nLSB = decoded[nDecodeCount] & 0xFF;
	    
	    System.out.format("MSB = %d [0x%x]\r\n" ,nMSB, nMSB);
	    System.out.format("LSB = %d [0x%x]\r\n" ,nLSB, nLSB);
	    log.error("Total Value = " + nCheckSum);
	    nCheckSum = ((~nCheckSum)& 0xFFFF) + 1;
	    System.out.format("Checksum Value = %d [0X%x] \r\n" ,nCheckSum,nCheckSum);
	    
	    String msb_digit = Integer.toHexString(nMSB);
	    String lsb_digit = Integer.toHexString(nLSB);
	    String checksum_num =  Integer.toHexString(nCheckSum);
	    
	    if(msb_digit.length()<2)
	    	msb_digit = "0"+msb_digit;
	    if(lsb_digit.length()<2)
	    	lsb_digit = "0"+lsb_digit;
	    
	    System.out.println("MSB : " + msb_digit + " " +  "LSB : " + lsb_digit);
	    System.out.println("Checksum : " + checksum_num);
	    
	    if((msb_digit+lsb_digit).equalsIgnoreCase(checksum_num)){
	    	return true;
	    }else{
	    	log.error("CRC VALIDATION FAILED :"); 
	    	return false;
	    }
	}

	public JSONObject getDeviceData(String encoded_string) throws HillromException{
		
		JSONObject chargerDataPieces = new  JSONObject();
		
        byte[] b = java.util.Base64.getDecoder().decode(encoded_string);
        String sout = "";
        for(int i=0;i<b.length;i++) {
        	int val = b[i] & 0xFF;
        	sout = sout + val + " ";
        }
        
        log.debug("Input Byte Array :"+sout);

        String deviceData = "";
        int start = returnMatch(b,DEVICE_DATA_FIELD_NAME);
        int end = returnMatch(b,CRC_FIELD_NAME)-CRC_FIELD_NAME.length;
        log.debug("start end : "+ start + " : " + end );
        
        byte[] deviceDataArray = new byte[end];
        int j=0;
        for(int i=start;i<end;i++) {
        	deviceDataArray[j++] = b[i];
        	int val = b[i] & 0xFF;
        	deviceData = deviceData + String.valueOf(Character.toChars(val));
        }
        chargerDataPieces.put("deviceData",sout);
        log.debug("deviceData : "+ sout );
        
        if(deviceData.equalsIgnoreCase("PING_PONG_PING")){
        	chargerDataPieces.put("device_data_type","PING_PONG_PING");
        	return chargerDataPieces;
        	//return "PING_PONG_PING";
        }
    	chargerDataPieces.put("device_data_type","NOT_PING_PONG_PING");
    	log.debug("deviceData is NOT PING_PONG_PING" );
        
		int x = getFragTotal(encoded_string);
		int y = getFragCurrent(encoded_string);
		byte[] devsnbt = getDevSN(encoded_string);
		chargerDataPieces.put("serial_number",devsnbt);
		byte[] wifibt = getDevWifi(encoded_string);
		chargerDataPieces.put("device_wifi",wifibt);
		byte[] verbt = getDevVer(encoded_string);
		chargerDataPieces.put("device_ver",verbt);
        
        byte[] session_index  = Arrays.copyOfRange(deviceDataArray, SESSION_INDEX_LOC, SESSION_INDEX_LOC + SESSION_INDEX_LEN);
        sout = "";
        
        for(int k=0;k<session_index.length;k++){
        	sout = sout + (session_index[k]  & 0xFF) + " ";
        }
        chargerDataPieces.put("session_index",sout);
        log.debug("session_index : "+ sout );
        
        log.debug("Combined session_index : "+ intergerCombinedFromHex(session_index));
              
        byte[] start_time  = Arrays.copyOfRange(deviceDataArray, START_TIME_LOC, START_TIME_LOC + START_TIME_LEN);
        sout = "";
        for(int k=0;k<start_time.length;k++){
        	sout = sout + (start_time[k]  & 0xFF) + " ";
        }
        chargerDataPieces.put("start_time",sout);
        log.debug("start_time : "+ sout );
        
        byte[] end_time  = Arrays.copyOfRange(deviceDataArray, END_TIME_LOC, END_TIME_LOC + END_TIME_LEN);        
        sout = "";
        for(int k=0;k<end_time.length;k++){
        	sout = sout + (end_time[k]  & 0xFF) + " ";
        }
        chargerDataPieces.put("end_time",sout);
        log.debug("end_time : "+ sout );
        
        byte[] start_battery_level  = Arrays.copyOfRange(deviceDataArray, START_BATTERY_LEVEL_LOC, START_BATTERY_LEVEL_LOC + START_BATTERY_LEVEL_LEN);
        sout = "";
        for(int k=0;k<start_battery_level.length;k++){
        	sout = sout + (start_battery_level[k]  & 0xFF) + " ";
        }
        chargerDataPieces.put("start_battery_level",sout);
        log.debug("start_battery_level : "+ sout );
        
        byte[] end_battery_level  = Arrays.copyOfRange(deviceDataArray, END_BATTERY_LEVEL_LOC, END_BATTERY_LEVEL_LOC + END_BATTERY_LEVEL_LEN);
        sout = "";
        for(int k=0;k<end_battery_level.length;k++){
        	sout = sout + (end_battery_level[k]  & 0xFF) + " ";
        }
        chargerDataPieces.put("end_battery_level",sout);
        log.debug("end_battery_level : "+ sout );
        
        byte[] number_of_events  = Arrays.copyOfRange(deviceDataArray, NUMBER_OF_EVENTS_LOC, NUMBER_OF_EVENTS_LOC + NUMBER_OF_EVENTS_LEN);
        sout = "";
        for(int k=0;k<number_of_events.length;k++){
        	sout = sout + (number_of_events[k]  & 0xFF) + " ";
        }
        chargerDataPieces.put("number_of_events",sout);
        log.debug("number_of_events : "+ sout );
        
        byte[] number_of_pods  = Arrays.copyOfRange(deviceDataArray, NUMBER_OF_PODS_LOC, NUMBER_OF_PODS_LOC + NUMBER_OF_PODS_LEN);
        sout = "";
        for(int k=0;k<number_of_pods.length;k++){
        	sout = sout + (number_of_pods[k]  & 0xFF) + " ";
        }
        chargerDataPieces.put("number_of_pods",sout);
        log.debug("number_of_pods : "+ sout );
        
        byte[] hmr_seconds  = Arrays.copyOfRange(deviceDataArray, HMR_SECONDS_LOC, HMR_SECONDS_LOC + HMR_SECONDS_LEN);
        sout = "";
        for(int k=0;k<hmr_seconds.length;k++){
        	sout = sout + (hmr_seconds[k]  & 0xFF) + " ";
        }
        
        log.debug("hmr_seconds : "+ sout );
        chargerDataPieces.put("hmr_seconds",intergerCombinedFromHex(hmr_seconds));
        log.debug("Combined hmr_seconds : "+ intergerCombinedFromHex(hmr_seconds));
        
        //log.debug("Value of deviceDataArray.length : "+ j );
        for(int i=EVENT_LOG_START_POS+1;i<j;i=i+EVENT_LOG_LEN){
        	
        	//log.debug("Value of i : "+ i );
        	
	        byte[] event_timestamp  = Arrays.copyOfRange(deviceDataArray, i + EVENT_TIMESTAMP_LOC-1, (i+EVENT_TIMESTAMP_LOC-1) + EVENT_TIMESTAMP_LEN);
	        sout = "";
	        for(int k=0;k<event_timestamp.length;k++){
	        	sout = sout + (event_timestamp[k]  & 0xFF) + " ";
	        }
	        log.debug("event_timestamp : "+ sout );
	        
	        byte[] event_code  = Arrays.copyOfRange(deviceDataArray, i+EVENT_CODE_LOC-1, (i+EVENT_CODE_LOC-1) + EVENT_CODE_LEN);        
	        sout = "";
	        for(int k=0;k<event_code.length;k++){
	        	sout = sout + (event_code[k]  & 0xFF) + " ";
	        }
	        log.debug("event_code : "+ sout );
	        
	        byte[] frequency  = Arrays.copyOfRange(deviceDataArray, i+FREQUENCY_LOC-1, (i+FREQUENCY_LOC-1) + FREQUENCY_LEN);
	        sout = "";
	        for(int k=0;k<frequency.length;k++){
	        	sout = sout + (frequency[k]  & 0xFF) + " ";
	        }
	        log.debug("frequency : "+ sout );

	        
	        byte[] intensity  = Arrays.copyOfRange(deviceDataArray, i+INTENSITY_LOC-1, (i+INTENSITY_LOC-1) + INTENSITY_LEN);
	        sout = "";
	        for(int k=0;k<intensity.length;k++){
	        	sout = sout + (intensity[k]  & 0xFF) + " ";
	        }
	        log.debug("intensity : "+ sout );

	        
	        byte[] duration  = Arrays.copyOfRange(deviceDataArray, i+DURATION_LOC-1, (i+DURATION_LOC-1) + DURATION_LEN);
	        sout = "";
	        for(int k=0;k<duration.length;k++){
	        	sout = sout + (duration[k]  & 0xFF) + " ";
	        }
	        log.debug("duration : "+ sout );
        }
        
    	chargerDataPieces.put("device_data_type","NOT_PING_PONG_PING");
    	return chargerDataPieces;

	}

	public byte[] getDevSN(String encoded_string) throws HillromException{
        byte[] b = java.util.Base64.getDecoder().decode(encoded_string);
        String sout = "";
        for(int i=0;i<b.length;i++) {
        	int val = b[i] & 0xFF;
        	sout = sout + val + " ";
        }
        
        //log.debug("Input Byte Array in devSN :"+sout);

        
        String devSN = "";
        int start = returnMatch(b,DEV_SN);
        int end = returnMatch(b,DEV_WIFI)-DEV_WIFI.length;
        log.debug("start end : "+ start + " : " + end );
        
        byte[] devSNArray = new byte[end];
        int j=0;
        sout = "";
        for(int i=start;i<end;i++) {
        	devSNArray[j++] = b[i];
        	int val = b[i] & 0xFF;
        	devSN = devSN + val + " ";
        }

        
        log.debug("Value of devSN : "+ devSN );
        return devSNArray;
        
	}
	
	public byte[] getDevWifi(String encoded_string) throws HillromException{
        byte[] b = java.util.Base64.getDecoder().decode(encoded_string);
        String sout = "";
        for(int i=0;i<b.length;i++) {
        	int val = b[i] & 0xFF;
        	sout = sout + val + " ";
        }
        
        //log.debug("Input Byte Array in devWifi :"+sout);

        
        String devWifi = "";
        int start = returnMatch(b,DEV_WIFI);
        int end = returnMatch(b,DEV_VER)-DEV_VER.length;
        log.debug("start end : "+ start + " : " + end );
        
        byte[] devWifiArray = new byte[end];
        int j=0;
        sout = "";
        for(int i=start;i<end;i++) {
        	devWifiArray[j++] = b[i];
        	int val = b[i] & 0xFF;
        	devWifi = devWifi + val + " ";
        }

        
        log.debug("Value of devWifi : "+ devWifi );
        return devWifiArray;
        
	}
	
	public byte[] getDevVer(String encoded_string) throws HillromException{
        byte[] b = java.util.Base64.getDecoder().decode(encoded_string);
        String sout = "";
        for(int i=0;i<b.length;i++) {
        	int val = b[i] & 0xFF;
        	sout = sout + val + " ";
        }
        
        //log.debug("Input Byte Array in devVer :"+sout);

        
        String devVer = "";
        int start = returnMatch(b,DEV_VER);
        int end = returnMatch(b,FRAG_TOTAL_FIELD_NAME)-FRAG_TOTAL_FIELD_NAME.length;
        log.debug("start end : "+ start + " : " + end );
        
        byte[] devVerArray = new byte[end];
        int j=0;
        sout = "";
        for(int i=start;i<end;i++) {
        	devVerArray[j++] = b[i];
        	int val = b[i] & 0xFF;
        	devVer = devVer + val + " ";
        }

        
        log.debug("Value of devVer : "+ devVer );
        return devVerArray;
        
	}
	
	public int getFragTotal(String encoded_string) throws HillromException{
        byte[] b = java.util.Base64.getDecoder().decode(encoded_string);
        String sout = "";
        for(int i=0;i<b.length;i++) {
        	int val = b[i] & 0xFF;
        	sout = sout + val + " ";
        }
        
        //log.debug("Input Byte Array in getFragTotal :"+sout);

        
        int start = returnMatch(b,FRAG_TOTAL_FIELD_NAME);
        log.debug("start : "+ start  );
        
        int fragTotal = b[start] & 0xFF;
        
        log.debug("Total number of fragments : "+ fragTotal );
        return fragTotal;
        
	}
	
	public int getFragCurrent(String encoded_string) throws HillromException{
        byte[] b = java.util.Base64.getDecoder().decode(encoded_string);
        String sout = "";
        for(int i=0;i<b.length;i++) {
        	int val = b[i] & 0xFF;
        	sout = sout + val + " ";
        }
        
        //log.debug("Input Byte Array in getFragCurrent :"+sout);

        
        int start = returnMatch(b,FRAG_CURRENT_FIELD_NAME);
        log.debug("start : "+ start  );
        
        int fragCurrent = b[start] & 0xFF;
        
        log.debug("Current fragment number : "+ fragCurrent );
        return fragCurrent;
        
	}
    
    private int returnMatch(byte[] inputArray,byte[] matchArray){

        for(int i=0;i<inputArray.length;i++){
        	int val = inputArray[i] & 0xFF;
        	boolean found = false;
        	
        	if((val == 38) && !found){
        		int j=i;int k=0;
        		while((inputArray[j++]==matchArray[k++]) && (k<matchArray.length)){
        			
        		}
        		if(k==matchArray.length){
        			found = true;
        			return j;
        		}
        	}
        }
        
        return -1;
    	
    }

	public int intergerCombinedFromHex(byte[] input)
	{
	    
	    String hexString =  "";
	    int hexTotal = 0;
	    for (int t = 0; t < input.length; t++)
	    {
	    	hexTotal = hexTotal + Integer.parseInt(Integer.toHexString(input[t]& 0xFF), 16);
	    }
	    log.debug("hexTotal : " + hexTotal);
	    return hexTotal;
	}
	
	public List<PatientVestDeviceDataMonarch> getDeviceDataForAllFragments(Long patientUserId, String serialNumber, int therapyIndex){
		
		List<PatientVestDeviceDataMonarch> allDeviceData = 
				patientMonarchDeviceDataRepository.findByPatientUserIdAndSerialNumberAndTherapyIndex(patientUserId, serialNumber, therapyIndex);
		
		return allDeviceData;
	}

}
