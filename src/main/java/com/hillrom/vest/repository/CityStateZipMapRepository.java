package com.hillrom.vest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hillrom.vest.domain.CityStateZipMap;
import java.lang.String;

public interface CityStateZipMapRepository extends JpaRepository<CityStateZipMap, Long> {

	@Query("Select distinct(state) from CityStateZipMap")
	List<String> findUniqueStates();
	
	@Query("Select distinct(state) from PatientInfo pi Where pi.state is NOT NULL")
	List<String> findAvailableStates();
	
	@Query("Select distinct(city) from PatientInfo pi where pi.state = ?1 and pi.city IS NOT NULL")
	List<String> findAvailableCities(String state);
	
	
	
	List<CityStateZipMap> findByZipCode(String zipCode);

	List<CityStateZipMap> findByState(String state);
	
	@Query("Select distinct(state) from CityStateZipMap where country=?1")
	List<CityStateZipMap> findByCountry(String country);
	
	@Query("Select distinct(city) from CityStateZipMap csz where csz.state = ?1")
	List<String> findUniqueCitiesByState(String state);
}