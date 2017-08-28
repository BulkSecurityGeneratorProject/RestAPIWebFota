package com.hillrom.vest.service.FOTA;
import static com.hillrom.vest.config.FOTA.FOTAConstants.ABORTED;
import static com.hillrom.vest.config.FOTA.FOTAConstants.AMPERSAND;
import static com.hillrom.vest.config.FOTA.FOTAConstants.BUFFER_EQ;
import static com.hillrom.vest.config.FOTA.FOTAConstants.BUFFER_LEN_EQ;
import static com.hillrom.vest.config.FOTA.FOTAConstants.CONNECTION_TYPE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.CRC;
import static com.hillrom.vest.config.FOTA.FOTAConstants.CRC_EQ;
import static com.hillrom.vest.config.FOTA.FOTAConstants.DEVICE_PARTNUMBER;
import static com.hillrom.vest.config.FOTA.FOTAConstants.DEVICE_SN;
import static com.hillrom.vest.config.FOTA.FOTAConstants.DEVICE_VER;
import static com.hillrom.vest.config.FOTA.FOTAConstants.FOTA_FILE_PATH;
import static com.hillrom.vest.config.FOTA.FOTAConstants.HANDLE_EQ;
import static com.hillrom.vest.config.FOTA.FOTAConstants.INIT;
import static com.hillrom.vest.config.FOTA.FOTAConstants.NOT_OK;
import static com.hillrom.vest.config.FOTA.FOTAConstants.OK;
import static com.hillrom.vest.config.FOTA.FOTAConstants.PREV_REQ_STATUS;
import static com.hillrom.vest.config.FOTA.FOTAConstants.REQUEST_TYPE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.REQUEST_TYPE1;
import static com.hillrom.vest.config.FOTA.FOTAConstants.REQUEST_TYPE2;
import static com.hillrom.vest.config.FOTA.FOTAConstants.REQUEST_TYPE3;
import static com.hillrom.vest.config.FOTA.FOTAConstants.RESULT;
import static com.hillrom.vest.config.FOTA.FOTAConstants.RESULT_EQ;
import static com.hillrom.vest.config.FOTA.FOTAConstants.SOFT_VER_DATE;
import static com.hillrom.vest.config.FOTA.FOTAConstants.TOTAL_CHUNK;
import static com.hillrom.vest.config.FOTA.FOTAConstants.YES;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;

import net.minidev.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hillrom.vest.domain.FOTA.FOTADeviceFWareUpdate;
import com.hillrom.vest.domain.FOTA.FOTAInfo;
import com.hillrom.vest.exceptionhandler.HillromException;
import com.hillrom.vest.pointer.FOTA.HM_HandleHolder;
import com.hillrom.vest.pointer.FOTA.HM_part01;
import com.hillrom.vest.pointer.FOTA.HandleHolder;
import com.hillrom.vest.pointer.FOTA.PartNoHolder;
import com.hillrom.vest.repository.FOTA.FOTADeviceRepository;
import com.hillrom.vest.repository.FOTA.FOTARepository;
import com.hillrom.vest.service.util.DateUtil;
import com.hillrom.vest.service.util.FOTA.FOTAParseUtil;
import com.hillrom.vest.web.rest.FOTA.dto.CRC32Dto;
import com.hillrom.vest.web.rest.FOTA.dto.FOTAInfoDto;
@Service
@Transactional
public class FOTAService {

	private final Logger log = LoggerFactory.getLogger(FOTAService.class);
	@Inject
	private FOTARepository fotaRepository;
	
	@Inject
	private FOTADeviceRepository fotaDeviceRepository;
	
	
	private  Map<Long,String> storeChunk ;
	private  Map<Long,String> handleHolder ;
	
	private static final int HEX = 16;
	
	public static final byte[] CRC_FIELD_NAME = new byte[]{38,99,114,99,61};
	public static final byte[] CHUNK_SIZE = new byte[]{38,99,104,117,110,107,83,105,122,101,61};
	public static final byte[] HANDLE = new byte[]{38,104,97,110,100,108,101,61};
	
	public static final byte[] DEV_VER = new byte[]{38,100,101,118,86,101,114,61};
	private int bufferLen = 0;
	private String buffer = null;
	
	//Dynamic part number
	private static Map<String,PartNoHolder> partNosBin = new LinkedHashMap<String, PartNoHolder>();
	private static Map<String,HandleHolder> handleHolderBin = new LinkedHashMap<String, HandleHolder>();
	private PartNoHolder partNoHolder;
	
	//private Map<Integer, String> storedChunks;

