package com.hillrom.vest.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A UserExtension.
 */
@Entity
@Audited
@Table(name = "USER_EXTENSION")
@PrimaryKeyJoinColumn(name = "USER_ID", referencedColumnName = "id")
@SQLDelete(sql = "UPDATE USER_EXTENSION SET is_deleted = 1 WHERE USER_ID = ?")
public class UserExtension extends User implements Serializable {

	@Column(name = "speciality")
	private String speciality;

	@Column(name = "credentials")
	private String credentials;

	@Column(name = "primary_phone")
	private String primaryPhone;

	@Column(name = "mobile_phone")
	private String mobilePhone;

	@Column(name = "fax_number")
	private String faxNumber;

	@Column(name = "address")
	private String address;

	@Column(name = "city")
	private String city;

	@Column(name = "state")
	private String state;

	@Column(name = "npi_number")
	private String npiNumber;

	//@ManyToMany
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "CLINIC_USER_ASSOC", joinColumns = { @JoinColumn(name = "users_id", referencedColumnName = "user_id") }, inverseJoinColumns = { @JoinColumn(name = "clinics_id", referencedColumnName = "id") })
	@AuditJoinTable
	private Set<Clinic> clinics = new HashSet<>();

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted = false;
	
	//adding column for user deactivation reason
	@Size(max=255)
	@Column(name="deactivation_reason", length=255)
	private String deactivationReason;
	
	@Size(max=255)
	@Column(name="time_zone", length=255)
	private String timeZone;

	public String getDeactivationReason() {
		return deactivationReason;
	}

	public void setDeactivationReason(String deactivationReason) {
		this.deactivationReason = deactivationReason;
	}

	public String getSpeciality() {
		return speciality;
	}

	public void setSpeciality(String speciality) {
		this.speciality = speciality;
	}

	public String getCredentials() {
		return credentials;
	}

	public void setCredentials(String credentials) {
		this.credentials = credentials;
	}

	public String getPrimaryPhone() {
		return primaryPhone;
	}

	public void setPrimaryPhone(String primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	public String getMobilePhone() {
		return mobilePhone;
	}

	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}

	public String getFaxNumber() {
		return faxNumber;
	}

	public void setFaxNumber(String faxNumber) {
		this.faxNumber = faxNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getNpiNumber() {
		return npiNumber;
	}

	public void setNpiNumber(String npiNumber) {
		this.npiNumber = npiNumber;
	}

	public Set<Clinic> getClinics() {
		return clinics;
	}

	public void setClinics(Set<Clinic> clinics) {
		this.clinics = clinics;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	@Override
	public String toString() {
		return "UserExtension [speciality=" + speciality + ", credentials=" + credentials + ", primaryPhone="
				+ primaryPhone + ", mobilePhone=" + mobilePhone + ", faxNumber=" + faxNumber + ", address=" + address
				+ ", city=" + city + ", state=" + state + ", npiNumber=" + npiNumber + ", clinics=" + clinics
				+ ", deleted=" + deleted + ", deactivationReason=" + deactivationReason + ", timeZone=" + timeZone + "]";
	}

}
