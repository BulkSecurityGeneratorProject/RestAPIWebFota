package com.hillrom.vest.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.boon.json.annotations.JsonProperty;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hillrom.vest.domain.util.ISO8601LocalDateDeserializer;
import com.hillrom.vest.domain.util.MMDDYYYYLocalDateSerializer;

/**
 * A user.
 */
@Entity
@Audited
@Table(name = "USER")
@Inheritance(strategy = InheritanceType.JOINED)
@SQLDelete(sql="UPDATE USER SET is_deleted = 1 WHERE id = ?")
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class,property= "id")
public class User extends AbstractAuditingEntity implements Serializable {

    
	private static final long serialVersionUID = 1L;

	@Id
    @Column(name = "id")
    @GeneratedValue
    private Long id;

    @JsonIgnore
    @Size(max = 60)
    @Column(length = 60)
    private String password;
    
    @Size(max = 50)
    @Column(name = "title", length = 50)
    private String title;

    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;
    
    @Size(max = 50)
    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    @Size(max = 100)
    @Column(length = 100, unique = true)
    private String email;
    
    @Size(max = 10)
    @Column(name = "gender", length = 10)
    private String gender;
    
    @Column(name = "zipcode")
    private String zipcode;

    @Column(nullable = false)
    private boolean activated = false;

    @Size(min = 2, max = 5)
    @Column(name = "lang_key", length = 5)
    private String langKey;

