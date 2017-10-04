package com.hillrom.vest.web.rest.FOTA;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import net.minidev.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hillrom.vest.config.FOTA.FOTAConstants;
import com.hillrom.vest.domain.FOTA.FOTAInfo;
import com.hillrom.vest.repository.FOTA.FOTARepository;
import com.hillrom.vest.service.FOTA.FOTAService;
import com.hillrom.vest.web.rest.FOTA.dto.ApproverCRCDto;
import com.hillrom.vest.web.rest.FOTA.dto.CRC32Dto;
import com.hillrom.vest.web.rest.FOTA.dto.FOTADeviceDto;
import com.hillrom.vest.web.rest.FOTA.dto.FOTAInfoDto;
import com.hillrom.vest.web.rest.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class FOTAResource {
	private final Logger log = LoggerFactory.getLogger(FOTAResource.class);

	@Inject
	private FOTAService fotaService;

	@Inject
	private FOTARepository fotaRepository;

	/**
	 * FOTA update
	 * 
	 * @param rawMessage
	 * @return
	 */
	@RequestMapping(value = "/FOTA", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> FOTA(
			@RequestBody(required = true) String rawMessage) {
		String FOTAencoded = " ";
		try {
			FOTAencoded = fotaService.FOTAUpdate(rawMessage);
			return new ResponseEntity<>(FOTAencoded, HttpStatus.OK);

		} catch (Exception ex) {
			ex.printStackTrace();
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.OK);
		}
	}

	/**
	 * POST /FIOTA -> Create New FOTA
	 */
	@RequestMapping(value = "/FOTA/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createFOTA(
			@Valid @RequestBody(required = true) FOTAInfoDto fotaInfoDto,
			HttpServletRequest request) {

		log.debug("REST request to save FOTA info : {}", fotaInfoDto);
		String baseUrl = request.getScheme() + "://" + request.getServerName()
				+ ":" + request.getServerPort();
		JSONObject jsonObject = new JSONObject();

		try {

			FOTAInfo fotaInfo = fotaService.savFotaInfoData(fotaInfoDto,
					baseUrl);
			jsonObject.put("fotaInfo", fotaInfo);
			if (Objects.nonNull(fotaInfo)) {
				jsonObject.put("statusMsg", "fotaInfo created successfully");
				return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);

			}
		} catch (Exception ex) {
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	/**
	 * POST /uploadFile -> receive and locally save a file.
	 * 
	 * @param uploadfile
	 *            The uploaded file as Multipart file parameter in the HTTP
	 *            request. The RequestParam name must be the same of the
	 *            attribute "name" in the input tag with type file.
	 * 
	 * @return An http OK status in case of success, an http 4xx status in case
	 *         of errors.
	 * 
	 *         While calling from pastman pass x-auth-token and name =
	 *         uploadfile . Body should be form-data , uploadfile and ChooseFile
	 */
	@RequestMapping(value = "/FOTA/uploadFile", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> FOTAUploadFile(
			@RequestParam("uploadfile") MultipartFile uploadfile) {

		String filename = null;
		String directory = null;
		String filepath = null;

		JSONObject jsonObject = new JSONObject();

		try {
			// Get the filename and build the local file path
			filename = uploadfile.getOriginalFilename();
			directory = FOTAConstants.FOTA_FILE_PATH;
			File filePathDir = createUniqueDirectory(new File(directory),
					"File");
			filepath = Paths.get(filePathDir.toString(), filename).toString();
			// Save the file locally
			BufferedOutputStream stream = new BufferedOutputStream(
					new FileOutputStream(new File(filepath)));
			stream.write(uploadfile.getBytes());
			stream.close();
			jsonObject.put("filepath", filepath);
			return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);

		} catch (FileNotFoundException ex) {
			jsonObject.put("ERROR",
					"The system cannot find the path/Directory specified");
			return new ResponseEntity<JSONObject>(jsonObject,
					HttpStatus.BAD_REQUEST);
		} catch (Exception ex) {
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject,
					HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Get Firmware list
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/FOTA/{id}/getFirmware", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> getFirmwareDetails(@PathVariable Long id) {
		JSONObject jsonObject = new JSONObject();

		try {
			// Get the firmware
			FOTAInfo fotaInfo = fotaService.getFirmwareDetails(id);
			jsonObject.put("fotaInfo", fotaInfo);
			return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
		} catch (Exception ex) {
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject,
					HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Delete Firmware
	 * 
	 * @param id
	 * @param userRole
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/FOTA/{id}/{userRole}/firmwareDelete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> firmwareDelete(@PathVariable Long id,
			@PathVariable String userRole, HttpServletRequest request) {
		log.debug("REST request to delete FOTA info : {}", id);
		String baseUrl = request.getScheme() + "://" + request.getServerName()
				+ ":" + request.getServerPort();
		JSONObject jsonObject = new JSONObject();

		try {
			// Get the firmware
			FOTAInfo fotaInfo = fotaService.firmwareDelete(id, userRole,
					baseUrl);
			jsonObject.put("fotaInfo", fotaInfo);
			return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
		} catch (Exception ex) {
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject,
					HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * GET /FOTAList
	 */
	@RequestMapping(value = "/FOTAList", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> FOTAList(
			@RequestParam(value = "page", required = false) Integer offset,
			@RequestParam(value = "per_page", required = false) Integer limit,
			@RequestParam(value = "status", required = true) String status,
			@RequestParam(value = "searchString", required = true) String searchString,
			@RequestParam(value = "sort_by", required = false) String sortBy,
			@RequestParam(value = "asc", required = false) boolean isAscending) {
		try {

			List<FOTAInfo> FOTAInfoList = fotaService.FOTAList(status,
					searchString, sortBy, isAscending);

			int firstResult = PaginationUtil.generatePageRequest(offset, limit)
					.getOffset();
			int maxResults = firstResult
					+ PaginationUtil.generatePageRequest(offset, limit)
							.getPageSize();
			List<FOTAInfo> FOTAInfoDtoSubList = new ArrayList<>();
			if (firstResult < FOTAInfoList.size()) {
				maxResults = maxResults > FOTAInfoList.size() ? FOTAInfoList
						.size() : maxResults;
				FOTAInfoDtoSubList = FOTAInfoList.subList(firstResult,
						maxResults);
			}
			Page<FOTAInfo> page = new PageImpl<FOTAInfo>(FOTAInfoDtoSubList,
					PaginationUtil.generatePageRequest(offset, limit),
					Long.valueOf(FOTAInfoList.size()));

			HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
					page, "/FOTAList", offset, limit);
			return new ResponseEntity<>(page, headers, HttpStatus.OK);

		} catch (Exception ex) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * GET /FOTADeviceList
	 */
	@RequestMapping(value = "/FOTADeviceList", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> FOTADeviceList(
			@RequestParam(value = "page", required = false) Integer offset,
			@RequestParam(value = "per_page", required = false) Integer limit,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "searchString", required = true) String searchString,
			@RequestParam(value = "sort_by", required = false) String sortBy,
			@RequestParam(value = "asc", required = false) boolean isAscending) {
		try {
			List<FOTADeviceDto> fotaDeviceList = fotaService.getFOTADeviceList(
					status, searchString, sortBy, isAscending);

			int firstResult = PaginationUtil.generatePageRequest(offset, limit)
					.getOffset();
			int maxResults = firstResult
					+ PaginationUtil.generatePageRequest(offset, limit)
							.getPageSize();

			List<FOTADeviceDto> fotaDeviceSubList = new ArrayList<FOTADeviceDto>();

			if (firstResult < fotaDeviceList.size()) {
				maxResults = maxResults > fotaDeviceList.size() ? fotaDeviceList
						.size() : maxResults;
				fotaDeviceSubList = fotaDeviceList.subList(firstResult,
						maxResults);
			}
			Page<FOTADeviceDto> page = new PageImpl<FOTADeviceDto>(
					fotaDeviceSubList, PaginationUtil.generatePageRequest(
							offset, limit), Long.valueOf(fotaDeviceList.size()));

			HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
					page, "/FOTADeviceList", offset, limit);
			return new ResponseEntity<>(page, headers, HttpStatus.OK);

		} catch (Exception ex) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * 
	 * @param crc32Dt0
	 * @return
	 */
	@RequestMapping(value = "/FOTA/CRC32Calculation", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> CRC32Calculation(
			@Valid @RequestBody(required = true) CRC32Dto crc32Dt0) {
		JSONObject jsonObject = new JSONObject();
		try {
			// Get the filename and build the local file path
			boolean CRC32JsonObject = fotaService.CRC32Calculation(crc32Dt0);
			jsonObject.put("CRC32", CRC32JsonObject);
			log.debug("File Path:" + crc32Dt0.getFilePath());
			// To check existing record
			boolean oldRecord = fotaService.getFotaInfoByPartNumber(crc32Dt0
					.getPartNumber());
			jsonObject.put("oldRecord", oldRecord);
			// Delete upload file from the server system if crc32 is invalid
			if (CRC32JsonObject == false) {
				deleteUploadFile(crc32Dt0.getFilePath());
			}
			return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
		} catch (Exception ex) {
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject,
					HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * 
	 * @param rootDir
	 * @param seed
	 * @return
	 * @throws IOException
	 */
	public synchronized File createUniqueDirectory(File rootDir, String seed)
			throws IOException {
		int index = seed.lastIndexOf('.');
		if (index > 0) {
			seed = seed.substring(0, index);
		}
		File result = null;
		int count = 0;
		while (result == null) {
			String name = seed + "." + count;
			File file = new File(rootDir, name);
			if (!file.exists()) {
				file.mkdirs();
				result = file;
			}
			count++;
		}
		return result;
	}

	/**
	 * deleteUploadFile
	 * 
	 * @param filePath
	 */
	public void deleteUploadFile(String filePath) {
		Path path = FileSystems.getDefault().getPath(filePath);
		try {
			Files.delete(path);
			log.debug("Uploaded File directory deleted successfully");
		} catch (NoSuchFileException x) {
			System.err.format("%s: no such" + " file or directory%n", path);
		} catch (DirectoryNotEmptyException x) {
			System.err.format("%s not empty%n", path);
		} catch (IOException x) {
			// File permission problems are caught here.
			System.err.println(x);
		}

	}

	/**
	 * Get validatApproverCRC32
	 * 
	 * @param apprDto
	 * @return
	 */
	@RequestMapping(value = "/FOTA/validateApproverCRC32", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> validateApproverCRC32(
			@Valid @RequestBody(required = true) ApproverCRCDto apprDto) {
		JSONObject jsonObject = new JSONObject();
		try {
			// Validate approver crc 32
			boolean result = fotaService.validateApproverCRC32(apprDto);
			jsonObject.put("apvrCRC32", result);
			return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
		} catch (Exception ex) {
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<JSONObject>(jsonObject,
					HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * 
	 * @param fileName
	 * @param response
	 */
	@RequestMapping(value = "/FOTA/{id}/download", method = RequestMethod.GET)
	public ResponseEntity<?> FOTAGetFile(@PathVariable Long id,
			HttpServletResponse response) {

		JSONObject jsonObject = new JSONObject();
		try {
			// Save the file locally
			FOTAInfo fotaInfo = null;
			fotaInfo = fotaRepository.findOneById(id);

			InputStream is = new FileInputStream(fotaInfo.getFilePath());
			response.addHeader("Content-disposition", "inline;filename="
					+ fotaInfo.getFilePath());
			response.setContentType("application/octet-stream");
			// copy it to response's OutputStream
			FileCopyUtils.copy(is, response.getOutputStream());
			response.flushBuffer();
			jsonObject.put("response", response);
			return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);

		} catch (IOException ex) {
			jsonObject.put("ERROR", ex.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

	}

}