	public String FOTAUpdate(String rawMessage) throws Exception {
		
		int countInt = 0;
		String decoded_string = "";
		String resultPair = "";
		String handlePair = "";
		String totalChunkPair = "";
		String bufferLenPair = "";
		String bufferPair = "";
		String crcPair = "";
		String finalResponseStr = new String();
		

		Map<String, String> fotaJsonData = new LinkedHashMap<String, String>();
		// Decoding raw data
		decoded_string = decodeRawMessage(rawMessage);
		// Parsing into key value pair
		fotaJsonData = FOTAParseUtil
				.getFOTAJsonDataFromRawMessage(decoded_string);

		// Global handler
		String crsResultValue = "";
		
		// Checking if request Type is 01 & //Checking if request Type is 02
		if(validateCRC(rawMessage)){
			//crcResult = "Yes";
			crsResultValue = asciiToHex(YES);
			String handleId = "";
			boolean softDeleteFlag = false;
			boolean activePublishedFlag = false;
			
			
			//check Update request.
			if (fotaJsonData.get(REQUEST_TYPE).equals(REQUEST_TYPE1)) {
				
				//Get active pending FOTA details from the DB 
				FOTAInfo fotaInfo = fotaRepository.FOTAByPartNumber(fotaJsonData.get(DEVICE_PARTNUMBER),softDeleteFlag,activePublishedFlag);
				
				// Date formating which is from request
				SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(
						"MMddyy");
				simpleDateFormat1.setLenient(false);
				Date date3 = simpleDateFormat1.parse(fotaJsonData
						.get(SOFT_VER_DATE));
				
				// Release date from request
				DateTime reqReleaseDate = new DateTime(date3);
				
				//Check if null or no record exist and send response No to device
				if(fotaInfo != null){
					// Get release date from DB
					DateTime dbRelaseDate = fotaInfo.getReleaseDate();
					// Get Software version from request
					String reqDev = getDeviceVersion(rawMessage);
					//if(!(reqDev.equals(fotaInfo.getSoftVersion())) && (Integer.valueOf(reqDev)<Integer.valueOf(fotaInfo.getSoftVersion()))||((reqReleaseDate.isBefore(dbRelaseDate)|| reqReleaseDate.equals(dbRelaseDate)) && (Integer.valueOf(reqDev)<Integer.valueOf(fotaInfo.getSoftVersion())))){
					if ((fotaInfo.getDevicePartNumber().equals(
							fotaJsonData.get(DEVICE_PARTNUMBER)) && (Integer
							.valueOf(fotaInfo.getSoftVersion()) > Integer
							.valueOf(reqDev)))
							|| (reqDev.equals(fotaInfo.getSoftVersion()) && dbRelaseDate
									.isAfter(reqReleaseDate))) {

						int totalChunks = 0;
						//Generate Handle
						handleId = getHandleNumber();
						// Get Chunk Size from request
						String chunkStr = getChunk(rawMessage);
						// Decimal conversion
						int chunkSize = hex2decimal(chunkStr);
						// PartNumber:Chunk Size
						String storeChunk = fotaJsonData.get(DEVICE_PARTNUMBER)
								.concat(":").concat(fotaInfo.getSoftVersion()).concat(":").concat(String.valueOf(chunkSize));
						boolean crcValid = false;
						partNoHolder = null;
						for(String key : partNosBin.keySet())
						{
							if(key.contains(storeChunk))
							{
								partNoHolder = partNosBin.get(storeChunk);
								
								if(partNoHolder.getAbortFlag()==false &&  partNoHolder.getChunkSize() == chunkSize)
								{
									break;
								}
								else
								{
									partNoHolder = null;
								}
							}
						}
						if (partNoHolder != null) {
							//partNoHolder = partNosBin.get(storeChunk);
							crcValid = partNoHolder.checkCRC32(fotaInfo);
							if ( crcValid == true) {
								// partNosBin.put(storeChunk, partNoHolder);
								// Initially
								HandleHolder holder = new HandleHolder();
								holder.setCurrentChunk(String.valueOf(0));
								holder.setPartNo(partNoHolder.getPart_No());
								holder.setChunkSize(partNoHolder.getChunkSize());
								holder.setFotaInfoId(fotaInfo.getId());
								holder.setDeviceSerialNumber(fotaJsonData
										.get(DEVICE_SN));
								holder.setConnectionType(fotaJsonData
										.get(CONNECTION_TYPE));
								holder.setDeviceSoftwareVersion(reqDev);
								//request Device software date
								holder.setDeviceSoftwareDateTime(reqReleaseDate);
								holder.setUpdatedSoftVersion(fotaInfo.getSoftVersion());
								holder.setCheckupdateDateTime(new DateTime());
								holder.setPreviousChunkTransStatus("CheckUpdate");
								//added new stmt
								holder.setSoftwareVersion(fotaInfo.getSoftVersion());
								handleId = getHandleNumber();
								handleHolderBin.put(handleId, holder);
							} else {
								//Abort case
								partNoHolder.setAbortFlag(true);
							}
						} else {
							partNoHolder = new PartNoHolder(chunkSize, fotaInfo);
							crcValid = partNoHolder.checkCRC32(fotaInfo);
							if ( crcValid == true)
							{
								partNoHolder.setChunkSize(chunkSize);
								partNoHolder.setPart_No(fotaJsonData
										.get(DEVICE_PARTNUMBER));
								partNoHolder.setVersion_No(fotaInfo
										.getSoftVersion());
								partNoHolder.setEffectiveDate(new DateTime());
								// PartNo with Chuck size
								partNosBin.put(storeChunk, partNoHolder);
								// Initially
								HandleHolder holder = new HandleHolder();
								holder.setCurrentChunk(String.valueOf(0));
								holder.setPartNo(partNoHolder.getPart_No());
								holder.setChunkSize(partNoHolder.getChunkSize());
								holder.setFotaInfoId(fotaInfo.getId());
								holder.setDeviceSerialNumber(fotaJsonData
										.get(DEVICE_SN));
								holder.setConnectionType(fotaJsonData
										.get(CONNECTION_TYPE));
								holder.setDeviceSoftwareVersion(reqDev);
								//Request Device software date
								holder.setDeviceSoftwareDateTime(reqReleaseDate);
								holder.setUpdatedSoftVersion(fotaInfo.getSoftVersion());
								holder.setCheckupdateDateTime(new DateTime());
								holder.setPreviousChunkTransStatus("CheckUpdate");
								//added new stmt
								holder.setSoftwareVersion(fotaInfo.getSoftVersion());
								handleId = getHandleNumber();
								handleHolderBin.put(handleId, holder);
							} else {
								//Abort case
								partNoHolder.setAbortFlag(true);
							}
						}

						if (crcValid == false || partNoHolder.getAbortFlag()==true) {
							crsResultValue = asciiToHex("No");
							resultPair = getResponePairResult();
							crcPair = getResponePair3();
							String crsRaw = resultPair.concat(crsResultValue)
									.concat(crcPair);

							byte[] encodedCRC = java.util.Base64.getEncoder()
									.encode(DatatypeConverter
											.parseHexBinary(crsRaw));
							String encodedString = new String(encodedCRC);
							log.debug("encodedString: " + encodedString);
							String crcValue = calculateCRC(encodedString);

							finalResponseStr = resultPair
									.concat(crsResultValue).concat(crcPair)
									.concat(crcValue);
						} else {
							totalChunks = partNoHolder.getTotalChunk();
							// Response pair1
							resultPair = getResponePairResult();

							handlePair = getResponePair1();

							// Handle in raw format
							String handleIdRaw = hexToAscii(asciiToHex(toLittleEndian((handleId))));

							// Response pair2
							totalChunkPair = getResponePair2();

							// Total chunk in raw format
							String totalChunkRaw = getChunkRaw(totalChunks);

							// Response pair3 crc
							crcPair = getResponePair3();

							// CRC calculation
							String crcInput = resultPair.concat(crsResultValue)
									.concat(handlePair).concat(handleIdRaw)
									.concat(totalChunkPair)
									.concat(totalChunkRaw).concat(crcPair);

							byte[] encodedCRC = java.util.Base64.getEncoder()
									.encode(DatatypeConverter
											.parseHexBinary(crcInput));
							String encodedString = new String(encodedCRC);
							log.debug("encodedString: " + encodedString);

							String crcstr = calculateCRC(encodedString);
							// Final response String
							finalResponseStr = getAllResponseCheckUpdate(
									resultPair, crsResultValue, handlePair,
									handleIdRaw, totalChunkPair, totalChunkRaw,
									crcPair, crcstr);
							log.debug("finalResponseStr: " + finalResponseStr);
						}
					}else{
						crsResultValue = asciiToHex("No");
						resultPair = getResponePairResult();
						crcPair = getResponePair3();
						String crsRaw = resultPair.concat(crsResultValue).concat(
								crcPair);

						byte[] encodedCRC = java.util.Base64.getEncoder().encode(
								DatatypeConverter.parseHexBinary(crsRaw));
						String encodedString = new String(encodedCRC);
						log.debug("encodedString: " + encodedString);
						String crcValue = calculateCRC(encodedString);
						
						finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
						}
				}else{
				crsResultValue = asciiToHex("No");
				resultPair = getResponePairResult();
				crcPair = getResponePair3();
				String crsRaw = resultPair.concat(crsResultValue).concat(
						crcPair);

				byte[] encodedCRC = java.util.Base64.getEncoder().encode(
						DatatypeConverter.parseHexBinary(crsRaw));
				String encodedString = new String(encodedCRC);
				log.debug("encodedString: " + encodedString);
				String crcValue = calculateCRC(encodedString);
				
				finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
				}
			}else if(fotaJsonData.get(REQUEST_TYPE).equals(REQUEST_TYPE2)){

					//Get handle from request
					handleId = getHandleFromRequest(rawMessage);
					//Initially 
					HandleHolder holder = new HandleHolder();
					//Get handle object based on handleId
					holder = handleHolderBin.get(handleId);
					//Frame key to get partNumber details
					String storeChunk = holder.getPartNo().concat(":").concat(holder.getSoftwareVersion()).concat(":").concat(String.valueOf(holder.getChunkSize()));
					partNoHolder =  partNosBin.get(storeChunk);
					
				if(partNoHolder.getAbortFlag() == false){
					
					if (fotaJsonData.get(PREV_REQ_STATUS).equals(INIT)) 
					{
						//Get current chunk count from handle holder object
						int chunkCount = Integer.parseInt(holder.getCurrentChunk());
						
						//Get the particular chunk from the based chunk count
						String zeroChunk = partNoHolder.getFileChunks().get(chunkCount);
						
						holder.setCurrentChunk(holder.getCurrentChunk());
						holder.setPreviousChunkTransStatus("INIT");
						holder.setDownloadStartDateTime(new DateTime());
						
						handleHolderBin.put(handleId, holder);
						//Zero the Chunk in raw format
						buffer = hexToAscii(asciiToHex(zeroChunk));
						log.debug("buffer Encoded:" + buffer);
						
						//Chunk size in hex byte
						bufferLen = (zeroChunk.length() / 2);
						log.debug("bufferLen:" + bufferLen);
						
					}else if (fotaJsonData.get(PREV_REQ_STATUS).equals(OK))
						{
							/*//Get handle from request
							handleId = getHandleFromRequest(rawMessage);
							log.debug("handleId from Request:" + handleId);
							
							handleId = getHandleFromRequest(rawMessage);
							//Initially 
							HandleHolder holder = new HandleHolder();
							holder = handleHolderBin.get(handleId);
							
							String storeChunk = holder.getPartNo().concat(":").concat(String.valueOf(holder.getChunkSize()));
							
							partNoHolder =  partNosBin.get(storeChunk);*/
							
							int chunkCount = Integer.parseInt(holder.getCurrentChunk())+1;
							
							String zeroChunk = partNoHolder.getFileChunks().get(chunkCount);
							
							holder.setCurrentChunk(String.valueOf(chunkCount));
							holder.setPreviousChunkTransStatus("OK");
							handleHolderBin.put(handleId, holder);
							
							//Zero the Chunk in raw format
							buffer = hexToAscii(asciiToHex(zeroChunk));
							log.debug("buffer Encoded:" + buffer);
							
							//Chunk size in hex byte
							bufferLen = (zeroChunk.length() / 2);
							log.debug("bufferLen:" + bufferLen);
							
					}else if (fotaJsonData.get(PREV_REQ_STATUS).equals(NOT_OK)) {
						/*//Get handle from request
						handleId = getHandleFromRequest(rawMessage);
						log.debug("handleId from Request:" + handleId);
						
						handleId = getHandleFromRequest(rawMessage);
						//Initially 
						HandleHolder holder = new HandleHolder();
						holder = handleHolderBin.get(handleId);
						
						String storeChunk = holder.getPartNo().concat(":").concat(String.valueOf(holder.getChunkSize()));
						
						partNoHolder =  partNosBin.get(storeChunk);*/
						
						int chunkCount = Integer.parseInt(holder.getCurrentChunk());
						String zeroChunk = partNoHolder.getFileChunks().get(chunkCount);
						
						holder.setCurrentChunk(String.valueOf(chunkCount));
						holder.setPreviousChunkTransStatus("OK");
						handleHolderBin.put(handleId, holder);
						
						//Zero the Chunk in raw format
						buffer = hexToAscii(asciiToHex(zeroChunk));
						log.debug("buffer Encoded:" + buffer);
						
						//Chunk size in hex byte
						bufferLen = (zeroChunk.length() / 2);
						log.debug("bufferLen:" + bufferLen);
						
					} 	
						
					// result pair1
					resultPair = getResponePairResult();
					
					//Init and ok send result is ok
					//crcResult = "OK";
					crsResultValue = asciiToHex(OK);
					
					//handlePair Init Pair1 HANDLE_EQ
					
					handlePair = getResponePair1();
					//handlePair = asciiToHex(HANDLE_EQ);
					
					//Handle in raw format(handle Value)
					String handleIdRaw = hexToAscii(asciiToHex(toLittleEndian((handleId))));
					
					//bufferLenPair Init Pair2(BUFFER_LEN_EQ)
					bufferLenPair = getInitResponsePair2();
					
					//String bufferLenRaw =  hexToAscii(asciiToHex(Integer.toHexString(bufferLen)));
					String bufferLenRaw =  getBufferLenTwoHexByte(bufferLen);
					
					//bufferPair Init Pair2 BUFFER_EQ
					bufferPair = getInitReponsePair3();
					
					//crcPair pair4 Init  crc
					crcPair = getResponePair3();
					
					String crsRaw = resultPair.concat(crsResultValue).concat(handlePair).concat(handleIdRaw).concat(bufferLenPair).concat(bufferLenRaw).concat(bufferPair).concat(buffer).concat(crcPair);
			
				byte[] encodedCRC = java.util.Base64.getEncoder().encode(DatatypeConverter.parseHexBinary(crsRaw));
				String encodedString = new String(encodedCRC);
				log.debug("encodedString: " + encodedString);
				
				String crcstr = calculateCRC(encodedString);
				
				// Final response String
				finalResponseStr = getInitOKResponseSendChunk(resultPair,crsResultValue,handlePair, handleIdRaw,
						bufferLenPair, bufferLenRaw, bufferPair, buffer,crcPair,crcstr,countInt);
				log.debug("finalResponseStr: " + finalResponseStr);
				}else{
					crsResultValue = asciiToHex("ABORT");
					resultPair = getResponePairResult();
					crcPair = getResponePair3();
					String crsRaw = resultPair.concat(crsResultValue).concat(
							crcPair);
					byte[] encodedCRC = java.util.Base64.getEncoder().encode(
							DatatypeConverter.parseHexBinary(crsRaw));
					String encodedString = new String(encodedCRC);
					log.debug("encodedString: " + encodedString);
					String crcValue = calculateCRC(encodedString);
					finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
				}
			
			}else if (fotaJsonData.get(REQUEST_TYPE).equals(REQUEST_TYPE3)) {
				if (fotaJsonData.get(RESULT).equals(OK)) {
					//Get handle from request
					handleId = getHandleFromRequest(rawMessage);
					log.debug("handleId from Request:" + handleId);
					
					//Initially 
					HandleHolder holder = new HandleHolder();
					holder = handleHolderBin.get(handleId);
					FOTADeviceFWareUpdate fotaDeviceFWareUpdate = new FOTADeviceFWareUpdate();
					fotaDeviceFWareUpdate.setFotaInfoId(holder.getFotaInfoId());
					fotaDeviceFWareUpdate.setDeviceSerialNumber(holder.getDeviceSerialNumber());
					fotaDeviceFWareUpdate.setDeviceSoftVersion(holder.getSoftwareVersion());
					fotaDeviceFWareUpdate.setUpdatedSoftVersion(holder.getUpdatedSoftVersion());
					fotaDeviceFWareUpdate.setDeviceSoftwareDateTime(holder.getDeviceSoftwareDateTime());
					fotaDeviceFWareUpdate.setCheckupdateDateTime(holder.getCheckupdateDateTime());
					fotaDeviceFWareUpdate.setDownloadStartDateTime(holder.getDownloadStartDateTime());
					fotaDeviceFWareUpdate.setDownloadEndDateTime(new DateTime());
					fotaDeviceFWareUpdate.setConnectionType(holder.getConnectionType());
					fotaDeviceFWareUpdate.setDownloadStatus("Success");
					
					fotaDeviceRepository.save(fotaDeviceFWareUpdate);
					// result pair1
					resultPair = getResponePairResult();
					//crcResult = "OK";
					crsResultValue = asciiToHex(OK);
					crcPair = getResponePair3();
					
					String crsRaw = resultPair.concat(crsResultValue).concat(crcPair);
					byte[] encodedCRC = java.util.Base64.getEncoder().encode(DatatypeConverter.parseHexBinary(crsRaw));
					String encodedString = new String(encodedCRC);
					log.debug("encodedString: " + encodedString);
					
					String crcValue = calculateCRC(encodedString);
					//Final String 
					finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
				} else if (fotaJsonData.get(RESULT).equals(NOT_OK)) {
					//Get handle from request
					handleId = getHandleFromRequest(rawMessage);
					log.debug("handleId from Request:" + handleId);
					
					//Initially 
					HandleHolder holder = new HandleHolder();
					holder = handleHolderBin.get(handleId);
					FOTADeviceFWareUpdate fotaDeviceFWareUpdate = new FOTADeviceFWareUpdate();
					fotaDeviceFWareUpdate.setFotaInfoId(holder.getFotaInfoId());
					fotaDeviceFWareUpdate.setDeviceSerialNumber(holder.getDeviceSerialNumber());
					fotaDeviceFWareUpdate.setDeviceSoftVersion(holder.getSoftwareVersion());
					fotaDeviceFWareUpdate.setUpdatedSoftVersion(holder.getUpdatedSoftVersion());
					fotaDeviceFWareUpdate.setDeviceSoftwareDateTime(holder.getDeviceSoftwareDateTime());
					fotaDeviceFWareUpdate.setCheckupdateDateTime(holder.getCheckupdateDateTime());
					fotaDeviceFWareUpdate.setDownloadStartDateTime(holder.getDownloadStartDateTime());
					fotaDeviceFWareUpdate.setDownloadEndDateTime(new DateTime());
					fotaDeviceFWareUpdate.setConnectionType(holder.getConnectionType());
					fotaDeviceFWareUpdate.setDownloadStatus("Failure");
					
					fotaDeviceRepository.save(fotaDeviceFWareUpdate);
					
					// result pair1
					resultPair = getResponePairResult();
					//crcResult = "OK";
					crsResultValue = asciiToHex(NOT_OK);
					crcPair = getResponePair3();
					
					String crsRaw = resultPair.concat(crsResultValue).concat(crcPair);

					byte[] encodedCRC = java.util.Base64.getEncoder().encode(
							DatatypeConverter.parseHexBinary(crsRaw));
					String encodedString = new String(encodedCRC);
					log.debug("encodedString: " + encodedString);
					String crcValue = calculateCRC(encodedString);
					
					//Final String 
					finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
				} else if (fotaJsonData.get(RESULT).equals(ABORTED)) {

					//Get handle from request
					handleId = getHandleFromRequest(rawMessage);
					log.debug("handleId from Request:" + handleId);
					
					//Initially 
					HandleHolder holder = new HandleHolder();
					holder = handleHolderBin.get(handleId);
					FOTADeviceFWareUpdate fotaDeviceFWareUpdate = new FOTADeviceFWareUpdate();
					fotaDeviceFWareUpdate.setFotaInfoId(holder.getFotaInfoId());
					fotaDeviceFWareUpdate.setDeviceSerialNumber(holder.getDeviceSerialNumber());
					fotaDeviceFWareUpdate.setDeviceSoftVersion(holder.getSoftwareVersion());
					fotaDeviceFWareUpdate.setUpdatedSoftVersion(holder.getUpdatedSoftVersion());
					fotaDeviceFWareUpdate.setDeviceSoftwareDateTime(holder.getDeviceSoftwareDateTime());
					fotaDeviceFWareUpdate.setCheckupdateDateTime(holder.getCheckupdateDateTime());
					fotaDeviceFWareUpdate.setDownloadStartDateTime(holder.getDownloadStartDateTime());
					fotaDeviceFWareUpdate.setDownloadEndDateTime(new DateTime());
					fotaDeviceFWareUpdate.setConnectionType(holder.getConnectionType());
					fotaDeviceFWareUpdate.setDownloadStatus("Aborted");
					
					fotaDeviceRepository.save(fotaDeviceFWareUpdate);
					
					// result pair1
					resultPair = getResponePairResult();
					//crcResult = "OK";
					crsResultValue = asciiToHex(OK);
					crcPair = getResponePair3();
					
					String crsRaw = resultPair.concat(crsResultValue).concat(crcPair);

					byte[] encodedCRC = java.util.Base64.getEncoder().encode(
							DatatypeConverter.parseHexBinary(crsRaw));
					String encodedString = new String(encodedCRC);
					log.debug("encodedString: " + encodedString);
					String crcValue = calculateCRC(encodedString);
					
					//Final String 
					finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
				
					
				}
			}
		
		}else if(!validateCRC(rawMessage)){
			//crcResult = "NOT OK";
			crsResultValue = asciiToHex(NOT_OK);
			resultPair = getResponePairResult();
			crcPair = getResponePair3();
			String crsRaw = resultPair.concat(crsResultValue).concat(
					crcPair);

			byte[] encodedCRC = java.util.Base64.getEncoder().encode(
					DatatypeConverter.parseHexBinary(crsRaw));
			String encodedString = new String(encodedCRC);
			log.debug("encodedString: " + encodedString);
			String crcValue = calculateCRC(encodedString);
			
			finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
		}
		
			byte[] encoded = java.util.Base64.getEncoder().encode(DatatypeConverter.parseHexBinary(finalResponseStr));
			String finalString1 = new String(encoded);
			log.error("finalString1: " + finalString1);
			return finalString1;
		
	
		
	}
	
