package com.hillrom.vest.repository;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hillrom.vest.domain.PatientCompliance;
import com.hillrom.vest.domain.PatientDevicesAssoc;


/**
 * Spring Data JPA repository for the Authority entity.
 */
public interface PatientDevicesAssocRepository extends JpaRepository<PatientDevicesAssoc, Long> {
	
	@Query("from PatientDevicesAssoc PDA where PDA.serialNumber = ?1 and isActive=1")
	Optional<PatientDevicesAssoc> findOneBySerialNumber(String deviceAddress);

	@Query(nativeQuery=true,value=" SELECT * from PATIENT_DEVICES_ASSOC where is_active=1 and date(modified_date)=:modifiedDate ")
	List<PatientDevicesAssoc> findByModifiedDate(@Param("modifiedDate")String modifiedDate);
	
	@Query("from PatientDevicesAssoc where patientId = ?1 and isActive=1")	 
	List<PatientDevicesAssoc> findByPatientId(String patientId);
	
	@Query("from PatientDevicesAssoc where oldPatientId = ?1 and isActive=1 ")	 
	List<PatientDevicesAssoc> findByOldPatientId(String oldPatientId);
	

	@Query("from PatientDevicesAssoc PDA where PDA.patientId = ?1 and deviceType=?2 and isActive=1")
	PatientDevicesAssoc findOneByPatientIdAndDeviceType(String patientId, String deviceType);
	
 /*
	@Query("from PatientDevicesAssoc PDA where PDA.hillromId = ?1 and isActive=1")
	Optional<PatientDevicesAssoc> findByHillromId(String hillromId);
	*/

	@Query(nativeQuery=true,value=" SELECT * from PATIENT_DEVICES_ASSOC where hillrom_id=:hillromId and is_active=1 LIMIT 1")
	Optional<PatientDevicesAssoc> findByHillromId(@Param("hillromId")String hillromId);
	
	@Query("from PatientDevicesAssoc PDA where PDA.hillromId = ?1 and deviceType=?2 and isActive=1")
	Optional<PatientDevicesAssoc> findByHillromIdAndDeviceType(String hillromId, String deviceType);
	
	@Query("from PatientDevicesAssoc PDA where PDA.hillromId = ?1 and deviceType=?2 and isActive=0")
	Optional<PatientDevicesAssoc> findByHillromIdAndDeviceTypeInactivated(String hillromId, String deviceType);
	
	@Query("from PatientDevicesAssoc PDA where PDA.serialNumber = ?1 and deviceType=?2 and isActive=1")
	Optional<PatientDevicesAssoc> findOneBySerialNumberAndDeviceType(String deviceAddress, String deviceType);
	
	@Query("from PatientDevicesAssoc PDA where PDA.serialNumber = ?1 and deviceType=?2 and isActive=0")
	Optional<PatientDevicesAssoc> findOneBySerialNumberAndDeviceTypeInactive(String deviceAddress, String deviceType);
	
	@Query("from PatientDevicesAssoc PDA where PDA.serialNumber = ?1 and PDA.hillromId = ?2 and deviceType=?3 and isActive=1")
	Optional<PatientDevicesAssoc> findOneBySerialNumberAndHillromIdAndDeviceType(String deviceAddress, String hillromId, String deviceType);
	
	@Query(nativeQuery=true,value=" SELECT * from PATIENT_DEVICES_ASSOC where is_active=1 and date(swapped_date)=:swappedDate ")
	List<PatientDevicesAssoc> findBySwappedDate(@Param("swappedDate")String swappedDate);

	@Query("from PatientDevicesAssoc PDA where deviceType=?1 and patientType=?2 and isActive=1")
	List<PatientDevicesAssoc> findAllByDeviceTypeAndPatientType(String deviceType,String patientType);
}
