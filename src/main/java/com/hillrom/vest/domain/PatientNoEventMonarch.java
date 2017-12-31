package com.hillrom.vest.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hillrom.vest.domain.util.CustomLocalDateSerializer;
import com.hillrom.vest.domain.util.ISO8601LocalDateDeserializer;

@Entity
@Table(name="PATIENT_NO_EVENT_MONARCH")
public class PatientNoEventMonarch {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(name="first_transmission_date")
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
	@JsonSerialize(using = CustomLocalDateSerializer.class)
    @JsonDeserialize(using = ISO8601LocalDateDeserializer.class)
	private LocalDate firstTransmissionDate;
	
	@OneToOne(optional=false,targetEntity=PatientInfo.class,fetch=FetchType.LAZY)
	@JoinColumn(name="patient_id",referencedColumnName="id")
	private PatientInfo patient;
	
	@OneToOne(optional=false,targetEntity=User.class,fetch=FetchType.LAZY)
	@JoinColumn(name="user_id",referencedColumnName="id")
	private User patientUser;
	
	@Column(name="user_created_date")
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
	@JsonSerialize(using = CustomLocalDateSerializer.class)
    @JsonDeserialize(using = ISO8601LocalDateDeserializer.class)
	private LocalDate userCreatedDate;
	
	@Column(name="first_transmission_date_before_update")
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
	@JsonSerialize(using = CustomLocalDateSerializer.class)
    @JsonDeserialize(using = ISO8601LocalDateDeserializer.class)
	private LocalDate firstTransmissionDateBeforeUpdate;
	
	@Column(name="date_first_transmission_date_updated")
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
	@JsonSerialize(using = CustomLocalDateSerializer.class)
    @JsonDeserialize(using = ISO8601LocalDateDeserializer.class)
	private LocalDate dateFirstTransmissionDateUpdated;
	
	@Column(name="first_trans_date_type")
	private String firstTransDateType;
	
	
	public PatientNoEventMonarch() {
		super();
	}

	public PatientNoEventMonarch(LocalDate userCreatedDate,LocalDate firstTransmissionDate, PatientInfo patient,
			User patientUser) {
		super();
		this.userCreatedDate = userCreatedDate;
		this.firstTransmissionDate = firstTransmissionDate;
		this.patient = patient;
		this.patientUser = patientUser;
	}

	
	
	public String getFirstTransDateType() {
		return firstTransDateType;
	}

	public void setFirstTransDateType(String firstTransDateType) {
		this.firstTransDateType = firstTransDateType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getFirstTransmissionDate() {
		return firstTransmissionDate;
	}

	public void setFirstTransmissionDate(LocalDate firstTransmissionDate) {
		this.firstTransmissionDate = firstTransmissionDate;
	}

	public PatientInfo getPatient() {
		return patient;
	}

	public void setPatient(PatientInfo patient) {
		this.patient = patient;
	}

	public User getPatientUser() {
		return patientUser;
	}

	public void setPatientUser(User patientUser) {
		this.patientUser = patientUser;
	}

	public LocalDate getUserCreatedDate() {
		return userCreatedDate;
	}

	public void setUserCreatedDate(LocalDate userCreatedDate) {
		this.userCreatedDate = userCreatedDate;
	}

	@JsonIgnore
	public int getDayOfTheWeek(){
		return this.userCreatedDate.getDayOfWeek();
	}
	
	@JsonIgnore
	public int getWeekOfYear(){
		return this.userCreatedDate.getWeekOfWeekyear();
	}
	
	@JsonIgnore
	public int getMonthOfTheYear(){
		return this.userCreatedDate.getMonthOfYear();
	}
	
	
	public LocalDate getFirstTransmissionDateBeforeUpdate() {
		return firstTransmissionDateBeforeUpdate;
	}

	public void setFirstTransmissionDateBeforeUpdate(
			LocalDate firstTransmissionDateBeforeUpdate) {
		this.firstTransmissionDateBeforeUpdate = firstTransmissionDateBeforeUpdate;
	}

	public LocalDate getDateFirstTransmissionDateUpdated() {
		return dateFirstTransmissionDateUpdated;
	}

	public void setDateFirstTransmissionDateUpdated(
			LocalDate dateFirstTransmissionDateUpdated) {
		this.dateFirstTransmissionDateUpdated = dateFirstTransmissionDateUpdated;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PatientNoEventMonarch other = (PatientNoEventMonarch) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PatientNoEventMonarch [id=" + id + ", firstTransmissionDate=" + firstTransmissionDate + ", patient="
				+ patient + ", patientUser=" + patientUser + ", userCreatedDate=" + userCreatedDate + "]";
	}

}
