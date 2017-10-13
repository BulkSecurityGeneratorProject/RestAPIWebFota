package com.hillrom.monarch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hillrom.vest.domain.PatientVestDeviceRawLog;
import com.hillrom.vest.domain.PatientVestDeviceRawLogMonarch;

/**
 * Spring Data JPA repository for the PatientVestDeviceRawLog entity.
 */
public interface PatientMonarchDeviceRawLogRepository extends JpaRepository<PatientVestDeviceRawLogMonarch,Long> {

}
