package com.hillrom.vest.service;

import static com.hillrom.vest.security.AuthoritiesConstants.PATIENT;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.hillrom.vest.domain.PatientInfo;
import com.hillrom.vest.domain.PatientNoEvent;
import com.hillrom.vest.domain.PatientVestDeviceData;
import com.hillrom.vest.domain.PatientVestDeviceRawLog;
import com.hillrom.vest.domain.TempPatientVestDeviceData;
import com.hillrom.vest.domain.TherapySession;
import com.hillrom.vest.domain.UserExtension;
import com.hillrom.vest.domain.UserPatientAssoc;
import com.hillrom.vest.domain.UserPatientAssocPK;
import com.hillrom.vest.domain.VestDeviceBadData;
import com.hillrom.vest.repository.AuthorityRepository;
import com.hillrom.vest.repository.PatientInfoRepository;
import com.hillrom.vest.repository.PatientVestDeviceDataRepository;
import com.hillrom.vest.repository.PatientVestDeviceRawLogRepository;
import com.hillrom.vest.repository.TempPatientVestDeviceDataRepository;
import com.hillrom.vest.repository.TempRepository;
import com.hillrom.vest.repository.UserExtensionRepository;
import com.hillrom.vest.repository.UserPatientRepository;
import com.hillrom.vest.repository.VestDeviceBadDataRepository;
import com.hillrom.vest.security.AuthoritiesConstants;
import com.hillrom.vest.service.util.PatientVestDeviceTherapyUtil;
import com.hillrom.vest.util.RelationshipLabelConstants;
import com.hillrom.vest.web.rest.dto.PatientVestDeviceDataVO;

@Service
public class PatientVestDeviceDataService {

	@Inject
	private DeviceLogParser deviceLogParser;

	@Inject
	private PatientVestDeviceDataRepository deviceDataRepository;

	@Inject
	private TempPatientVestDeviceDataRepository tempPatientdeviceDataRepository;

	@Inject
	private PatientVestDeviceRawLogRepository deviceRawLogRepository;

	@Inject
	private PatientInfoRepository patientInfoRepository;

	@Inject
	private UserExtensionRepository userExtensionRepository;

	@Inject
	private UserPatientRepository userPatientRepository;

	@Inject
	private TherapySessionService therapySessionService;

	@Inject
	private AuthorityRepository authorityRepository;

	@Inject
	private VestDeviceBadDataRepository vestDeviceBadDataRepository;

	@Inject
	private TempRepository patientDeviceDataTempRepository;

	@Inject
	private PatientNoEventService noEventService;

	@Inject
	private JobLauncher jobLauncher;
	
	@Inject
	private ApplicationContext applicationContext;
	
	public List<PatientVestDeviceData> save(final String rawData) {
		PatientVestDeviceRawLog deviceRawLog = null;
		List<PatientVestDeviceData> patientVestDeviceRecords = null;
		try {
			deviceRawLog = deviceLogParser.parseBase64StringToPatientVestDeviceRawLog(rawData);

			patientVestDeviceRecords = deviceLogParser
					.parseBase64StringToPatientVestDeviceLogEntry(deviceRawLog.getDeviceData());

			String deviceSerialNumber = deviceRawLog.getDeviceSerialNumber();

			UserPatientAssoc userPatientAssoc = createPatientUserIfNotExists(deviceRawLog, deviceSerialNumber);
			assignDefaultValuesToVestDeviceData(deviceRawLog, patientVestDeviceRecords, userPatientAssoc);

			deviceDataRepository.save(patientVestDeviceRecords);

			List<TherapySession> therapySessions = PatientVestDeviceTherapyUtil
					.prepareTherapySessionFromDeviceData(patientVestDeviceRecords);

			therapySessions = therapySessionService.saveOrUpdate(therapySessions);

		} catch (Exception e) {
			vestDeviceBadDataRepository.save(new VestDeviceBadData(rawData));
			throw new RuntimeException(e.getMessage());
		} finally {
			if (Objects.nonNull(deviceRawLog)) {
				deviceRawLogRepository.save(deviceRawLog);
			}
		}
		return patientVestDeviceRecords;
	}