	public String FOTAUpdate1(String rawMessage) {
		
		int countInt = 0;
		String decoded_string = "";
		String resultPair = "";
		String handlePair = "";
		String totalChunkPair = "";
		String bufferLenPair = "";
		String bufferPair = "";
		String crcPair = "";
		String finalResponseStr = new String();
		

		Map<String, String> fotaJsonData = new LinkedHashMap<String, String>();

		// Decoding raw data
		decoded_string = decodeRawMessage(rawMessage);
		// Parsing into key value pair
		fotaJsonData = FOTAParseUtil
				.getFOTAJsonDataFromRawMessage(decoded_string);

		// Global handler
		HM_HandleHolder globalHandleHolder = HM_HandleHolder.getInstance();
		
		String crsResultValue = "";
		
		// Checking if request Type is 01 & //Checking if request Type is 02
		
		if(validateCRC(rawMessage)){
			//crcResult = "Yes";
			crsResultValue = asciiToHex(YES);
			
			//
			
			//Get Fota Details based on part numbers.
			
			if (fotaJsonData.get(REQUEST_TYPE).equals(REQUEST_TYPE1)) {
			//String diviceVer = getDeviceVersion(rawMessage);
			//FOTAInfo fotaInfo = getFotaInforByPartNumber(fotaJsonData.get(DEVICE_PARTNUMBER),false);
			FOTAInfo fotaInfo = fotaRepository.findFOTAInfo(fotaJsonData.get(DEVICE_PARTNUMBER),true);
			String reqDev = getDeviceVersion(rawMessage);
			if(reqDev.equals(fotaInfo.getSoftVersion()) ||  (fotaInfo != null)){
			if(fotaJsonData.get(DEVICE_PARTNUMBER).equals(fotaInfo.getDevicePartNumber())){
				int totalChunks = 0;
				HM_part01 hmp01 = HM_part01.getInstance(rawMessage,fotaJsonData.get(REQUEST_TYPE),fotaInfo.getFilePath());
				
				Map<String,String> partNumberWithCount = new LinkedHashMap<String, String>();
				String handleId = getHandleNumber();
				SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yyyy.HH.mm.ss");
				
				partNumberWithCount.put(fotaJsonData.get(DEVICE_PARTNUMBER), String.valueOf(0));
				partNumberWithCount.put("TimeStamp", sdf.format(new Date()));
				
				globalHandleHolder.getHandleWithPartNumber().put(handleId,partNumberWithCount);
				
				totalChunks = hmp01.getTotalChunk();
				// Response pair1
				resultPair = getResponePairResult();
				
				handlePair = getResponePair1();
				
				// Handle in raw format
				String handleIdRaw = hexToAscii(asciiToHex(toLittleEndian((handleId))));

				// Response pair2
				totalChunkPair = getResponePair2();

				// Total chunk in raw format
				String totalChunkRaw = getChunkRaw(totalChunks);

				// Response pair3 crc
				crcPair = getResponePair3();
				
				//CRC calculation 
				String crcInput = resultPair.concat(crsResultValue).concat(handlePair).concat(handleIdRaw).concat(totalChunkPair).concat(totalChunkRaw).concat(crcPair);
				
				byte[] encodedCRC = java.util.Base64.getEncoder().encode(DatatypeConverter.parseHexBinary(crcInput));
				String encodedString = new String(encodedCRC);
				log.debug("encodedString: " + encodedString);
				
				String crcstr = calculateCRC(encodedString);
				// Final response String
				finalResponseStr = getAllResponseCheckUpdate(resultPair,crsResultValue,handlePair, handleIdRaw,
						totalChunkPair, totalChunkRaw, crcPair, crcstr);
				log.debug("finalResponseStr: " + finalResponseStr);
				
				}
			}else{
				crsResultValue = asciiToHex("NO");
				resultPair = getResponePairResult();
				crcPair = getResponePair3();
				String crsRaw = resultPair.concat(crsResultValue).concat(
						crcPair);

				byte[] encodedCRC = java.util.Base64.getEncoder().encode(
						DatatypeConverter.parseHexBinary(crsRaw));
				String encodedString = new String(encodedCRC);
				log.debug("encodedString: " + encodedString);
				String crcValue = calculateCRC(encodedString);
				
				finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
			}
			}else if(fotaJsonData.get(REQUEST_TYPE).equals(REQUEST_TYPE2)){
				String handleId = "";
				int chunkCount = 0;
				if (fotaJsonData.get(PREV_REQ_STATUS).equals(INIT)) {
					//Get handle from request
					handleId = getHandleFromRequest(rawMessage);
					log.debug("handleId from Request:" + handleId);
					
					Map<String,String> partNumberWithCount = globalHandleHolder.getHandleWithPartNumber().get(handleId);
					
					Map<String,String> updatePartNumberWithCount = new LinkedHashMap<String, String>();
					
					for(Map.Entry<String,String> partDetail : partNumberWithCount.entrySet()){
						/*if(partDetail.getKey().equals(fotaInfo.getDevicePartNumber()) && partDetail.getValue().equals(String.valueOf(chunkCount))){*/
						if(partDetail.getValue().equals(String.valueOf(chunkCount))){
							updatePartNumberWithCount.put(partDetail.getKey(), String.valueOf(chunkCount));
							globalHandleHolder.getHandleWithPartNumber().put(handleId,updatePartNumberWithCount);
							HM_part01 hmp01 = HM_part01.getInstance(rawMessage,fotaJsonData.get(REQUEST_TYPE),"");
							String zeroChunk = hmp01.getFileChunks().get(chunkCount);
							//Zero the Chunk in raw format
							buffer = hexToAscii(asciiToHex(zeroChunk));
							log.debug("buffer Encoded:" + buffer);
							
							//Chunk size in hex byte
							bufferLen = (zeroChunk.length() / 2);
							log.debug("bufferLen:" + bufferLen);
							
						} 
					}
				}else if (fotaJsonData.get(PREV_REQ_STATUS).equals(OK)) {
						//Get handle from request
						handleId = getHandleFromRequest(rawMessage);
						log.debug("handleId from Request:" + handleId);
						
						Map<String,String> partNumberWithCount = globalHandleHolder.getHandleWithPartNumber().get(handleId);
						
						Map<String,String> updatePartNumberWithCount = new LinkedHashMap<String, String>();
						for(Map.Entry<String,String> partDetail : partNumberWithCount.entrySet()){
							/*if(partDetail.getKey().equals(fotaInfo.getDevicePartNumber())){*/
								//Part Number
								HM_part01 hmp01 = HM_part01.getInstance(rawMessage,fotaJsonData.get(REQUEST_TYPE),"");
								String countStr = partDetail.getValue();
								countInt = Integer.parseInt(countStr);
								countInt = countInt + 1;
								
								updatePartNumberWithCount.put(partDetail.getKey(), String.valueOf(countInt));
								globalHandleHolder.getHandleWithPartNumber().put(handleId,updatePartNumberWithCount);
								
								String okSendChunk = hmp01.getFileChunks().get(countInt);
								//OK Chunk in raw format
								buffer = hexToAscii(asciiToHex(okSendChunk));
								log.debug("buffer Encoded:" + buffer);
								
								//Chunk size in hex byte
								bufferLen = (okSendChunk.length() / 2);
								log.debug("bufferLen:" + bufferLen);
							//} 
						}	
						
				} else if (fotaJsonData.get(PREV_REQ_STATUS).equals(NOT_OK)) {
						//Get handle from request
						handleId = getHandleFromRequest(rawMessage);
						log.debug("handleId from Request:" + handleId);
						Map<String,String> partNumberWithCount = globalHandleHolder.getHandleWithPartNumber().get(handleId);
						
						Map<String,String> updatePartNumberWithCount = new LinkedHashMap<String, String>();
						for(Map.Entry<String,String> partDetail : partNumberWithCount.entrySet()){
							/*if(partDetail.getKey().equals(fotaInfo.getDevicePartNumber())){*/
								//Part Number
								HM_part01 hmp01 = HM_part01.getInstance(rawMessage,fotaJsonData.get(REQUEST_TYPE),"");
								String countStr = partDetail.getValue();
								countInt = Integer.parseInt(countStr);
								//countInt = countInt + 1;
								updatePartNumberWithCount.put(partDetail.getKey(), String.valueOf(countInt));
								globalHandleHolder.getHandleWithPartNumber().put(handleId,updatePartNumberWithCount);
								
								//Previous Chunk
								String okSendChunk = hmp01.getFileChunks().get(countInt);
								
								//Buffer values
								buffer = hexToAscii(asciiToHex(okSendChunk));
								log.debug("buffer Encoded:" + buffer);

								//Chunk size in hex byte
								bufferLen = (okSendChunk.length() / 2);
								log.debug("bufferLen:" + bufferLen);
								
							//} 
						}	
				}
				// result pair1
				resultPair = getResponePairResult();
				
				//Init and ok send result is ok
				//crcResult = "OK";
				crsResultValue = asciiToHex(OK);
				
				//handlePair Init Pair1 HANDLE_EQ
				
				handlePair = getResponePair1();
				//handlePair = asciiToHex(HANDLE_EQ);
				
				//Handle in raw format(handle Value)
				String handleIdRaw = hexToAscii(asciiToHex(toLittleEndian((handleId))));
				
				//bufferLenPair Init Pair2(BUFFER_LEN_EQ)
				bufferLenPair = getInitResponsePair2();
				
				//String bufferLenRaw =  hexToAscii(asciiToHex(Integer.toHexString(bufferLen)));
				String bufferLenRaw =  getBufferLenTwoHexByte(bufferLen);
				
				//bufferPair Init Pair2 BUFFER_EQ
				bufferPair = getInitReponsePair3();
				
				//crcPair pair4 Init  crc
				crcPair = getResponePair3();
				
				String crsRaw = resultPair.concat(crsResultValue).concat(handlePair).concat(handleIdRaw).concat(bufferLenPair).concat(bufferLenRaw).concat(bufferPair).concat(buffer).concat(crcPair);
				
				byte[] encodedCRC = java.util.Base64.getEncoder().encode(DatatypeConverter.parseHexBinary(crsRaw));
				String encodedString = new String(encodedCRC);
				log.debug("encodedString: " + encodedString);
				
				String crcstr = calculateCRC(encodedString);
				
				// Final response String
				finalResponseStr = getInitOKResponseSendChunk(resultPair,crsResultValue,handlePair, handleIdRaw,
						bufferLenPair, bufferLenRaw, bufferPair, buffer,crcPair,crcstr,countInt);
				log.debug("finalResponseStr: " + finalResponseStr);
				
			}else if (fotaJsonData.get(REQUEST_TYPE).equals(REQUEST_TYPE3)) {
				if (fotaJsonData.get(RESULT).equals(OK)) {
					// result pair1
					resultPair = getResponePairResult();
					//crcResult = "OK";
					crsResultValue = asciiToHex(OK);
					crcPair = getResponePair3();
					
					String crsRaw = resultPair.concat(crsResultValue).concat(crcPair);
					
					byte[] encodedCRC = java.util.Base64.getEncoder().encode(DatatypeConverter.parseHexBinary(crsRaw));
					String encodedString = new String(encodedCRC);
					log.debug("encodedString: " + encodedString);
					
					String crcValue = calculateCRC(encodedString);
					//Final String 
					finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
				} else if (fotaJsonData.get(RESULT).equals(NOT_OK)) {
					// result pair1
					resultPair = getResponePairResult();
					//crcResult = "OK";
					crsResultValue = asciiToHex(NOT_OK);
					crcPair = getResponePair3();
					
					String crsRaw = resultPair.concat(crsResultValue).concat(
							crcPair);

					byte[] encodedCRC = java.util.Base64.getEncoder().encode(
							DatatypeConverter.parseHexBinary(crsRaw));
					String encodedString = new String(encodedCRC);
					log.debug("encodedString: " + encodedString);
					String crcValue = calculateCRC(encodedString);
					
					//Final String 
					finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
				}
			}
		
		}else if(!validateCRC(rawMessage)){
			//crcResult = "NOT OK";
			crsResultValue = asciiToHex(NOT_OK);
			resultPair = getResponePairResult();
			crcPair = getResponePair3();
			String crsRaw = resultPair.concat(crsResultValue).concat(
					crcPair);

			byte[] encodedCRC = java.util.Base64.getEncoder().encode(
					DatatypeConverter.parseHexBinary(crsRaw));
			String encodedString = new String(encodedCRC);
			log.debug("encodedString: " + encodedString);
			String crcValue = calculateCRC(encodedString);
			
			finalResponseStr = resultPair.concat(crsResultValue).concat(crcPair).concat(crcValue);
		}
		
			byte[] encoded = java.util.Base64.getEncoder().encode(DatatypeConverter.parseHexBinary(finalResponseStr));
			String finalString1 = new String(encoded);
			log.error("finalString1: " + finalString1);
			return finalString1;
		
	}
	
