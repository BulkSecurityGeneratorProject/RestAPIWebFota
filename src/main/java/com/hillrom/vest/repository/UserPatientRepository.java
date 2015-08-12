package com.hillrom.vest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hillrom.vest.domain.UserPatientAssoc;
import com.hillrom.vest.domain.UserPatientAssocPK;

public interface UserPatientRepository extends
		JpaRepository<UserPatientAssoc, UserPatientAssocPK> {

	@Query("from UserPatientAssoc upa where upa.userPatientAssocPK.user.id = ?1")
	List<Optional<UserPatientAssoc>> findOneByUserId(Long userId);
	
	@Query("from UserPatientAssoc upa where upa.userPatientAssocPK.patient.id = ?1")
	List<Optional<UserPatientAssoc>> findOneByPatientId(String patientId);
	
	@Query("from UserPatientAssoc upa where upa.userPatientAssocPK.user.id = ?1 and upa.userPatientAssocPK.patient.id = ?2")
	Optional<UserPatientAssoc> findOneByUserIdAndPatientId(Long userId,String patientId);
}