	public List<PatientVestDeviceData> saveToTemp(final String rawData) throws Exception {
		PatientVestDeviceRawLog deviceRawLog = null;
		List<TempPatientVestDeviceData> tempPatientVestDeviceRecords = null;
			deviceRawLog = deviceLogParser.parseBase64StringToPatientVestDeviceRawLog(rawData);

			tempPatientVestDeviceRecords = deviceLogParser
					.parseBase64StringToPatientVestDeviceLogEntryForTemp(deviceRawLog.getDeviceData());

			String deviceSerialNumber = deviceRawLog.getDeviceSerialNumber();

			UserPatientAssoc userPatientAssoc = createPatientUserIfNotExists(deviceRawLog, deviceSerialNumber);
			assignDefaultValuesToVestDeviceDataTemp(deviceRawLog, tempPatientVestDeviceRecords, userPatientAssoc);

			Job addNewPodcastJob = applicationContext.getBean("processTherapySessionsAndCompliance", Job.class);
			JobParameters jobParameters = new JobParametersBuilder()
    		.addLong("TIME", System.currentTimeMillis())
    		.toJobParameters();
			jobLauncher.run(addNewPodcastJob, jobParameters);
			//tempPatientdeviceDataRepository.save(tempPatientVestDeviceRecords);

			/*List<PatientVestDeviceDataVO> deviceDataVOs = patientDeviceDataTempRepository.getPatientDeviceDataDelta();
			List<PatientVestDeviceData> patientVestDeviceRecords = convertToPatientVestDeviceData(deviceDataVOs,
					userPatientAssoc);

			deviceDataRepository.save(patientVestDeviceRecords);
*/
		return new LinkedList<>();
	}

	private UserPatientAssoc createPatientUserIfNotExists(PatientVestDeviceRawLog deviceRawLog,
			String deviceSerialNumber) {
		Optional<PatientInfo> patientFromDB = patientInfoRepository.findOneBySerialNumber(deviceSerialNumber);

		PatientInfo patientInfo = null;

		if (patientFromDB.isPresent()) {
			patientInfo = patientFromDB.get();
			List<UserPatientAssoc> associations = userPatientRepository.findOneByPatientId(patientInfo.getId());
			List<UserPatientAssoc> userPatientAssociations = associations.stream()
					.filter(assoc -> RelationshipLabelConstants.SELF.equalsIgnoreCase(assoc.getRelationshipLabel()))
					.collect(Collectors.toList());
			return userPatientAssociations.get(0);
		} else {
			patientInfo = new PatientInfo();
			// Assigns the next hillromId for the patient
			String hillromId = patientInfoRepository.id();
			patientInfo.setId(hillromId);
			patientInfo.setHillromId(hillromId);
			patientInfo.setBluetoothId(deviceRawLog.getDeviceAddress());
			patientInfo.setHubId(deviceRawLog.getHubId());
			patientInfo.setSerialNumber(deviceRawLog.getDeviceSerialNumber());
			String customerName = deviceRawLog.getCustomerName();
			setNameToPatient(patientInfo, customerName);
			patientInfo = patientInfoRepository.save(patientInfo);

			UserExtension userExtension = new UserExtension();
			userExtension.setHillromId(hillromId);
			userExtension.setActivated(true);
			userExtension.setDeleted(false);
			userExtension.setFirstName(patientInfo.getFirstName());
			userExtension.setLastName(patientInfo.getLastName());
			userExtension.setMiddleName(patientInfo.getMiddleName());
			userExtension.getAuthorities().add(authorityRepository.findOne(PATIENT));
			userExtensionRepository.save(userExtension);

			UserPatientAssoc userPatientAssoc = new UserPatientAssoc(new UserPatientAssocPK(patientInfo, userExtension),
					AuthoritiesConstants.PATIENT, RelationshipLabelConstants.SELF);

			userPatientRepository.save(userPatientAssoc);

			userExtension.getUserPatientAssoc().add(userPatientAssoc);
			patientInfo.getUserPatientAssoc().add(userPatientAssoc);

			userExtensionRepository.save(userExtension);
			patientInfoRepository.save(patientInfo);
			LocalDate createdOrTransmittedDate = userExtension.getCreatedDate().toLocalDate();
			noEventService.createIfNotExists(
					new PatientNoEvent(createdOrTransmittedDate, createdOrTransmittedDate, patientInfo, userExtension));
			return userPatientAssoc;
		}
	}