	private String getDeviceVersion(String rawMessage) {

		byte[] getHandleByte = java.util.Base64.getDecoder().decode(rawMessage);
		int deviceIndex = returnMatch(getHandleByte, DEV_VER);
		log.error("str1: " + deviceIndex);
		StringBuilder deviceRes = new StringBuilder();
		String device1 = Integer.toHexString(getHandleByte[deviceIndex] & 0xFF);
		String device2 = Integer
				.toHexString(getHandleByte[deviceIndex + 1] & 0xFF);
		String device3 = Integer
				.toHexString(getHandleByte[deviceIndex + 2] & 0xFF);
		String device4 = Integer
				.toHexString(getHandleByte[deviceIndex + 3] & 0xFF);
				
		device1 =	("00"+ device1).substring(device1.length());
		device2 =	("00"+ device2).substring(device2.length());
		device3 =	("00"+ device3).substring(device3.length());
		device4 =	("00"+ device4).substring(device4.length());
		deviceRes.append(device1);
		deviceRes.append(device2);
		deviceRes.append(device3);
		deviceRes.append(device4);
		//written new code
		String deviceVer = toLittleEndian(deviceRes.toString());
		log.error("deviceVer: " + deviceVer);
		return deviceVer;
	}

	public boolean getFotaInfoByPartNumber(String partNumber) {
		boolean softDeleteFlag = false;
		boolean activePublishedFlag = false;
		boolean oldVersion = false;
		FOTAInfo fotaInfo = fotaRepository.FOTAByPartNumber(partNumber,softDeleteFlag,activePublishedFlag);
		if(fotaInfo != null){
			if(fotaInfo.getSoftDeleteFlag() == false && fotaInfo.getActivePublishedFlag() == false){
				oldVersion = true;
			}
		}
		return oldVersion;
	}

