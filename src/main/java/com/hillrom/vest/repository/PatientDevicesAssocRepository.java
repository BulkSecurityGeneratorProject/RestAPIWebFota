package com.hillrom.vest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hillrom.vest.domain.PatientDevicesAssoc;

/**
 * Spring Data JPA repository for the Authority entity.
 */
public interface PatientDevicesAssocRepository extends JpaRepository<PatientDevicesAssoc, Long> {
}