	private void setNameToPatient(PatientInfo patientInfo, String customerName) {
		String names[] = customerName.split(" ");
		if (names.length == 2) {
			assignNameToPatient(patientInfo, names[1], names[0], null);
		}
		if (names.length == 3) {
			assignNameToPatient(patientInfo, names[2], names[1], names[0]);
		}
		if (names.length == 1) {
			assignNameToPatient(patientInfo, names[0], null, null);
		}
	}

	private void assignNameToPatient(PatientInfo patientInfo, String firstName, String lastName, String middleName) {
		patientInfo.setFirstName(firstName);
		patientInfo.setLastName(lastName);
		patientInfo.setMiddleName(middleName);
	}

	private void assignDefaultValuesToVestDeviceData(PatientVestDeviceRawLog deviceRawLog,
			List<PatientVestDeviceData> patientVestDeviceRecords, UserPatientAssoc userPatientAssoc) {
		patientVestDeviceRecords.stream().forEach(deviceData -> {
			deviceData.setHubId(deviceRawLog.getHubId());
			deviceData.setSerialNumber(deviceRawLog.getDeviceSerialNumber());
			deviceData.setPatient(userPatientAssoc.getPatient());
			deviceData.setPatientUser(userPatientAssoc.getUser());
			deviceData.setBluetoothId(deviceRawLog.getDeviceAddress());
		});
	}

	private void assignDefaultValuesToVestDeviceDataTemp(PatientVestDeviceRawLog deviceRawLog,
			List<TempPatientVestDeviceData> patientVestDeviceRecords, UserPatientAssoc userPatientAssoc) {
		patientVestDeviceRecords.stream().forEach(deviceData -> {
			deviceData.setHubId(deviceRawLog.getHubId());
			deviceData.setSerialNumber(deviceRawLog.getDeviceSerialNumber());
			deviceData.setPatient(userPatientAssoc.getPatient());
			deviceData.setPatientUser(userPatientAssoc.getUser());
			deviceData.setBluetoothId(deviceRawLog.getDeviceAddress());
		});
	}

	private List<PatientVestDeviceData> convertToPatientVestDeviceData(List<PatientVestDeviceDataVO> deviceDataVOs,
			UserPatientAssoc userPatientAssoc) {

		List<PatientVestDeviceData> vestDeviceDatas = new ArrayList<>();
		PatientVestDeviceData patientVestDeviceData;

		for (PatientVestDeviceDataVO dataVO : deviceDataVOs) {
			patientVestDeviceData = new PatientVestDeviceData();

			patientVestDeviceData.setBluetoothId(dataVO.getBluetoothId());
			patientVestDeviceData.setChecksum(dataVO.getChecksum());
			patientVestDeviceData.setDuration(dataVO.getDuration());
			patientVestDeviceData.setEventId(dataVO.getEventId());
			patientVestDeviceData.setFrequency(dataVO.getFrequency());
			patientVestDeviceData.setHmr(dataVO.getHmr());
			patientVestDeviceData.setHubId(dataVO.getHubId());
			patientVestDeviceData.setPatient(userPatientAssoc.getPatient());
			patientVestDeviceData.setPatientUser(userPatientAssoc.getUser());
			patientVestDeviceData.setPressure(dataVO.getPressure());
			patientVestDeviceData.setSequenceNumber(dataVO.getSequenceNumber());
			patientVestDeviceData.setTimestamp(dataVO.getTimestamp());

			vestDeviceDatas.add(patientVestDeviceData);
		}
		return vestDeviceDatas;

	}

}