	private String calculateCRC(String encodedString) {
		 
		log.debug("Inside  calculateCRC : " ,encodedString);
		  
	    int nCheckSum = 0;
	    byte[] decoded = java.util.Base64.getDecoder().decode(encodedString);
	    
	    int nDecodeCount = 0;
	    for ( ; nDecodeCount <= (decoded.length-1); nDecodeCount++ )
	    {
	      int nValue = (decoded[nDecodeCount] & 0xFF);
	      nCheckSum += nValue;
	    }
	    
	    System.out.format("Inverted Value = %d [0X%x] \r\n" ,nCheckSum,nCheckSum);
	    nCheckSum = nCheckSum & 0xFFFF;
	    log.error("Total Value = " + nCheckSum);
	    nCheckSum = ((~nCheckSum)& 0xFFFF) + 1;
	    System.out.format("Checksum Value = %d [0X%x] \r\n" ,nCheckSum,nCheckSum);

	    String checksum_num =  Integer.toHexString(nCheckSum);
	    //String handleHexString = checksum_num.toString(16);
	    checksum_num = ("0000" + checksum_num).substring(checksum_num.length());
	    System.out.println("Checksum : " + checksum_num);
	    return toLittleEndian(checksum_num);
	  
	}

	private String getResponePairResult() {
		
		String getResponePairResult = asciiToHex(RESULT_EQ);
		log.error("getResponePairResult: " + getResponePairResult);
		//response.append("Yes");
		return getResponePairResult;
	}