    @Size(max = 20)
    @Column(name = "activation_key", length = 20)
    @JsonIgnore
    private String activationKey;

    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    private String resetKey;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "reset_date", nullable = true)
    private DateTime resetDate = null;
    
    @Column(name = "terms_condition_accepted")
    private boolean termsConditionAccepted = false;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "terms_condition_accepted_date", nullable = true)
    private DateTime termsConditionAcceptedDate = null;

    @Audited
    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(
            name = "USER_AUTHORITY",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "name")})
    private Set<Authority> authorities = new HashSet<>();
    
    @Column(name="is_deleted")
    @JsonIgnore
    private boolean deleted = false;
    
    @Column(name="last_loggedin_at")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime lastLoggedInAt;
    
    @NotAudited
    @OneToMany(mappedBy = "userPatientAssocPK.user",fetch=FetchType.LAZY)
    @JsonIgnore
    private Set<UserPatientAssoc> userPatientAssoc = new HashSet<>();
    
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
    @JsonSerialize(using = MMDDYYYYLocalDateSerializer.class)
    @JsonDeserialize(using = ISO8601LocalDateDeserializer.class)
    @Column(name = "dob")
    private LocalDate dob;
    
    @Column(name="hillrom_id")
    private String hillromId;
    
    @Column(name="missed_therapy_notification")
    private boolean missedTherapyNotification = false;

	@Column(name="non_hmr_notification")
    private boolean nonHMRNotification = false;
    
    @Column(name="setting_deviation_notification")
    private boolean  settingDeviationNotification = false;
    
    @Column(name="message_notification")
    private boolean  messageNotification = false;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "activation_link_sent_date", nullable = true)
    private DateTime activationLinkSentDate = null;
    
    @Column(name = "expired")
    private Boolean expired = false;
    
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "expiration_date", nullable = true)
    private DateTime expirationDate = null;
    
    @Column(name="re_registered")
    private Boolean reRegister = false;
    
  //Garment changes
    @Transient
    @JsonSerialize
    @JsonDeserialize
	private String vestGarmentColor;
	
    @Transient
    @JsonSerialize
    @JsonDeserialize
	private String vestGarmentSize;
	
    @Transient
    @JsonSerialize
    @JsonDeserialize
	private String vestGarmentType;
    
  //Garment changes
    @Transient
    @JsonSerialize
    @JsonDeserialize
	private String monarchGarmentColor;
	
    @Transient
    @JsonSerialize
    @JsonDeserialize
	private String monarchGarmentSize;
	
    @Transient
    @JsonSerialize
    @JsonDeserialize
	private String monarchGarmentType;
    
    @Transient
    @JsonSerialize
    @JsonDeserialize
    private String deactivationReason;
    
    @Transient
    @JsonSerialize
    @JsonDeserialize
    private String userPreferenceTimezone;
    
	public User() {
		super();
	}

	public User(String firstName, String lastName, String email,
			boolean missedTherapyNotification, boolean nonHMRNotification,
			boolean settingDeviationNotification) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.missedTherapyNotification = missedTherapyNotification;
		this.nonHMRNotification = nonHMRNotification;
		this.settingDeviationNotification = settingDeviationNotification;
	}


	public String getDeactivationReason() {
		return deactivationReason;
	}

	public void setDeactivationReason(String deactivationReason) {
		this.deactivationReason = deactivationReason;
	}
	
	public String getVestGarmentColor() {
		return vestGarmentColor;
	}

	public void setVestGarmentColor(String vestGarmentColor) {
		this.vestGarmentColor = vestGarmentColor;
	}

	public String getVestGarmentSize() {
		return vestGarmentSize;
	}

	public void setVestGarmentSize(String vestGarmentSize) {
		this.vestGarmentSize = vestGarmentSize;
	}

	public String getVestGarmentType() {
		return vestGarmentType;
	}

	public void setVestGarmentType(String vestGarmentType) {
		this.vestGarmentType = vestGarmentType;
	}

	public String getMonarchGarmentColor() {
		return monarchGarmentColor;
	}

	public void setMonarchGarmentColor(String monarchGarmentColor) {
		this.monarchGarmentColor = monarchGarmentColor;
	}

	public String getMonarchGarmentSize() {
		return monarchGarmentSize;
	}

	public void setMonarchGarmentSize(String monarchGarmentSize) {
		this.monarchGarmentSize = monarchGarmentSize;
	}

	public String getMonarchGarmentType() {
		return monarchGarmentType;
	}

	public void setMonarchGarmentType(String monarchGarmentType) {
		this.monarchGarmentType = monarchGarmentType;
	}

	public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean getActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public String getResetKey() {
        return resetKey;
    }

    public void setResetKey(String resetKey) {
        this.resetKey = resetKey;
    }

    public DateTime getResetDate() {
       return resetDate;
    }

    public DateTime getLastLoggedInAt() {
		return lastLoggedInAt;
	}

	public void setLastLoggedInAt(DateTime lastLoggedInAt) {
		this.lastLoggedInAt = lastLoggedInAt;
	}

	public void setResetDate(DateTime resetDate) {
       this.resetDate = resetDate;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = (langKey != null) ? langKey : "en";
    }

    public Set<Authority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities;
    }

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getZipcode() {
		return zipcode;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}

	public boolean getTermsConditionAccepted() {
		return termsConditionAccepted;
	}

	public void setTermsConditionAccepted(boolean termsConditionAccepted) {
		this.termsConditionAccepted = termsConditionAccepted;
	}

	public DateTime getTermsConditionAcceptedDate() {
		return termsConditionAcceptedDate;
	}

	public void setTermsConditionAcceptedDate(DateTime termsConditionAcceptedDate) {
		this.termsConditionAcceptedDate = termsConditionAcceptedDate;
	}

	public Set<UserPatientAssoc> getUserPatientAssoc() {
		return userPatientAssoc;
	}

	public void setUserPatientAssoc(Set<UserPatientAssoc> userPatientAssoc) {
		this.userPatientAssoc = userPatientAssoc;
	}

	public LocalDate getDob() {
		return dob;
	}

	public void setDob(LocalDate dob) {
		this.dob = dob;
	}

	public String getHillromId() {
		return hillromId;
	}

	public void setHillromId(String hillromId) {
		this.hillromId = hillromId;
	}
	
	public boolean isMissedTherapyNotification() {
		return missedTherapyNotification;
	}

	public void setMissedTherapyNotification(boolean missedTherapyNotification) {
		this.missedTherapyNotification = missedTherapyNotification;
	}

	public boolean isNonHMRNotification() {
		return nonHMRNotification;
	}

	public void setNonHMRNotification(boolean nonHMRNotification) {
		this.nonHMRNotification = nonHMRNotification;
	}

	public boolean isSettingDeviationNotification() {
		return settingDeviationNotification;
	}

	public void setSettingDeviationNotification(boolean settingDeviationNotification) {
		this.settingDeviationNotification = settingDeviationNotification;
	}
	
	public boolean isMessageNotification() {
		return messageNotification;
	}

	public void setMessageNotification(boolean messageNotification) {
		this.messageNotification = messageNotification;
	}	

    public DateTime getActivationLinkSentDate() {
		return activationLinkSentDate;
	}

	public void setActivationLinkSentDate(DateTime activationLinkSentDate) {
		this.activationLinkSentDate = activationLinkSentDate;
	}

	public Boolean getExpired() {
		return expired;
	}

	public void setExpired(Boolean expired) {
		this.expired = expired;
	}

	public DateTime getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(DateTime expirationDate) {
		this.expirationDate = expirationDate;
	}

	public Boolean isReRegister() {
		return reRegister;
	}

	public void setReRegister(Boolean reRegister) {
		this.reRegister = reRegister;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        if (! Objects.equals(email, user.email)) {
            return false;
        }

        return true;
    }

	
    /**
	 * @return the userPreferenceTimezone
	 */
	public String getUserPreferenceTimezone() {
		return userPreferenceTimezone;
	}

	/**
	 * @param userPreferenceTimezone the userPreferenceTimezone to set
	 */
	public void setUserPreferenceTimezone(String userPreferenceTimezone) {
		this.userPreferenceTimezone = userPreferenceTimezone;
	}

	@Override
    public int hashCode() {
        if(email != null) return email.hashCode();
        else {
        	return HashCodeBuilder.reflectionHashCode(this);
        }
    }

	@Override
	public String toString() {
		return "User [id=" + id + ", password=" + password + ", title=" + title
				+ ", firstName=" + firstName + ", middleName=" + middleName
				+ ", lastName=" + lastName + ", email=" + email + ", gender="
				+ gender + ", zipcode=" + zipcode + ", activated=" + activated
				+ ", langKey=" + langKey + ", activationKey=" + activationKey
				+ ", resetKey=" + resetKey + ", resetDate=" + resetDate
				+ ", termsConditionAccepted=" + termsConditionAccepted
				+ ", termsConditionAcceptedDate=" + termsConditionAcceptedDate
				+ ", authorities=" + authorities + ", deleted=" + deleted
				+ ", lastLoggedInAt=" + lastLoggedInAt + ", userPatientAssoc="
				+ userPatientAssoc + ", dob=" + dob + ", hillromId="
				+ hillromId + ", missedTherapyNotification="
				+ missedTherapyNotification + ", nonHMRNotification="
				+ nonHMRNotification + ", settingDeviationNotification="
				+ settingDeviationNotification + ", messageNotification="
				+ messageNotification + ", activationLinkSentDate="
				+ activationLinkSentDate + ", expired=" + expired
				+ ", expirationDate=" + expirationDate + ", reRegister="
				+ reRegister + ", vestGarmentColor=" + vestGarmentColor
				+ ", vestGarmentSize=" + vestGarmentSize + ", vestGarmentType="
				+ vestGarmentType + ", monarchGarmentColor="
				+ monarchGarmentColor + ", monarchGarmentSize="
				+ monarchGarmentSize + ", monarchGarmentType="
				+ monarchGarmentType + ", deactivationReason="
				+ deactivationReason + ", userPreferenceTimezone="
				+ userPreferenceTimezone + "]";
	}

}
