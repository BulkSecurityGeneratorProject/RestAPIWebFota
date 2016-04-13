package com.hillrom.vest.service;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.hillrom.vest.domain.PatientTestResult;
import com.hillrom.vest.domain.User;
import com.hillrom.vest.domain.UserPatientAssoc;
import com.hillrom.vest.exceptionhandler.HillromException;
import com.hillrom.vest.repository.PatientTestResultRepository;
import com.hillrom.vest.repository.UserPatientRepository;
import com.hillrom.vest.repository.UserRepository;
import com.hillrom.vest.security.AuthoritiesConstants;
import com.hillrom.vest.util.ExceptionConstants;
import com.hillrom.vest.util.RelationshipLabelConstants;

@Service
@Transactional
public class PateintTestResultService {

	@Inject
	private PatientTestResultRepository patientTestResultRepository;
	
	@Inject
	private UserRepository userRepository;
	
	@Inject
	private UserPatientRepository userPatientRepository;

	public List<PatientTestResult> getPatientTestResult() {

		return patientTestResultRepository.findAll();
	}

	public PatientTestResult getPatientTestResultById(Long id) {
		return patientTestResultRepository.getOne(id);
	}

	public List<PatientTestResult> getPatientTestResultByUserId(Long id) {
		return patientTestResultRepository.findByUserId(id);
	}

	public PatientTestResult createPatientTestResult(PatientTestResult patientTestResult, Long userId, String baseURL)
			throws HillromException {
		
		User user = userRepository.getOne(userId);

		if (Objects.isNull(user))
			throw new HillromException(ExceptionConstants.HR_512);
		
		patientTestResult.setUser(user);
		List<UserPatientAssoc> userPatientAssocs = null;
		if (Objects.isNull(patientTestResult.getPatientInfo())) {
			userPatientAssocs = userPatientRepository.findByUserIdAndUserRole(userId, AuthoritiesConstants.PATIENT);
			for (UserPatientAssoc userPatientAssoc : userPatientAssocs)
				if (RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())) {
					patientTestResult.setPatientInfo(userPatientAssoc.getPatient());
					break;
				}
		}
		return patientTestResultRepository.saveAndFlush(patientTestResult);
	}	
	
	public PatientTestResult updatePatientTestResult(PatientTestResult patientTestResult, Long userId, String baseURL)
			throws HillromException {
		if (!patientTestResultRepository.exists(patientTestResult.getId()))
			throw new HillromException(ExceptionConstants.HR_719);
		User user = userRepository.getOne(userId);

		if (Objects.isNull(user))
			throw new HillromException(ExceptionConstants.HR_512);
		
		patientTestResult.setUser(user);
		List<UserPatientAssoc> userPatientAssocs = null;
		if (Objects.isNull(patientTestResult.getPatientInfo())) {
			userPatientAssocs = userPatientRepository.findByUserIdAndUserRole(userId, AuthoritiesConstants.PATIENT);
			for (UserPatientAssoc userPatientAssoc : userPatientAssocs)
				if (RelationshipLabelConstants.SELF.equals(userPatientAssoc.getRelationshipLabel())) {
					patientTestResult.setPatientInfo(userPatientAssoc.getPatient());
					break;
				}
		}
		return patientTestResultRepository.saveAndFlush(patientTestResult);
	}	
}