	private boolean validateCRC(String rawMessage) {
		 
		log.error("Inside  calculateCRC : " ,rawMessage);
		  
	    int nCheckSum = 0;

	    byte[] decoded = java.util.Base64.getDecoder().decode(rawMessage);
	    
	    int nDecodeCount = 0;
	    for ( ; nDecodeCount < (decoded.length-2); nDecodeCount++ )
	    {
	      int nValue = (decoded[nDecodeCount] & 0xFF);
	      nCheckSum += nValue;
	    }
	    
	    
	    System.out.format("Inverted Value = %d [0X%x] \r\n" ,nCheckSum,nCheckSum);
	    
	   /* while ( nCheckSum >  65535 )
	    {
	      nCheckSum -= 65535;
	    }*/
	    nCheckSum = nCheckSum & 0xFFFF;
	    
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
	    checksum_num = ("0000" + checksum_num).substring(checksum_num.length());
	    
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
	
	private String validateInvalideCRC(String rawMessage) {
		 
		log.error("Inside  calculateCRC : " ,rawMessage);
		  
	    int nCheckSum = 0;

	    byte[] decoded = java.util.Base64.getDecoder().decode(rawMessage);
	    
	    int nDecodeCount = 0;
	    for ( ; nDecodeCount < (decoded.length-2); nDecodeCount++ )
	    {
	      int nValue = (decoded[nDecodeCount] & 0xFF);
	      nCheckSum += nValue;
	    }
	    
	    
	    System.out.format("Inverted Value = %d [0X%x] \r\n" ,nCheckSum,nCheckSum);
	    
	   /* while ( nCheckSum >  65535 )
	    {
	      nCheckSum -= 65535;
	    }*/
	    nCheckSum = nCheckSum & 0xFFFF;
	    
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
	    checksum_num = ("0000" + checksum_num).substring(checksum_num.length());
	    
	    return checksum_num;
	   /* if((msb_digit+lsb_digit).equalsIgnoreCase(checksum_num)){
	    	return true;
	    }else{
	    	log.error("CRC VALIDATION FAILED :"); 
	    	return false;
	    }*/
	}


	private String getBufferLenTwoHexByte(int bufferLen) {
		//Convert to hex
		String bufferLenHex =	Integer.toHexString(bufferLen);
		//convert in two byte format
		bufferLenHex = ("0000" + bufferLenHex).substring(bufferLenHex.length());
		//converting to little Endian 
		String bufferInLsb = hexToAscii(asciiToHex(toLittleEndian((bufferLenHex))));
		return bufferInLsb;
	}

	private String getInitOKResponseSendChunk(String resultPair,
			String crsResultValue, String handlePair, String handleIdRaw,
			String bufferLenPair, String bufferLenRaw, String bufferPair,
			String buffer, String crcPair, String crcstr, int countInt) {
		
		//Final String 
		//String finalString = responsePair1.concat(handleIdRaw).concat(responsePair2).concat(bufferLenRaw).concat(responsePair3).concat(buffer).concat(responsePair4).concat(crcRaw);
		String finalString = resultPair.concat(crsResultValue)
				.concat(handlePair).concat(handleIdRaw).concat(bufferLenPair)
				.concat(bufferLenRaw).concat(bufferPair).concat(buffer)
				.concat(crcPair).concat(crcstr);
		log.debug(" Chunk Number:"+countInt);
		log.debug(" CRC value:"+crcstr);
		return finalString;
	}


	
	
	private String getHandleFromRequest(String rawMessage) {

		byte[] getHandleByte = java.util.Base64.getDecoder().decode(rawMessage);
		int handleIndex = returnMatch(getHandleByte, HANDLE);
		log.error("str1: " + handleIndex);
		StringBuilder handleRes = new StringBuilder();
		// handleRes.
	/*	handleRes
				.append(Integer.toHexString(getHandleByte[handleIndex] & 0xFF));
		handleRes.append(Integer
				.toHexString(getHandleByte[handleIndex + 1] & 0xFF));
		handleRes.append(Integer
				.toHexString(getHandleByte[handleIndex + 2] & 0xFF));
		handleRes.append(Integer
				.toHexString(getHandleByte[handleIndex + 3] & 0xFF));*/
		
		String handle1 = Integer.toHexString(getHandleByte[handleIndex] & 0xFF);
		String handle2 = Integer
				.toHexString(getHandleByte[handleIndex + 1] & 0xFF);
		String handle3 = Integer
				.toHexString(getHandleByte[handleIndex + 2] & 0xFF);
		String handle4 = Integer
				.toHexString(getHandleByte[handleIndex + 3] & 0xFF);
				
		handle1 =	("00"+ handle1).substring(handle1.length());
		handle2 =	("00"+ handle2).substring(handle2.length());
		
		
		handle3 =	("00"+ handle3).substring(handle3.length());
		handle4 =	("00"+ handle4).substring(handle4.length());
		handleRes.append(handle1);
		handleRes.append(handle2);
		handleRes.append(handle3);
		handleRes.append(handle4);
		//written new code
		String handleId = toLittleEndian(handleRes.toString());
		/*BigInteger toHex = new BigInteger(handleId,10);
	    String handleIdString = toHex.toString(16);*/
		//handleId = ("00000000"+ handleId).substring(handleId.length());
		//String handleIdStringHex = hexToAscii(asciiToHex(handleId));
		log.error("handleId: " + handleId);
		return handleId;
	}


	private String getInitReponsePair3() {
		StringBuilder response = new StringBuilder();	
		response.append(AMPERSAND);
		response.append(BUFFER_EQ);
		String initResponsePair3 = asciiToHex(response.toString());
		return initResponsePair3;
	}


	private String getInitResponsePair2() {
		StringBuilder response = new StringBuilder();	
		response.append(AMPERSAND);
		response.append(BUFFER_LEN_EQ);
		String initResponsePair3 = asciiToHex(response.toString());
		return initResponsePair3;
	}


	private String getAllResponseCheckUpdate(String responsePair1Result,String responsePair1ResultValue, String responsePair1, String handleIdRaw,
			String responsePair2, String totalChunkRaw, String responsePair3,
			String crcRaw) {
		/*StringBuilder sb = new StringBuilder();
		sb.*/
		log.debug("totalChunkRaw"+totalChunkRaw);
		
		String finalString = responsePair1Result.concat(responsePair1ResultValue).concat(responsePair1).concat(handleIdRaw).concat(responsePair2)
				.concat(totalChunkRaw).concat(responsePair3).concat(crcRaw);
		return finalString;
	}


	private String getCRC(String rawMessage) {
		byte[] getCRCByte = java.util.Base64.getDecoder().decode(rawMessage);
		int start = returnMatch(getCRCByte, CRC_FIELD_NAME);
		int start1 = returnMatch(getCRCByte, CRC_FIELD_NAME) + 1;
		/*StringBuilder crcHexBilder = new StringBuilder();

		crcHexBilder.append(Integer.toHexString(getCRCByte[start] & 0xFF));
		crcHexBilder.append(Integer.toHexString(getCRCByte[start1] & 0xFF));*/
		
		String crc1 = Integer.toHexString(getCRCByte[start] & 0xFF);
		String crc2 = Integer.toHexString(getCRCByte[start1] & 0xFF);
				
		crc1 =	("00"+ crc1).substring(crc1.length());
		crc2 =	("00"+ crc2).substring(crc2.length());
		StringBuilder sb = new StringBuilder();
		sb.append(crc1);
		sb.append(crc2);
		log.error("crcHexBilder: " + sb.toString());
		//To convert little Indian
		//String crcHexString = hexToAscii(asciiToHex(toLittleEndian((sb.toString()))));
		String crcHexString = hexToAscii(asciiToHex((sb.toString())));
		log.error("crcHexString: " + crcHexString);
		return crcHexString;
	}


	private String getResponePair3() {
		StringBuilder response = new StringBuilder();
		response.append(AMPERSAND);
		response.append(CRC_EQ);
		String responePair3 = asciiToHex(response.toString());
		log.error("responePair3: " + responePair3);
		return responePair3;
	}


	private String getChunkRaw(int totalChunks) {
		BigInteger toHex = new BigInteger(String.valueOf(totalChunks),10);
	    String totalChunkHexString = toHex.toString(16);
	    totalChunkHexString = ("00000000" + totalChunkHexString).substring(totalChunkHexString.length());
		//converting to little Indian
	    String strTotalChunk = hexToAscii(asciiToHex(toLittleEndian((totalChunkHexString))));
		log.error("strTotalChunk: " + strTotalChunk);
		return strTotalChunk;
	}


	private String getResponePair2() {
		StringBuilder response = new StringBuilder();
		response.append(AMPERSAND);
		response.append(TOTAL_CHUNK);
		String responePair2 = asciiToHex(response.toString());
		log.error("responePair2: " + responePair2);
		return responePair2;
	}


	private String getResponePair1() {
		StringBuilder response = new StringBuilder();
		/*response.append(RESULT_EQ);
		response.append("Yes");*/
		response.append(AMPERSAND);
		response.append(HANDLE_EQ);
		String responePair1 = asciiToHex(response.toString());
		log.error("responePair1: " + responePair1);
		return responePair1;
	}

	//Generating unique handle id
	private String getHandleNumber() {
		Random random = new Random();
		int random1 = random.nextInt(10000);
		int random2 = random.nextInt(1000);
		int uniqueRandom = random1+random2;
        BigInteger toHex = new BigInteger(String.valueOf(uniqueRandom),10);	
        String handleHexString = toHex.toString(16);
        handleHexString = ("00000000" + handleHexString).substring(handleHexString.length());
        //String lsb = getLSBValue(handleHexString);
        String handleInlsb = (handleHexString);
        return handleInlsb;
	}

	
	
	private  String toLittleEndian(final String hex) {
	    //int ret = 0;
	    String hexLittleEndian = "";
	    if (hex.length() % 2 != 0) return hexLittleEndian;
	    for (int i = hex.length() - 2; i >= 0; i -= 2) {
	        hexLittleEndian += hex.substring(i, i + 2);
	    }
	   // ret = Integer.parseInt(hexLittleEndian, 16);
	    return hexLittleEndian;
	}
	
	private String getLSBValue(String handleHexString) {
		int value = Integer.parseInt(handleHexString, 16);
		// Flip byte order using ByteBuffer
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.asIntBuffer().put(value);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		int flipped = buffer.asIntBuffer().get();
		log.debug("handleHexString:"+handleHexString);
		log.debug("flipped:"+flipped);
		BigInteger bigInt = new BigInteger(String.valueOf(flipped), 10);
		String str = bigInt.toString(16);
		System.out.println(str);

		return null;
	}

	//To read non readable character
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
	// Convert String to ASCII
	private  String asciiToHex(String asciiValue)
    {
        char[] chars = asciiValue.toCharArray();
        StringBuffer hex = new StringBuffer();
        for (int i = 0; i < chars.length; i++)
        {
            hex.append(Integer.toHexString((int) chars[i]));
        }
        return hex.toString();

	}
	//Make it ready for raw data 
	private String hexToAscii(String hexStr) {
		 String str = "";
		 StringBuilder output = new StringBuilder("");
		 try{
			 for (int i = 0; i < hexStr.length(); i+=2) {
			        str = hexStr.substring(i, i+2);
			        output.append((char)Integer.parseInt(str, 16));
			    }
			    System.out.println(output);
			 
		 }catch(Exception ex){

		 }
		 return new String(output.toString());
		}

	//DecodeRawMessage
	private String decodeRawMessage(String rawMessage) {
		String decoded_string = "";
		byte[] decoded = java.util.Base64.getDecoder().decode(rawMessage);
		String sout = "";
		for (int i = 0; i < decoded.length; i++) {
			int val = decoded[i] & 0xFF;
			sout = sout + val + " ";
		}
		log.debug("Input Byte Array :" + sout);
		decoded_string = new String(decoded);
		log.error("Decoded value is " + decoded_string);
		return decoded_string;
	}
	
	
	////Initial code starts
	public JSONObject processHexaToByteData(String HexaFilePath, Integer lines)
			throws HillromException {
		byte[] byteArray = new byte[906800];
		JSONObject jsonObject = new JSONObject();
		long count = 0;
		String hexDataStr = "";
	    String [] output = null;
	    String encodedData = null;
	    int flag = 1;
	    if(flag == 0){
	    	try {
				Path pp = FileSystems.getDefault().getPath(HexaFilePath,
						"193164_charger_mainboard.hex");
				FileInputStream fis = new FileInputStream(pp.toFile());
				int len;
				// Read bytes until EOF is encountered.
				do {
					
					len = fis.read(byteArray);

					hexDataStr = getDataInHexString(byteArray);
	
				} while (len != -1);

				fis.close();
			} catch (Exception ex) {
				ex.printStackTrace();
				log.error("Error in Ecoded bas64 data :" + ex.getMessage());
			}
	    	output = hexDataStr.split("(?<=\\G.{512})");
			storeChunk = new HashMap<Long, String>();
			for(String str :output ){
				storeChunk.put(count++, str);
				
				log.debug("Output into chnuks :" + str);
			}
			log.debug("Output into chnuks :" + storeChunk.size());
	    }
		
		//log.debug("Count :" + count);
		for (Map.Entry<Long, String> entry : storeChunk.entrySet())
		{
			if(entry.getKey() == 1652){
				log.debug("Output into chnuks :" + entry.getValue());
				BigInteger bigint = new BigInteger(entry.getValue(), 16);
				count = entry.getValue().length()/2;
				StringBuilder sb = new StringBuilder();
				byte[] bytes = Base64.encodeInteger(bigint);
				for (byte b : bytes) {
					sb.append((char) b);
				}
				encodedData = new String(sb.toString());
				log.debug("Ecoded bas64 data :" + encodedData);
				
			}
		}
		jsonObject.put("Base64 Encoded ", encodedData);
		jsonObject.put("ChunkSize:", count);
		return jsonObject;
	}

	private String getDataInHexString(byte[] byteArray) {
		String data = "";
		String trimData = "";
		try {
			data = new String(byteArray, 0, byteArray.length);
			trimData = data.replace(":", "").replace("\n", "")
					.replace("\r", "");
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Error with getReadIntelHexFile:" + ex.getMessage());
		}
		return trimData;
	}

	private byte[] convertToBas64Encode(byte[] bas64Input) {
		byte[] encoded = null;
		if (bas64Input != null) {
			encoded = Base64.encodeBase64(bas64Input);
		}
		return encoded;
	}

	private byte[] convertToBas64Decode(byte[] bas64Input) {
		byte[] decoded = null;
		if (bas64Input != null) {
			decoded = Base64.decodeBase64(bas64Input);
		}
		return decoded;
	}

	private String getStringFormatWithSapce(List<Integer> decimalValueList) {

		StringBuilder strbul = new StringBuilder();
		String bas64Input = "";
		Iterator<Integer> iter = decimalValueList.iterator();
		while (iter.hasNext()) {
			strbul.append(iter.next());
			if (iter.hasNext()) {
				strbul.append(" ");
			}
		}
		bas64Input = strbul.toString();

		return bas64Input;
	}

	private List<Integer> getReadIntelHexFile(byte[] byteArray) {
		String data = "";
		String trimData = "";
		List<Integer> decimalValueList = new LinkedList<Integer>();
		try {
			data = new String(byteArray, 0, byteArray.length, "ASCII");
			trimData = data.replace(":", "").replace("\n", "")
					.replace("\r", "");

			String val = "2";
			String result = trimData.replaceAll("(.{" + val + "})", "$1 ")
					.trim();
			log.debug("Haxa Decimal with Space:" + result);

			String[] hexaValues = trimData.split(" ", -1);
			
			for (String hex : hexaValues) {
				
				
				int outputDecimal = Integer.parseInt(hex, 16);
				decimalValueList.add(outputDecimal);

			}
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Error with getReadIntelHexFile:" + ex.getMessage());
		}

		return decimalValueList;
	}

	public JSONObject checkUpdate(String rawMessage) {
		String decoded_string = "";
		JSONObject FOTAJsonData = new JSONObject();
		Map<String, String> fotaJsonData = new LinkedHashMap<String, String>();
		byte[] decoded = java.util.Base64.getDecoder().decode(rawMessage);
		String sout = "";
		for (int i = 0; i < decoded.length; i++) {
			int val = decoded[i] & 0xFF;
			sout = sout + val + " ";
		}
		log.debug("Input Byte Array :" + sout);
		decoded_string = new String(decoded);
		log.error("Decoded value is " + decoded_string);
		fotaJsonData = FOTAParseUtil
				.getFOTAJsonDataFromRawMessage(decoded_string);
		StringBuilder resposeString = new StringBuilder();
		long totalChunk = readHexByteDataFromFile();
		log.error("totalChunk: " + totalChunk);
		
		Random rand = new Random();
		int  handleValue = rand.nextInt(100) + 1;
		resposeString.append(RESULT);
		resposeString.append("Yes");
		resposeString.append("&");
		resposeString.append(HANDLE);
		resposeString.append(handleValue);
		resposeString.append("&");
		resposeString.append(TOTAL_CHUNK);
		resposeString.append(totalChunk);
		resposeString.append("&");
		resposeString.append(CRC);
		String crc = "";
		for (Map.Entry<String, String> entry : fotaJsonData.entrySet()) {
			if (entry.getKey().equals(CRC)) {
				log.debug("CRC:" + entry.getValue());
				crc = entry.getValue();
				break;
			}
		}
		resposeString.append(crc);
		byte[] encoded = java.util.Base64.getEncoder().encode(
				resposeString.toString().getBytes());
		String encodedCheckUpdate = new String(encoded);
		log.error("encodedCheckUpdate: " + encodedCheckUpdate);
		FOTAJsonData.put("encodedCheckUpdate", encodedCheckUpdate);
		return FOTAJsonData;
	}

	private long readHexByteDataFromFile() {
		byte[] byteArray = new byte[906800];
		long count = 0;
		long totalChunk = 0L;
		String hexDataStr = "";
		String[] output = null;
		try {
			Path pp = FileSystems.getDefault().getPath(FOTA_FILE_PATH,
					"193164_charger_mainboard.hex");
			FileInputStream fis = new FileInputStream(pp.toFile());
			int len;
			// Read bytes until EOF is encountered.
			do {

				len = fis.read(byteArray);

				hexDataStr = getDataInHexString(byteArray);
				log.debug("hexa String :" + hexDataStr);

			} while (len != -1);

			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Error in Ecoded bas64 data :" + ex.getMessage());
		}
		output = hexDataStr.split("(?<=\\G.{512})");
		
		storeChunk = new LinkedHashMap<Long, String>();
		
		for (String str : output) {
			storeChunk.put(count++, str);
			log.debug("Output into chunk :" + str);
		}
		totalChunk = storeChunk.size()-1;
		log.debug("totalChunk :" + totalChunk);
		return totalChunk;
	}

	public FOTAInfo savFotaInfoData(FOTAInfoDto fotaInfoDto)
			throws ParseException {
		//check is existing if yes update to inactive pending
		if(fotaInfoDto.getOldRecord() == true){
			FOTAInfo fotaInfo = fotaRepository.FOTAByPartNumber(fotaInfoDto.getDevicePartNumber(),false,false);
			if(Objects.nonNull(fotaInfo))
	  	  {
				fotaInfo.setSoftDeleteFlag(true);
				fotaInfo.setUploadDatetime(DateUtil.getCurrentDateAndTime());
				fotaRepository.save(fotaInfo);
	  	        log.debug("updated fotaInfo Details: with inactive pending {}", fotaInfo);
	  	  }
			String storeChunk = fotaInfo.getDevicePartNumber().concat(":").concat(fotaInfo.getSoftVersion());
			
			for(String key : partNosBin.keySet()){
				
				if(key.contains(storeChunk)){
					partNoHolder = partNosBin.get(key);
					log.debug("key :"+key);
				}
			}
			partNoHolder.setAbortFlag(true);
			log.debug("Abort flag is set:"+partNoHolder.getAbortFlag());
		}
		FOTAInfo fotaInfo = new FOTAInfo();
		fotaInfo.setDevicePartNumber(fotaInfoDto.getDevicePartNumber());
		fotaInfo.setSoftVersion(fotaInfoDto.getSoftVersion());
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMddyy");
		simpleDateFormat.setLenient(false);
		Date date2 = simpleDateFormat.parse(fotaInfoDto.getReleaseDate());
		System.out.println("Date String 2 is '" + date2);
		DateTime dt = new DateTime(date2);
		System.out.println("Dt is '" + dt);
		fotaInfo.setReleaseDate(dt);
		
		fotaInfo.setProductType(fotaInfoDto.getProductType());

		fotaInfo.setFilePath(fotaInfoDto.getFilePath());

		fotaInfo.setUploadUser(fotaInfoDto.getUploadUser());

		fotaInfo.setUploadDatetime(DateUtil.getCurrentDateAndTime());
		
		if(StringUtils.isNotEmpty(fotaInfoDto.getPublishedUser())){
			fotaInfo.setPublishedUser(fotaInfoDto.getPublishedUser());
			fotaInfo.setPublishedDateTime(DateUtil.getCurrentDateAndTime());
		}
		
		fotaInfo.setModelId(fotaInfoDto.getModelId());
		fotaInfo.setBoardId(fotaInfoDto.getBoardId());
		fotaInfo.setBedId(fotaInfoDto.getBedId());
		fotaInfo.setBootCompVer(fotaInfoDto.getBootCompVer());
		fotaInfo.setFillPattern(fotaInfoDto.getFillPattern());
		fotaInfo.setMCUSize(fotaInfoDto.getmCUSize());
		fotaInfo.setReleaseNumber(fotaInfoDto.getReleaseNumber());
		//added new attribute
		fotaInfo.setSoftDeleteFlag(false);
		if(StringUtils.isNotEmpty(fotaInfoDto.getRegion1StartAddress())){
			fotaInfo.setRegion1StartAddress(fotaInfoDto.getRegion1StartAddress());
		}
		if(StringUtils.isNotEmpty(fotaInfoDto.getRegion1EndAddress())){
			fotaInfo.setRegion1EndAddress(fotaInfoDto.getRegion1EndAddress());
		}
		if(StringUtils.isNotEmpty(fotaInfoDto.getRegion1CRCLocation())){
			fotaInfo.setRegion1CRCLocation(fotaInfoDto.getRegion1CRCLocation());
		}
		if(StringUtils.isNotEmpty(fotaInfoDto.getRegion2StartAddress())){
			fotaInfo.setRegion2StartAddress(fotaInfoDto.getRegion2StartAddress());
		}
		if(StringUtils.isNotEmpty(fotaInfoDto.getRegion2EndAddress())){
			fotaInfo.setRegion2EndAddress(fotaInfoDto.getRegion2EndAddress());
		}
		if(StringUtils.isNotEmpty(fotaInfoDto.getRegion2CRCLocation())){
			fotaInfo.setRegion2CRCLocation(fotaInfoDto.getRegion2CRCLocation());
		}
		
		fotaRepository.save(fotaInfo);
		log.debug("Created New Fota: {}", fotaInfo);
		return fotaInfo;
	}

	public FOTAInfo softDeleteFOTA(String partNo, boolean isOldFile) {
		FOTAInfo fotaInfo = fotaRepository.findFOTAInfo(partNo,isOldFile);
		if(Objects.nonNull(fotaInfo))
  	  {
			fotaInfo.setSoftDeleteFlag(false);
			fotaInfo.setUploadDatetime(DateUtil.getCurrentDateAndTime());
			fotaRepository.save(fotaInfo);
  	        log.debug("updated fotaInfo Details: {}", fotaInfo);
  	  }
		return fotaInfo;
	}
	private String getChunk(String rawMessage) {

		byte[] getChunkByte = java.util.Base64.getDecoder().decode(rawMessage);
		int chunkByteIndex = returnMatch(getChunkByte, CHUNK_SIZE);
		log.error("chunkByteIndex: " + chunkByteIndex);
		// StringBuilder handleRes = new StringBuilder();
		// handleRes.
		// handleRes.append(Integer.toHexString(getChunkByte[chunkSize] &
		// 0xFF));
		int chunkSizeValue = getChunkByte[chunkByteIndex] & 0xFF;
		int chunkSizeValue1 = getChunkByte[chunkByteIndex + 1] & 0xFF;

		String chunkSize1 = Integer.toHexString(chunkSizeValue);
		String chunkSize2 = Integer.toHexString(chunkSizeValue1);

		chunkSize1 = ("00" + chunkSize1).substring(chunkSize1.length());
		chunkSize2 = ("00" + chunkSize2).substring(chunkSize2.length());

		StringBuilder sb = new StringBuilder();
		sb.append(chunkSize1);
		sb.append(chunkSize2);

		String littleEndianChunk = toLittleEndian(sb.toString());
		return littleEndianChunk;

	}
	
	private int hex2decimal(String chunkStr) {

		String digits = "0123456789ABCDEF";
		chunkStr = chunkStr.toUpperCase();
		int val = 0;
		for (int i = 0; i < chunkStr.length(); i++) {
			char c = chunkStr.charAt(i);
			int d = digits.indexOf(c);
			val = 16 * val + d;
		}
		return val;
	}

	public List<FOTADeviceFWareUpdate> getFOTADeviceList(String status) {
		
		List<FOTADeviceFWareUpdate> FOTADeviceList = null;
		
		if(status.equals("Success")){
			FOTADeviceList = new ArrayList<FOTADeviceFWareUpdate>();
			FOTADeviceList = fotaDeviceRepository.getFOTADeviceListByStatus(status);
		}else if(status.equals("Failure")){
			FOTADeviceList = new ArrayList<FOTADeviceFWareUpdate>();
			FOTADeviceList = fotaDeviceRepository.getFOTADeviceListByStatus(status);
			
		}else if(status.equals("All")){
			String statusSuccess = "Success";
			String statusFailure = "Failure";
			FOTADeviceList = new ArrayList<FOTADeviceFWareUpdate>();
			FOTADeviceList = fotaDeviceRepository.getFOTADeviceListByAll(statusSuccess,statusFailure);
			
		}
		
		return FOTADeviceList;
	}

	public List<FOTAInfoDto> FOTAList(String status) {
		List<FOTAInfoDto> FOTAInfoDtoList = null;
		List<FOTAInfo> FOTAInfoList = null;
		if(status.equals("Active")){
			FOTAInfoList = new ArrayList<FOTAInfo>();
			FOTAInfoDtoList = new ArrayList<FOTAInfoDto>();
			FOTAInfoList = fotaRepository.getFOTAListByStatus(true);
			for(FOTAInfo info : FOTAInfoList){
				FOTAInfoDto infoDto = new FOTAInfoDto();
				infoDto.setBedId(info.getBedId());
				infoDto.setBoardId(info.getBoardId());
				infoDto.setBootCompVer(info.getBootCompVer());
				infoDto.setDevicePartNumber(info.getDevicePartNumber());
				/*DateFormat sdf = new SimpleDateFormat("yyyy/MM-dd'T'HH:mm:ss.SSSXXX");
				
				Date startTime = sdf.parse("handleHolderBin.get(handleId).getDownloadStartTime()");*/
				//infoDto.setEffectiveDate(String.valueOf(info.getEffectiveDatetime()));
				infoDto.setFilePath(info.getFilePath());
				//infoDto.setFilePattern(info.getFilePattern());
				infoDto.setmCUSize(info.getMCUSize());
				//infoDto.setChecksum(info.getChecksum());
				infoDto.setReleaseNumber(info.getReleaseNumber());
				infoDto.setSoftVersion(info.getSoftVersion());
				infoDto.setReleaseDate(String.valueOf(info.getReleaseDate()));
				infoDto.setUploadUser(info.getUploadUser());
				infoDto.setModelId(info.getModelId());
				//infoDto.setOldSoftVerFlag(info.getOldSoftFlag());
				FOTAInfoDtoList.add(infoDto);
			}
			
			
		}else if(status.equals("All")){
			FOTAInfoList = new ArrayList<FOTAInfo>();
			FOTAInfoList = fotaRepository.getFOTAListByStatus(true,false);
			
			for(FOTAInfo info : FOTAInfoList){
				FOTAInfoDto infoDto = new FOTAInfoDto();
				FOTAInfoDtoList = new ArrayList<FOTAInfoDto>();
				infoDto.setBedId(info.getBedId());
				infoDto.setBoardId(info.getBoardId());
				infoDto.setBootCompVer(info.getBootCompVer());
				infoDto.setDevicePartNumber(info.getDevicePartNumber());
				/*DateFormat sdf = new SimpleDateFormat("yyyy/MM-dd'T'HH:mm:ss.SSSXXX");
				
				Date startTime = sdf.parse("handleHolderBin.get(handleId).getDownloadStartTime()");*/
				//infoDto.setEffectiveDate(String.valueOf(info.getEffectiveDatetime()));
				infoDto.setFilePath(info.getFilePath());
				//infoDto.setFilePattern(info.getFilePattern());
				infoDto.setmCUSize(info.getMCUSize());
				//infoDto.setChecksum(info.getChecksum());
				infoDto.setReleaseNumber(info.getReleaseNumber());
				infoDto.setSoftVersion(info.getSoftVersion());
				infoDto.setReleaseDate(String.valueOf(info.getReleaseDate()));
				infoDto.setUploadUser(info.getUploadUser());
				infoDto.setModelId(info.getModelId());
				//infoDto.setOldSoftVerFlag(info.getOldSoftFlag());
				FOTAInfoDtoList.add(infoDto);
			}
			
		}
		return FOTAInfoDtoList;
	}
	//CRC 32 validation method
	@SuppressWarnings("resource")
	public boolean CRC32Calculation(CRC32Dto crc32Dt0) throws Exception {
	    boolean eof = false;
	    boolean result = false;
	    int recordIdx = 0;
	    long upperAddress = 0;
	   /* long crcStartAddress  = 0x08010070;
	    long crcEndAddress = 0x080FFFFF;
	    long crcLocationAddress = 0x0801006C;*/
	    long crcStartAddress  = 0;
	    long crcEndAddress = 0;
	    long crcLocationAddress = 0;
	    
	    int crcValueInFile = 0;
	    long dataStartAddress = 0;

	   /* long crc2StartAddress = 0x60000070;
	    long crc2EndAddress = 0x60FFFFFF;
	    long crc2LocationAddress = 0x6000006C;*/
	    long crc2StartAddress = 0;
	    long crc2EndAddress = 0;
	    long crc2LocationAddress = 0;
	    
	    int crc2ValueInFile = 0;
	    long data2StartAddress = 0;

	    ByteArrayOutputStream crcData = new ByteArrayOutputStream();
	    ByteArrayOutputStream crcData2 = new ByteArrayOutputStream();

	    int record_length;
	    int record_address;
	    byte[] record_data;
	    
	    FileInputStream fs = null;
	    
	    
		if (StringUtils.isNotEmpty(crc32Dt0.getRegion1StartAddress())) {
			crcStartAddress = Long.parseLong(crc32Dt0.getRegion1StartAddress(),16);
		}
		if (StringUtils.isNotEmpty(crc32Dt0.getRegion1EndAddress())) {
			crcEndAddress = Long.parseLong(crc32Dt0.getRegion1EndAddress(),16);
		}
		
		if (StringUtils.isNotEmpty(crc32Dt0.getRegion1CRCLocation())) {
			crcLocationAddress = Long.parseLong(crc32Dt0.getRegion1CRCLocation(),16);
		}
		if (StringUtils.isNotEmpty(crc32Dt0.getRegion2StartAddress())) {
			crc2StartAddress = Long.parseLong(crc32Dt0.getRegion2StartAddress(),16);
		}
		if (StringUtils.isNotEmpty(crc32Dt0.getRegion2EndAddress())) {
			crc2EndAddress = Long.parseLong(crc32Dt0.getRegion2EndAddress(),16);
		}
		
		if (StringUtils.isNotEmpty(crc32Dt0.getRegion2CRCLocation())) {
			crc2LocationAddress = Long.parseLong(crc32Dt0.getRegion2CRCLocation(),16);
		}
		
	    
		
		fs = new FileInputStream(crc32Dt0.getFilePath());
		
	    InputStreamReader isr = new InputStreamReader(fs);
	    BufferedReader rdr =  new BufferedReader(isr);
        eof = false;
        recordIdx = 1;
        upperAddress = 0;
        String recordStr;
        while ((recordStr = rdr.readLine()) != null) {
            if (eof) {
                throw new Exception("Data after eof (" + recordIdx + ")");
            }

            if (!recordStr.startsWith(":")) {
                throw new Exception("Invalid Intel HEX record (" + recordIdx + ")");
            }

            int lineLength = recordStr.length();
            byte[] hexRecord = new byte[lineLength / 2];

            int sum = 0;
            for (int i = 0; i < hexRecord.length; i++) {
                String num = recordStr.substring(2 * i + 1, 2 * i + 3);
                hexRecord[i] = (byte) Integer.parseInt(num, HEX);
                sum += hexRecord[i] & 0xff;
            }
            sum &= 0xff;
        	
            if (sum != 0) {
                throw new Exception("Invalid checksum (" + recordIdx + ")");
            }

            record_length = hexRecord[0];
            if ((record_length + 5) != hexRecord.length) {
                throw new Exception("Invalid record length (" + recordIdx + ")");
            }
            record_data = new byte[record_length];
            System.arraycopy(hexRecord, 4, record_data, 0, record_length);
    		

            record_address = ((hexRecord[1] & 0xFF) << 8) + (hexRecord[2] & 0xFF);

            long addr = record_address | upperAddress;
            switch (hexRecord[3] & 0xFF) {
                case 0:
                	 long tmpAddr = addr;
                	 long tmp2Addr = addr;
                	  for(byte c: record_data)
                	  {
                		  if(tmpAddr >= crcLocationAddress && tmpAddr< (crcLocationAddress+4))
                		  {
                			   int diff = (int)(tmpAddr-crcLocationAddress);
                			  	switch(diff)
                			  	{
                			  	case 0:
                			  		crcValueInFile = ((int)c&0xFF);
                			  		break;
                			  	case 1:
                			  		crcValueInFile |= (((int)c&0xFF) << 8);
                			  		break;
                			  	case 2:
                			  		crcValueInFile |= (((int)c&0xFF) << 16);
                			  		break;
                			  	case 3:
                			  		crcValueInFile |= (((int)c&0xFF)<< 24);
                			  		break;
                			  	default:
                			  			
                			  	}
                		  }
                		  tmpAddr++;
                		  
                    	  if(addr>=crcStartAddress && addr<= crcEndAddress)
                    	  {
                    		  if(dataStartAddress==0)
                    		  {
                    			  dataStartAddress = addr;
                    		  }
                    		  crcData.write(c);
                    	  }

                		  if(tmp2Addr >= crc2LocationAddress && tmp2Addr< (crc2LocationAddress+4))
                		  {
                			   int diff = (int)(tmp2Addr-crc2LocationAddress);
                			  	switch(diff)
                			  	{
                			  	case 0:
                			  		crc2ValueInFile = ((int)c&0xFF);
                			  		break;
                			  	case 1:
                			  		crc2ValueInFile |= (((int)c&0xFF) << 8);
                			  		break;
                			  	case 2:
                			  		crc2ValueInFile |= (((int)c&0xFF) << 16);
                			  		break;
                			  	case 3:
                			  		crc2ValueInFile |= (((int)c&0xFF) << 24);
                			  		break;
                			  	default:
                			  			
                			  	}
                		  }
                		  tmp2Addr++;
                		  
                    	  if(addr>=crc2StartAddress && addr<= crc2EndAddress)
                    	  {
                    		  if(data2StartAddress==0)
                    		  {
                    			  data2StartAddress = addr;
                    		  }
                    		  crcData2.write(c);
                    	  }                    	  
                	  }
                    break;
                case 1:
                    break;
                case 2:
                    if (record_length == 2) {
                        upperAddress = ((record_data[0] & 0xFF) << 12) +( ((record_data[1] & 0xFF)) << 4);
                    } else {
                        throw new Exception("Invalid SEG record (" + recordIdx + ")");
                    }
                    break;                	
                case 4:
                    if (record_length == 2) {
                        upperAddress = ((record_data[0] & 0xFF) << 24) +( ((record_data[1] & 0xFF)) << 16);
                    } else {
                        throw new Exception("Invalid EXT_LIN record (" + recordIdx + ")");
                    }
                    break;
                default:
                    break;
            }
            recordIdx++;
        };
        rdr.close();
        isr.close();
        fs.close();
        
        // CRC Calculation Table initialize
    int crc;
    int i;
    if(crcStartAddress != 0){
        byte [] crcBytes = crcData.toByteArray();

        crc  = 0xFFFFFFFF;       // initial contents of LFBSR
        int poly = 0xEDB88320;   // reverse polynomial

        for (byte b : crcBytes) {
            int temp = (crc ^ b) & 0xff;

            // read 8 bits one at a time
            for (i = 0; i < 8; i++) {
                if ((temp & 1) == 1) temp = (temp >>> 1) ^ poly;
                else                 temp = (temp >>> 1);
            }
            crc = (crc >>> 8) ^ temp;
        }
      
        long tmpEndAddress = dataStartAddress + crcBytes.length;
        while(tmpEndAddress<=(crcEndAddress))
        {
            int temp = (crc ^ 0xFF) & 0xff;

        	for (i = 0; i < 8; i++) {
                if ((temp & 1) == 1) temp = (temp >>> 1) ^ poly;
                else                 temp = (temp >>> 1);
            }
            crc = (crc >>> 8) ^ temp;
            tmpEndAddress++;
        }

        // flip bits
        crc = crc ^ 0xffffffff;

		result = (crc == crcValueInFile);
		crcData.close();
       log.debug("Calculated Region1CRC32: " + (String.format("0x%08X", crc)) + "In file :" + (String.format("0x%08X", (crcValueInFile))));
        
    }
    if(crc2StartAddress != 0 && result == true){
    	byte [] crc2Bytes = crcData2.toByteArray();

        crc  = 0xFFFFFFFF;       // initial contents of LFBSR
        int poly2 = 0xEDB88320;   // reverse polynomial

        for (byte b : crc2Bytes) {
            int temp = (crc ^ b) & 0xff;

            // read 8 bits one at a time
            for (i = 0; i < 8; i++) {
                if ((temp & 1) == 1) temp = (temp >>> 1) ^ poly2;
                else                 temp = (temp >>> 1);
            }
            crc = (crc >>> 8) ^ temp;
        }
      
        long tmp2EndAddress = data2StartAddress + crc2Bytes.length;
        while(tmp2EndAddress<=(crc2EndAddress))
        {
            int temp = (crc ^ 0xFF) & 0xff;

        	for (i = 0; i < 8; i++) {
                if ((temp & 1) == 1) temp = (temp >>> 1) ^ poly2;
                else                 temp = (temp >>> 1);
            }
            crc = (crc >>> 8) ^ temp;
            tmp2EndAddress++;
        }
        // flip bits
        crc = crc ^ 0xffffffff;
        log.debug("Calculated Region2CRC32: " + (String.format("0x%08X", crc)) + "In file :" + (String.format("0x%08X", (crc2ValueInFile))));
        result &= (crc == crc2ValueInFile);
        crcData2.close();
    }
		return result;
	}
}
