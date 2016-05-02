package com.hillrom.vest.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hillrom.vest.domain.util.CustomLocalDateSerializer;
import com.hillrom.vest.domain.util.ISO8601LocalDateDeserializer;


/**
 * A PatientInfo.
 */
@Entity
@Audited
@Table(name = "PATIENT_INFO")
public class PatientInfo implements Serializable {

    @Id
    private String id;

    @Column(name = "hillrom_id")
    private String hillromId;

    @Column(name = "hub_id")
    private String hubId;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "bluetooth_id")
    private String bluetoothId;

    @Column(name = "title")
    private String title;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @JsonDeserialize(using = ISO8601LocalDateDeserializer.class)
    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "email")
    private String email;
    
    @Column(name = "gender")
    private String gender;
    
    @Column(name = "lang_key")
    private String langKey;
    
    @Column(name = "address")
    private String address;
    
    @Column(name="zipcode")
    private Integer zipcode;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "expired")
    private Boolean expired = false;
    
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "expired_date", nullable = true)
    private DateTime expiredDate = null;
    
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "training_date", nullable = true)
    private DateTime trainingDate = null;
    
    @Column(name = "web_login_created")
    private Boolean webLoginCreated;

    @NotAudited
    @OneToMany(mappedBy = "clinicPatientAssocPK.patient",fetch=FetchType.LAZY)
    @JsonIgnore
    private Set<ClinicPatientAssoc> clinicPatientAssoc = new HashSet<>();
    
    @NotAudited
    @OneToMany(mappedBy = "userPatientAssocPK.patient",fetch=FetchType.LAZY)
    @JsonIgnore
    private Set<UserPatientAssoc> userPatientAssoc = new HashSet<>();
    
	@Column(name = "primary_phone")
	private String primaryPhone;

	@Column(name = "mobile_phone")
	private String mobilePhone;
	
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	@Column(name = "device_assoc_date")
    private DateTime deviceAssocDate;

    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHillromId() {
        return hillromId;
    }

    public void setHillromId(String hillromId) {
        this.hillromId = hillromId;
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getBluetoothId() {
        return bluetoothId;
    }

    public void setBluetoothId(String bluetoothId) {
        this.bluetoothId = bluetoothId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getWebLoginCreated() {
        return webLoginCreated;
    }

    public void setWebLoginCreated(Boolean webLoginCreated) {
        this.webLoginCreated = webLoginCreated;
    }

    public Integer getZipcode() {
		return zipcode;
	}

	public void setZipcode(Integer zipcode) {
		this.zipcode = zipcode;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getLangKey() {
		return langKey;
	}

	public void setLangKey(String langKey) {
		this.langKey = langKey;
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

	public Boolean getExpired() {
		return expired;
	}

	public void setExpired(Boolean expired) {
		this.expired = expired;
	}

	public DateTime getExpiredDate() {
		return expiredDate;
	}

	public void setExpiredDate(DateTime expiredDate) {
		this.expiredDate = expiredDate;
	}
	
	public DateTime getTrainingDate() {
		return trainingDate;
	}

	public void setTrainingDate(DateTime trainingDate) {
		this.trainingDate = trainingDate;
	}

	public Set<ClinicPatientAssoc> getClinicPatientAssoc() {
		return clinicPatientAssoc;
	}

	public void setClinicPatientAssoc(Set<ClinicPatientAssoc> clinicPatientAssoc) {
		this.clinicPatientAssoc = clinicPatientAssoc;
	}

	public Set<UserPatientAssoc> getUserPatientAssoc() {
		return userPatientAssoc;
	}

	public void setUserPatientAssoc(Set<UserPatientAssoc> userPatientAssoc) {
		this.userPatientAssoc = userPatientAssoc;
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

	public DateTime getDeviceAssocDate() {
		return deviceAssocDate;
	}

	public void setDeviceAssocDate(DateTime deviceAssocDate) {
		this.deviceAssocDate = deviceAssocDate;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PatientInfo patientInfo = (PatientInfo) o;

        if ( ! Objects.equals(id, patientInfo.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
	@Override
    public String toString() {
        return "PatientInfo{" +
                "id=" + id +
                ", hillromId='" + hillromId + "'" +
                ", hubId='" + hubId + "'" +
                ", serialNumber='" + serialNumber + "'" +
                ", bluetoothId='" + bluetoothId + "'" +
                ", title='" + title + "'" +
                ", firstName='" + firstName + "'" +
                ", middleName='" + middleName + "'" +
                ", lastName='" + lastName + "'" +
                ", dob='" + dob + "'" +
                ", email='" + email + "'" +
                ", webLoginCreated='" + webLoginCreated + "'" +
                '}';
    }
}
