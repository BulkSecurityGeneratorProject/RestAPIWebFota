package com.hillrom.monarch.repository;

import static com.hillrom.vest.security.AuthoritiesConstants.HCP;
import static com.hillrom.vest.security.AuthoritiesConstants.PATIENT;
import static com.hillrom.vest.util.RelationshipLabelConstants.SELF;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import com.hillrom.vest.domain.Clinic;
import com.hillrom.vest.repository.util.QueryConstants;
import com.hillrom.vest.web.rest.dto.ClinicStatsNotificationVO;

/**
 * Spring Data JPA repository for the Clinic entity. But Monarch device
 */
public interface ClinicMonarchRepository extends JpaRepository<Clinic,String> , QueryDslPredicateExecutor<Clinic> {
	

	/*@Query(nativeQuery=true,
			value=" select puserid,pfirstname,plastname,hcp_id,huserid,CONCAT(hlastName,' ', hfirstName) as hname,clinicId,clinicname,clinicadminid,pc.missed_therapy_count,pc.is_settings_deviated,pc.is_hmr_compliant,"
					+ " nhn, sdn,mtn,hemail,caname,a_cahmr,a_casdn,a_camtn,a_caemail,clinicadherencesetting from ( "
					+ " select user.id as puserid,user.first_name as pfirstName,user.last_name as plastName,upa_hcp.user_id as hcp_id"
					+ " from USER user join USER_AUTHORITY user_authority on user_authority.user_id = user.id and user_authority.authority_name = '"+PATIENT+"'"
					+ " join USER_PATIENT_ASSOC  upa on user.id= upa.user_id and upa.relation_label = '"+SELF+"' "
					+ " join PATIENT_INFO patInfo on upa.patient_id = patInfo.id "
					+ " join USER_PATIENT_ASSOC upa_hcp on patInfo.id = upa_hcp.patient_id "
					+ " where user.is_deleted = 0 and user.activated=1) as associated_patient, "
					+ " (select huser.id as huserid,huser.first_name as hfirstName,huser.last_name as hlastName,huser.email as hemail,clinic.id as clinicid,clinic.name as clinicname,eua.user_id AS clinicadminid, clinic.adherence_setting as clinicadherencesetting, "
					+ " (select CONCAT(ca.last_name,' ', ca.first_name)  from USER ca where id = eua.user_id) as caname, "
					+ " (select ca.email from USER ca where id = eua.user_id) as a_caemail, "
					+ " (select cahmr.non_hmr_notification  from USER cahmr where id = eua.user_id) as a_cahmr, "
					+ " (select casdn.setting_deviation_notification  from USER casdn where id = eua.user_id) as a_casdn, "
					+ " (select camtn.missed_therapy_notification  from USER camtn where id = eua.user_id) as a_camtn, "
					+ " huser.non_hmr_notification as nhn, huser.setting_deviation_notification as sdn,huser.missed_therapy_notification as mtn from USER huser "
					+ " join USER_AUTHORITY user_authorityh on user_authorityh.user_id = huser.id and user_authorityh.authority_name = '"+HCP+"' "
					+ " left outer join CLINIC_USER_ASSOC user_clinic on user_clinic.users_id = huser.id "
					+ " left outer join CLINIC clinic on user_clinic.clinics_id = clinic.id and user_clinic.users_id = huser.id "
					+ " LEFT OUTER JOIN ENTITY_USER_ASSOC eua ON eua.entity_id = clinic.id and eua.user_id = huser.id "
					+ " where clinic.is_deleted=0 and (huser.non_hmr_notification=1 or huser.setting_deviation_notification=1 or huser.missed_therapy_notification=1 )) "
					+ " as associated_hcp, PATIENT_COMPLIANCE_MONARCH pc "
					+ " where puserid = pc.user_id AND pc.date=SUBDATE(CURDATE(),1) and associated_patient.hcp_id = associated_hcp.huserid ")
	List<Object[]> findPatientStatisticsClinicForActiveClinics();*/
	
	@Query(nativeQuery=true,
			 value=QueryConstants.QUERY_PATIENT_STATISTICS_CAREGIVER_MONARCH)
	List<Object[]> findPatientStatisticsCareGiver();
	
	
	@Query(nativeQuery=true,
			value=	QueryConstants.QUERY_FOR_ACTIVE_CLINICS_MONARCH)
	List<Object[]> findPatientStatisticsClinicForActiveClinics();
	
	@Query(nativeQuery=true,
			 value= QueryConstants.QUERY_PATIENT_STATISTICS_CAREGIVER_DETAILS_MONARCH)
	List<Object[]> findPatientStatisticsCareGiverDetails();

}
