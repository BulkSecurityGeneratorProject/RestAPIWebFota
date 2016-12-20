package com.hillrom.vest.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.hillrom.vest.domain.Announcements;

/**
 * Spring Data JPA repository for the User entity.
 */
public interface AnnouncementsRepository extends JpaRepository<Announcements, Long> {
	
	 	@Query(" from Announcements announcement where id = ? and isDeleted = ? ")
	    Announcements findOneById(Long id, boolean isDeleted);
	    
	    @Query(" from Announcements announcement where isDeleted = ? ")
	    Page<Announcements> findAnnouncements(boolean isDeleted,Pageable pageable);
	    

	    @Query(" from Announcements announcement, PatientInfo patientInfo where (announcement.sendTo = 'All' or announcement.sendTo = 'Patient') "
		      + " and (announcement.patientType = 'All' or announcement.patientType = patientInfo.primaryDiagnosis or (TIMESTAMPDIFF((YEAR, pi.dob, current_date()) > 18, 'Adult', 'Peds') = announcement.patientType )) "
		      + " and patientInfo.id = ?1  and announcement.isDeleted = ?2 ")
	    List<Announcements> test(String patientId, boolean isDeleted); 


	 
	
}
