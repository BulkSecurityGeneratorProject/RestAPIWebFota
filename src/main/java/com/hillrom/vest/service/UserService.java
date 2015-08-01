package com.hillrom.vest.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import net.minidev.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hillrom.vest.config.Constants;
import com.hillrom.vest.domain.Authority;
import com.hillrom.vest.domain.Clinic;
import com.hillrom.vest.domain.PatientInfo;
import com.hillrom.vest.domain.User;
import com.hillrom.vest.domain.UserExtension;
import com.hillrom.vest.domain.UserPatientAssoc;
import com.hillrom.vest.domain.UserSecurityQuestion;
import com.hillrom.vest.repository.AuthorityRepository;
import com.hillrom.vest.repository.ClinicRepository;
import com.hillrom.vest.repository.PatientInfoRepository;
import com.hillrom.vest.repository.UserExtensionRepository;
import com.hillrom.vest.repository.UserPatientRepository;
import com.hillrom.vest.repository.UserRepository;
import com.hillrom.vest.security.AuthoritiesConstants;
import com.hillrom.vest.security.OnCredentialsChangeEvent;
import com.hillrom.vest.security.SecurityUtils;
import com.hillrom.vest.service.util.RandomUtil;
import com.hillrom.vest.service.util.RequestUtil;
import com.hillrom.vest.web.rest.dto.PatientUserVO;
import com.hillrom.vest.web.rest.dto.UserDTO;
import com.hillrom.vest.web.rest.dto.UserExtensionDTO;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private UserRepository userRepository;
    
    @Inject
    private UserExtensionRepository userExtensionRepository;

    @Inject
    private AuthorityRepository authorityRepository;
    
    @Inject
    private UserPatientRepository userPatientRepository;
    
    @Inject
    private PatientInfoRepository patientInfoRepository;
    
    @Inject
    private PatientInfoService patientInfoService;
    
    @Inject
    private MailService mailService;

    @Inject
    private UserSecurityQuestionService userSecurityQuestionService;
    
    @Inject
	private ClinicRepository clinicRepository;
    
    @Inject
    private ApplicationEventPublisher eventPublisher;
    
    public String generateDefaultPassword(User patientUser) {
		StringBuilder defaultPassword = new StringBuilder();
		defaultPassword.append(patientUser.getZipcode());
		// default password will have the first 4 letters from last name, if length of last name <= 4, use complete string
		int endIndex = patientUser.getLastName().length() > Constants.NO_OF_CHARACTERS_TO_BE_EXTRACTED ? Constants.NO_OF_CHARACTERS_TO_BE_EXTRACTED : patientUser.getLastName().length() ; 
		defaultPassword.append(patientUser.getLastName().substring(0, endIndex));
		defaultPassword.append(patientUser.getDob().toString(Constants.DATEFORMAT_MMddyyyy));
		return defaultPassword.toString();
	}

    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        userRepository.findOneByActivationKey(key)
            .map(user -> {
                // activate given user for the registration key.
                user.setActivated(true);
                userRepository.save(user);
                log.debug("Activated user: {}", user);
                return user;
            });
        return Optional.empty();
    }

    /**
     * Completes the reset password flow
     * @param paramsMap
     * @return
     */
    public JSONObject completePasswordReset(Map<String,String> paramsMap) {
       log.debug("Reset user password for reset key {}", paramsMap);
   
       String requiredParams[] = {"password","questionId","answer"};
       JSONObject errorJSON =  RequestUtil.checkRequiredParams(paramsMap,requiredParams);
       if(null != errorJSON.get("ERROR"))
    	   return errorJSON;
       
       String key = paramsMap.get("key");
       String newPassword = paramsMap.get("password");
       String questionId = paramsMap.get("questionId");
       String answer = paramsMap.get("answer");
       
       JSONObject jsonObject = new JSONObject();
       if (!checkPasswordLength(newPassword)) {
    	   jsonObject.put("message", "Incorrect password");
    	   return jsonObject;
       }
       
       Optional<User> opUser = userRepository.findOneByResetKey(key);
       if(opUser.isPresent()){
    	   User user = opUser.get();
    	   errorJSON = canProceedPasswordReset(questionId, answer,user);
    	   if(null != errorJSON.get("ERROR")){
    		   return errorJSON;
    	   }
           user.setPassword(passwordEncoder.encode(newPassword));
           user.setResetKey(null);
           user.setResetDate(null);
           userRepository.save(user);
   		   eventPublisher.publishEvent(new OnCredentialsChangeEvent(user.getId()));
           jsonObject.put("email", user.getEmail());
           return jsonObject;
       }else{
    	   jsonObject.put("ERROR", "Invalid Reset Key");
    	   return jsonObject;   
       }
       
    }

    /**
     * Verifies whether Token expired or the security question answer matches 
     * @param questionId
     * @param answer
     * @param user
     * @return
     */
	private JSONObject canProceedPasswordReset(String questionId, String answer,
			 User user) {
		JSONObject jsonObject = new JSONObject();
		DateTime oneDayAgo = DateTime.now().minusHours(24);
           if(user.getResetDate().isBefore(oneDayAgo.toInstant().getMillis())){
        	   jsonObject.put("ERROR", "Reset Key Expired");
           }
           if(!verifySecurityQuestion(user,questionId,answer)){
        	   jsonObject.put("ERROR", "Incorrect Security Question or Password");
           }
           return jsonObject;
	}

    private boolean verifySecurityQuestion(User user,String questionId,String answer){
    	Optional<UserSecurityQuestion> opUserSecurityQuestion =  userSecurityQuestionService.findOneByUserIdAndQuestionId(user.getId(), Long.parseLong(questionId));
    	if(opUserSecurityQuestion.isPresent()){
    		return answer.equals(opUserSecurityQuestion.get().getAnswer());
    	}
    	return false;
    }
    
    private boolean checkPasswordLength(String password) {
        return (!StringUtils.isEmpty(password) && password.length() >= UserDTO.PASSWORD_MIN_LENGTH && password.length() <= UserDTO.PASSWORD_MAX_LENGTH);
    }
    
    public Optional<User> requestPasswordReset(String mail) {
       return userRepository.findOneByEmail(mail)
           .filter(user -> user.getActivated() == true)
           .map(user -> {
               user.setResetKey(RandomUtil.generateResetKey());
               user.setResetDate(DateTime.now());
               userRepository.save(user);
               return user;
           });
    }

    public User createUserInformation(String password, String firstName, String lastName, String email,
                                      String langKey) {

        User newUser = new User();
        Authority authority = authorityRepository.findOne(AuthoritiesConstants.ADMIN);
        Set<Authority> authorities = new HashSet<>();
        String encryptedPassword = passwordEncoder.encode(password);
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setLangKey(langKey);
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        authorities.add(authority);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public void updateUserInformation(String firstName, String lastName, String email, String langKey) {
        userRepository.findOneByEmail(SecurityUtils.getCurrentLogin()).ifPresent(u -> {
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setLangKey(langKey);
            userRepository.save(u);
    		eventPublisher.publishEvent(new OnCredentialsChangeEvent(u.getId()));
            log.debug("Changed Information for User: {}", u);
        });
    }

    public JSONObject changePassword(String password) {
    	JSONObject jsonObject = new JSONObject();
    	if(!checkPasswordLength(password)){
    		jsonObject.put("ERROR", "Incorrect password");
    	}else{
    		
    		userRepository.findOneByEmail(SecurityUtils.getCurrentLogin()).ifPresent(u-> {
    			String encryptedPassword = passwordEncoder.encode(password);
    			u.setPassword(encryptedPassword);
    			u.setLastLoggedInAt(DateTime.now());
    			userRepository.save(u);
    			eventPublisher.publishEvent(new OnCredentialsChangeEvent(u.getId()));
    			log.debug("Changed password for User: {}", u);
    		});    		
    	}
    	return jsonObject;
    }
    
    @Transactional(readOnly = true)
    public User getUserWithAuthorities() {
        User currentUser = userRepository.findOneByEmail(SecurityUtils.getCurrentLogin()).get();
        currentUser.getAuthorities().size(); // eagerly load the association
        return currentUser;
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p/>
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     * </p>
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        DateTime now = new DateTime();
        List<User> users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(now.minusDays(3));
        for (User user : users) {
            log.debug("Deleting not activated user {}", user.getEmail());
            userRepository.delete(user);
        }
    }
    
    private List<String> rolesAdminCanModerate() {
		List<String> rolesAdminCanModerate = new ArrayList<String>();
    	rolesAdminCanModerate.add(AuthoritiesConstants.ACCT_SERVICES);
    	rolesAdminCanModerate.add(AuthoritiesConstants.ASSOCIATES);
    	rolesAdminCanModerate.add(AuthoritiesConstants.ADMIN);
		return rolesAdminCanModerate;
	}
    
    public JSONObject createUser(UserExtensionDTO userExtensionDTO, String baseUrl){
    	JSONObject jsonObject = new JSONObject();
    	if(userExtensionDTO.getEmail() != null) {
			Optional<User> existingUser = userRepository.findOneByEmail(userExtensionDTO.getEmail());
			if (existingUser.isPresent()) {
				jsonObject.put("ERROR", "e-mail address already in use");
    			return jsonObject;
    		}
    	}
    	List<String> rolesAdminCanModerate = rolesAdminCanModerate();
    	if(rolesAdminCanModerate.contains(userExtensionDTO.getRole())
    			&& SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))) {
    		UserExtension user = createHillromTeamUser(userExtensionDTO);
    		if(user.getId() != null) {
    			if(userExtensionDTO.getEmail() != null) {
    				mailService.sendActivationEmail(user, baseUrl);
    			}
                jsonObject.put("message", "Hillrom User created successfully.");
                jsonObject.put("user", user);
                return jsonObject;
    		} else {
    			jsonObject.put("ERROR", "Unable to create Hillrom User.");
                return jsonObject;
    		}
    	} else if (AuthoritiesConstants.PATIENT.equals(userExtensionDTO.getRole())) {
        	return patientInfoRepository.findOneByHillromId(userExtensionDTO.getHillromId())
        			.map(user -> {
        				jsonObject.put("ERROR", "HR Id already in use.");
            			return jsonObject;
            		})
                    .orElseGet(() -> {
                    	UserExtension user = createPatientUser(userExtensionDTO);
                		if(user.getId() != null) {
	                        jsonObject.put("message", "Patient User created successfully.");
	                        jsonObject.put("user", user);
	                        return jsonObject;
                		} else {
                			jsonObject.put("ERROR", "Unable to create Patient.");
	                        return jsonObject;
                		}
                    });
        } else if (AuthoritiesConstants.HCP.equals(userExtensionDTO.getRole())) {
        	UserExtension user = createHCPUser(userExtensionDTO);
        	if(user.getId() != null) {
                mailService.sendActivationEmail(user, baseUrl);
                jsonObject.put("message", "HealthCare Professional created successfully.");
                jsonObject.put("user", user);
                jsonObject.put("clinics", user.getClinics());
                return jsonObject;
        	} else {
    			jsonObject.put("ERROR", "Unable to create HealthCare Professional.");
                return jsonObject;
    		}
        } else {
    		jsonObject.put("ERROR", "Incorrect data.");
    		return jsonObject;
    	}
    }

    public UserExtension createHillromTeamUser(UserExtensionDTO userExtensionDTO) {
    	UserExtension newUser = new UserExtension();
		assignValuesToUserObj(userExtensionDTO, newUser);
		newUser.setActivated(false);
		newUser.setDeleted(false);
		newUser.setActivationKey(RandomUtil.generateActivationKey());
		newUser.getAuthorities().add(authorityRepository.findOne(userExtensionDTO.getRole()));
		userExtensionRepository.save(newUser);
		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}
    
    public UserExtension createPatientUser(UserExtensionDTO userExtensionDTO) {
    	UserExtension newUser = new UserExtension();
    	PatientInfo patientInfo = new PatientInfo();
    	patientInfoRepository.findOneByHillromId(userExtensionDTO.getHillromId())
    	.map(patient -> {
    		return newUser;
    	})
    	.orElseGet(() -> {
    		assignValuesToPatientInfoObj(userExtensionDTO, patientInfo);
    		patientInfoRepository.save(patientInfo);
    		assignValuesToUserObj(userExtensionDTO, newUser);
    		newUser.setPassword(passwordEncoder.encode(generateDefaultPassword((User)newUser)));
    		newUser.setActivated(true);
    		newUser.setDeleted(false);
    		if(AuthoritiesConstants.PATIENT.equals(userExtensionDTO.getRole())) {
    			newUser.setEmail(userExtensionDTO.getHillromId());
    		}
    		newUser.getAuthorities().add(authorityRepository.findOne(userExtensionDTO.getRole()));
			userExtensionRepository.save(newUser);
			UserPatientAssoc userPatientAssoc = new UserPatientAssoc(patientInfo, newUser, AuthoritiesConstants.PATIENT, "SELF");
			userPatientRepository.save(userPatientAssoc);
			newUser.getUserPatientAssoc().add(userPatientAssoc);
			patientInfo.getUserPatientAssoc().add(userPatientAssoc);
			log.debug("Created Information for Patient User: {}", newUser);
			return newUser;
    	});
		return newUser;
	}
    
    public UserExtension createHCPUser(UserExtensionDTO userExtensionDTO) {
    	UserExtension newUser = new UserExtension();
		assignValuesToUserObj(userExtensionDTO, newUser);
		newUser.setActivated(false);
		newUser.setDeleted(false);
		newUser.setActivationKey(RandomUtil.generateActivationKey());
		for(Map<String, String> clinicObj : userExtensionDTO.getClinicList()){
			Clinic clinic = clinicRepository.getOne(Long.parseLong(clinicObj.get("id")));
			newUser.getClinics().add(clinic);
		}
		newUser.getAuthorities().add(authorityRepository.findOne(userExtensionDTO.getRole()));
		userExtensionRepository.save(newUser);
		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}
    
    public JSONObject updateUser(Long id, UserExtensionDTO userExtensionDTO, String baseUrl){
    	JSONObject jsonObject = new JSONObject();
        if(userExtensionDTO.getEmail() != null) {
    		Optional<User> existingUser = userRepository.findOneByEmail(userExtensionDTO.getEmail());
			if(existingUser.isPresent() && existingUser.get().getId() != id) {
				jsonObject.put("ERROR", "e-mail address already in use");
				return jsonObject;
			}
    	}
        List<String> rolesAdminCanModerate = rolesAdminCanModerate();
        if(rolesAdminCanModerate.contains(userExtensionDTO.getRole())
        		&& SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))) {
        	UserExtension user = updateHillromTeamUser(id, userExtensionDTO);
    		if(user.getId() != null) {
    			if(!user.getEmail().equals(userExtensionDTO.getEmail()) && !user.getActivated()) {
    				mailService.sendActivationEmail(user, baseUrl);
    			}
                jsonObject.put("message", "Hillrom User updated successfully.");
                jsonObject.put("user", user);
                return jsonObject;
    		} else {
    			jsonObject.put("ERROR", "Unable to update Hillrom User.");
                return jsonObject;
    		}
    	} else if (AuthoritiesConstants.PATIENT.equals(userExtensionDTO.getRole())) {
           	UserExtension user = updatePatientUser(id, userExtensionDTO);
    		if(user.getId() != null) {
    			if(!user.getEmail().equals(userExtensionDTO.getEmail()) && !user.getActivated()) {
    				mailService.sendActivationEmail(user, baseUrl);
    	    		eventPublisher.publishEvent(new OnCredentialsChangeEvent(user.getId()));
    			}
                jsonObject.put("message", "Patient User updated successfully.");
                jsonObject.put("user", user);
                return jsonObject;
    		} else {
    			jsonObject.put("ERROR", "Unable to update Patient.");
                return jsonObject;
    		}
        } else if (AuthoritiesConstants.HCP.equals(userExtensionDTO.getRole())) {
           	UserExtension user = updateHCPUser(id, userExtensionDTO);
    		if(user.getId() != null) {
    			if(!user.getEmail().equals(userExtensionDTO.getEmail()) && !user.getActivated()) {
    				mailService.sendActivationEmail(user, baseUrl);
    			}
                jsonObject.put("message", "HealthCare Professional updated successfully.");
                jsonObject.put("user", user);
                return jsonObject;
    		} else {
    			jsonObject.put("ERROR", "Unable to update HealthCare Professional.");
                return jsonObject;
    		}
        } else {
    		jsonObject.put("ERROR", "Incorrect data.");
    		return jsonObject;
    	}
    }
    
    public UserExtension updateHillromTeamUser(Long id, UserExtensionDTO userExtensionDTO) {
    	UserExtension user = userExtensionRepository.findOne(id);
		assignValuesToUserObj(userExtensionDTO, user);
		user.getAuthorities().add(authorityRepository.findOne(userExtensionDTO.getRole()));
		userExtensionRepository.save(user);
		log.debug("Updated Information for Hillrom User: {}", user);
		return user;
	}
    
    public UserExtension updatePatientUser(Long id, UserExtensionDTO userExtensionDTO) {
    	UserExtension user = userExtensionRepository.findOne(id);
    	patientInfoRepository.findOneByHillromId(userExtensionDTO.getHillromId())
    	.map(patient -> {
    		assignValuesToPatientInfoObj(userExtensionDTO, patient);
    		patientInfoRepository.save(patient);
    		assignValuesToUserObj(userExtensionDTO, user);
			userExtensionRepository.save(user);
			log.debug("Updated Information for Patient User: {}", user);
    		return user;
    	});
		return user;
	}
    
    public UserExtension updateHCPUser(Long id, UserExtensionDTO userExtensionDTO) {
    	UserExtension hcpUser = userExtensionRepository.findOne(id);
		assignValuesToUserObj(userExtensionDTO, hcpUser);
		userExtensionRepository.save(hcpUser);
		log.debug("Updated Information for HealthCare Proffessional: {}", hcpUser);
		return hcpUser;
	}

	private void assignValuesToPatientInfoObj(UserExtensionDTO userExtensionDTO, PatientInfo patientInfo) {
		patientInfo.setHillromId(userExtensionDTO.getHillromId());
		if(userExtensionDTO.getTitle() != null)
			patientInfo.setTitle(userExtensionDTO.getTitle());
		if(userExtensionDTO.getFirstName() != null)
			patientInfo.setFirstName(userExtensionDTO.getFirstName());
		if(userExtensionDTO.getMiddleName() != null)
			patientInfo.setMiddleName(userExtensionDTO.getMiddleName());
		if(userExtensionDTO.getLastName() != null)
			patientInfo.setLastName(userExtensionDTO.getLastName());
		if(userExtensionDTO.getGender() != null)
			patientInfo.setGender(userExtensionDTO.getGender());
		if(userExtensionDTO.getDob() != null)
			patientInfo.setDob(LocalDate.parse(userExtensionDTO.getDob(), DateTimeFormat.forPattern("MM/dd/yyyy")));
		if(userExtensionDTO.getLangKey() != null)
			patientInfo.setLangKey(userExtensionDTO.getLangKey());
		if(userExtensionDTO.getEmail() != null)
			patientInfo.setEmail(userExtensionDTO.getEmail());
		if(userExtensionDTO.getAddress() != null)
			patientInfo.setAddress(userExtensionDTO.getAddress());
		if(userExtensionDTO.getZipcode() != null)
			patientInfo.setZipcode(userExtensionDTO.getZipcode());
		if(userExtensionDTO.getCity() != null)
			patientInfo.setCity(userExtensionDTO.getCity());
		if(userExtensionDTO.getState() != null)
			patientInfo.setState(userExtensionDTO.getState());
		patientInfo.setWebLoginCreated(true);
	}
    
	private void assignValuesToUserObj(UserExtensionDTO userExtensionDTO, UserExtension newUser) {
		if(userExtensionDTO.getTitle() != null)
			newUser.setTitle(userExtensionDTO.getTitle());
		if(userExtensionDTO.getFirstName() != null)
			newUser.setFirstName(userExtensionDTO.getFirstName());
		if(userExtensionDTO.getMiddleName() != null)
			newUser.setMiddleName(userExtensionDTO.getMiddleName());
		if(userExtensionDTO.getLastName() != null)
			newUser.setLastName(userExtensionDTO.getLastName());
		if(userExtensionDTO.getEmail() != null)
			newUser.setEmail(userExtensionDTO.getEmail());
		if(userExtensionDTO.getSpeciality() != null)
			newUser.setSpeciality(userExtensionDTO.getSpeciality());
		if(userExtensionDTO.getCredentials() != null)
			newUser.setCredentials(userExtensionDTO.getCredentials());
		if(userExtensionDTO.getAddress() != null)
			newUser.setAddress(userExtensionDTO.getAddress());
		if(userExtensionDTO.getZipcode() != null)
			newUser.setZipcode(userExtensionDTO.getZipcode());
		if(userExtensionDTO.getCity() != null)
			newUser.setCity(userExtensionDTO.getCity());
		if(userExtensionDTO.getState() != null)
			newUser.setState(userExtensionDTO.getState());
		if(userExtensionDTO.getPrimaryPhone() != null)
			newUser.setPrimaryPhone(userExtensionDTO.getPrimaryPhone());
		if(userExtensionDTO.getMobilePhone() != null)
			newUser.setMobilePhone(userExtensionDTO.getMobilePhone());
		if(userExtensionDTO.getFaxNumber() != null)
			newUser.setFaxNumber(userExtensionDTO.getFaxNumber());
		if(userExtensionDTO.getNpiNumber() != null)
			newUser.setNpiNumber(userExtensionDTO.getNpiNumber());
		if(userExtensionDTO.getDob() != null)
			newUser.setDob(LocalDate.parse(userExtensionDTO.getDob(), DateTimeFormat.forPattern("MM/dd/yyyy")));
		newUser.setLangKey(userExtensionDTO.getLangKey());
	}

    public Optional<User> findOneByEmail(String email) {
		return userRepository.findOneByEmail(email);
	}

	public User createUserFromPatientInfo(PatientInfo patientInfo,String encodedPassword) {

		String username = getUsernameAsEmailOrHillromIdFromPatientInfo(patientInfo);

		// If User exists already , then return the existing user.
		Optional<User> existingUser = userRepository.findOneByEmail(username);
		if(existingUser.isPresent()){
			return existingUser.get();
		}
		
		User newUser = new User();
		newUser.setActivated(true);
		newUser.setDeleted(false);
		
		Authority patientAuthority = authorityRepository.findOne(AuthoritiesConstants.PATIENT);
		newUser.getAuthorities().add(patientAuthority);
		
		newUser.setCreatedDate(new DateTime());
		
		newUser.setEmail(username.toLowerCase());
		newUser.setFirstName(patientInfo.getFirstName());
		newUser.setLastName(patientInfo.getLastName());
		newUser.setPassword(encodedPassword);
		User persistedUser = userRepository.save(newUser);

		UserPatientAssoc userPatientAssoc = new UserPatientAssoc(patientInfo, newUser, AuthoritiesConstants.PATIENT, "SELF");
		userPatientRepository.save(userPatientAssoc);
		newUser.getUserPatientAssoc().add(userPatientAssoc);
		patientInfo.getUserPatientAssoc().add(userPatientAssoc);
		
		newUser.setId(persistedUser.getId());
		// Update WebLoginCreated to be true  and user patient association
		updateWebLoginStatusAndUserPatientAssoc(patientInfo, persistedUser);
		return newUser;
		
	}

	/**
	 * @param patientInfo
	 * @return
	 */
	private String getUsernameAsEmailOrHillromIdFromPatientInfo(
			PatientInfo patientInfo) {
		// Set the email to hillromId if email is blank
		String username = null;
		if(StringUtils.isNotBlank(patientInfo.getEmail())){
			username = patientInfo.getEmail();
		}else{
			username = patientInfo.getHillromId();
		}
		return username;
	}	

	/**
	 * @param patientInfo
	 * @param persistedUser
	 */
	private void updateWebLoginStatusAndUserPatientAssoc(
			PatientInfo patientInfo, User persistedUser) {
		patientInfoService.findOneByHillromId(patientInfo.getHillromId()).map(patientUser ->{
			patientUser.setWebLoginCreated(true);
			patientInfoService.update(patientUser);
			return patientUser;
		});
	}
	
	public JSONObject updateEmailOrPassword(Map<String,String> params){
		
		String email = params.get("email");
    	String password = params.get("password");
    	String questionId = params.get("questionId");
    	String answer = params.get("answer");
    	String termsAndConditionsAccepted = params.get("termsAndConditionsAccepted");
    	
    	JSONObject errorsJsonObject = validateRequest(password, questionId,
				answer,termsAndConditionsAccepted);
        
        if( null != errorsJsonObject.get("ERROR"))
        	return errorsJsonObject;
        
        User currentUser = findOneByEmail(SecurityUtils.getCurrentLogin()).get();

        errorsJsonObject = isUserExistsWithEmail(email, currentUser);
        
        if(null != errorsJsonObject.get("ERROR")){
        	return errorsJsonObject;
        }
        
        if(null!= email)
        	currentUser.setEmail(email);
        
        Long qid = Long.parseLong(questionId);
        Optional<UserSecurityQuestion> opUserSecQ = userSecurityQuestionService.saveOrUpdate(currentUser.getId(), qid, answer);
        
        if(opUserSecQ.isPresent()){
        	
        	currentUser.setPassword(passwordEncoder.encode(password));
        	currentUser.setLastLoggedInAt(DateTime.now());
        	currentUser.setTermsConditionAccepted(true);
        	currentUser.setTermsConditionAcceptedDate(DateTime.now());
        	userRepository.save(currentUser);
    		eventPublisher.publishEvent(new OnCredentialsChangeEvent(currentUser.getId()));
        	// update email in patientInfo, if the User is Patient
        	updatePatientEmailIfNotPresent(email);
        	
        	log.debug("updateEmailOrPassword for User: {}", currentUser);
        	
        }else{
        	errorsJsonObject.put("ERROR", "Invalid Security Question or Answer");
        	return errorsJsonObject;
        }
		return new JSONObject();
	}

	/**
	 * Checks whether User Exists with provided Email or Whether Email is left blank
	 * @param email
	 * @param currentUser
	 * @return
	 */
	private JSONObject isUserExistsWithEmail(String email, User currentUser) {
		JSONObject jsonObject = new JSONObject();
		if(!RandomUtil.isValidEmail(currentUser.getEmail()) && StringUtils.isBlank(email)){
        	jsonObject.put("ERROR", "Required field Email is missing");
        }
        
        // Update Email for the firstTime Login , if not present
        if(StringUtils.isNotBlank(email)){
        	Optional<User> existingUser = findOneByEmail(email);
        	if(existingUser.isPresent()){
            	jsonObject.put("ERROR", "Email Already registered, please choose another email");
        	}
        }
        return jsonObject;
	}

	/**
	 * This updates Email in PatientInfo, if the loggedIn User is Patient
	 * @param email
	 */
	private void updatePatientEmailIfNotPresent(String email) {
		if(null != email){
        	patientInfoService.findOneByHillromId(SecurityUtils.getCurrentLogin()).ifPresent(patient -> {
        		patient.setEmail(email);        		
        		patientInfoService.update(patient);
        	});
        }
	}

	/**
	 * Validate whether all required fields present in the request
	 * @param password
	 * @param questionId
	 * @param answer
	 * @return
	 */
	private JSONObject validateRequest(String password,
			String questionId, String answer,String termsAndConditionsAccepted) {
		JSONObject jsonObject = new JSONObject();
    	if(!StringUtils.isNotBlank(termsAndConditionsAccepted) || "false".equalsIgnoreCase(termsAndConditionsAccepted)){
    		jsonObject.put("ERROR", "Please accept terms and conditions");
    		return jsonObject;
    	}
    	if(StringUtils.isBlank(answer)){
    		jsonObject.put("ERROR", "Required field Answer is missing");
    		return jsonObject;
    	}
    	if(StringUtils.isBlank(questionId) || !StringUtils.isNumeric(questionId)){
    		jsonObject.put("ERROR", "Required field SecurityQuestion is missing");
    		return jsonObject;
    	}
        if (!checkPasswordLength(password)) {
        	jsonObject.put("ERROR", "Incorrect password");
            return jsonObject;
        }
		return jsonObject;
	}
	
	public JSONObject deleteUser(Long id) {
    	JSONObject jsonObject = new JSONObject();
    	UserExtension existingUser = userExtensionRepository.findOne(id);
    	List<Authority> authorities  = authorityRepository.findAll();
    	Map<String,Authority> authorityMap = new HashMap<>();
    	authorities.stream().forEach(authority -> {
    		authorityMap.put(authority.getName(), authority);
    	});
		if(existingUser.getId() != null) {
			if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority(AuthoritiesConstants.ACCT_SERVICES))) {
				if(existingUser.getAuthorities().contains(authorityMap.get(AuthoritiesConstants.PATIENT))) {
					userExtensionRepository.delete(existingUser);
					jsonObject.put("message", "Patient User deleted successfully.");
					//TO-DO CareGiver deactivate Stuff
				} else if((existingUser.getAuthorities().contains(authorityMap.get(AuthoritiesConstants.HCP))
							|| existingUser.getAuthorities().contains(authorityMap.get(AuthoritiesConstants.CLINIC_ADMIN)))) {
					userExtensionRepository.delete(existingUser);
					jsonObject.put("message", "User deleted successfully.");
				} else {
					jsonObject.put("ERROR", "Unable to delete User.");
				}
			} else if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))
					&& (existingUser.getAuthorities().contains(authorityMap.get(AuthoritiesConstants.ADMIN))
							|| existingUser.getAuthorities().contains(authorityMap.get(AuthoritiesConstants.ACCT_SERVICES))
							|| existingUser.getAuthorities().contains(authorityMap.get(AuthoritiesConstants.ASSOCIATES))
							|| existingUser.getAuthorities().contains(authorityMap.get(AuthoritiesConstants.PATIENT)))) {
				userExtensionRepository.delete(existingUser);
				jsonObject.put("message", "User deleted successfully.");
			} else {
				jsonObject.put("ERROR", "Unable to delete User.");
			}
		} else {
			jsonObject.put("ERROR", "Unable to delete User.");
		}
		return jsonObject;
    }

	public JSONObject updatePasswordSecurityQuestion(Map<String,String> params){
		String requiredParams[] = {"key","password","questionId","answer","termsAndConditionsAccepted"};
		JSONObject errorsJson = RequestUtil.checkRequiredParams(params, requiredParams);
		if(errorsJson.containsKey("ERROR")){
			return errorsJson;
		}
		
		String password = params.get("password");
		if(!checkPasswordLength(password)){
			errorsJson.put("ERROR", "Incorrect Password");
			return errorsJson;
		}
		
		String key = params.get("key");
		Optional<User> existingUser = userRepository.findOneByActivationKey(key);
		User currentUser = null;
		if(existingUser.isPresent()){
			currentUser = existingUser.get();
		}else{
			errorsJson.put("ERROR", "Invalid Activation Key");
			return errorsJson;
		}
		
		Long qid = Long.parseLong(params.get("questionId"));
		String answer = params.get("answer");
		Optional<UserSecurityQuestion> opUserSecQ = userSecurityQuestionService.saveOrUpdate(currentUser.getId(), qid, answer);		
		
		if(opUserSecQ.isPresent()){
			currentUser.setActivationKey(null);
			currentUser.setLastLoggedInAt(DateTime.now());
			currentUser.setLastModifiedDate(DateTime.now());
			currentUser.setPassword(passwordEncoder.encode(params.get("password")));
			currentUser.setTermsConditionAccepted(true);
			currentUser.setTermsConditionAcceptedDate(DateTime.now());
			userRepository.save(currentUser);
		}else{
			errorsJson.put("ERROR","Invalid Security Question or Answer");
			return errorsJson;
		}
		return new JSONObject();
	}
	
	public JSONObject getHCPUser(Long id){
		JSONObject jsonObject = new JSONObject();
		UserExtension hcpUser = userExtensionRepository.findOne(id);
		if(hcpUser.getId() != null) {
			jsonObject.put("message", "HealthCare Professional fetched successfully.");
		    jsonObject.put("user", hcpUser);
		    jsonObject.put("clinics", hcpUser.getClinics());
		} else {
			jsonObject.put("ERROR", "Unable to fetch HealthCare Professional.");
		}	
		return jsonObject;
	 }
	
	public Optional<PatientUserVO> getPatientUser(Long id){
		UserExtension user = userExtensionRepository.findOne(id);
		if(null == user)
			return Optional.empty();
		Set<UserPatientAssoc> associations = user.getUserPatientAssoc();
		List<UserPatientAssoc> listOfassociations = null;
		if (associations.size() > 0) {
			listOfassociations = associations
					.stream()
					.filter(assoc -> "SELF".equalsIgnoreCase(assoc
							.getRelationshipLabel()))
					.collect(Collectors.toList());
		}
		if(listOfassociations.isEmpty()){
			return Optional.of(new PatientUserVO(user,null));
		}
		UserPatientAssoc selfAssociation = listOfassociations.get(0);
		PatientInfo patientInfo = selfAssociation != null ? selfAssociation.getPatient() : null;
		return Optional.of(new PatientUserVO(user,patientInfo));
	}
}

