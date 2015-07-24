package com.hillrom.vest.web.rest;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import net.minidev.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.hillrom.vest.domain.Authority;
import com.hillrom.vest.domain.User;
import com.hillrom.vest.repository.UserRepository;
import com.hillrom.vest.security.SecurityUtils;
import com.hillrom.vest.service.MailService;
import com.hillrom.vest.service.UserLoginTokenService;
import com.hillrom.vest.service.UserService;
import com.hillrom.vest.web.rest.dto.UserDTO;


/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserService userService;

    @Inject
    private MailService mailService;
    
    @Inject
    private UserLoginTokenService authTokenService;

    /**
     * POST  /register -> register the user.
     */
    @RequestMapping(value = "/register",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<?> registerAccount(@Valid @RequestBody UserDTO userDTO, HttpServletRequest request) {
        return userRepository.findOneByEmail(userDTO.getEmail())
                .map(user -> new ResponseEntity<>("e-mail address already in use", HttpStatus.BAD_REQUEST))
                .orElseGet(() -> {
                    User user = userService.createUserInformation(userDTO.getPassword(),
                    userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail().toLowerCase(),
                    userDTO.getLangKey());
                    String baseUrl = request.getScheme() + // "http"
                    "://" +                                // "://"
                    request.getServerName() +              // "myhost"
                    ":" +                                  // ":"
                    request.getServerPort();               // "80"

                    mailService.sendActivationEmail(user, baseUrl);
                    return new ResponseEntity<>(HttpStatus.CREATED);
                });
    }

    /**
     * GET  /activate -> activate the registered user.
     */
    @RequestMapping(value = "/activate",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<String> activateAccount(@RequestParam(value = "key") String key) {
        return Optional.ofNullable(userService.activateRegistration(key))
            .map(user -> new ResponseEntity<String>(HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * GET  /authenticate -> check if the user is authenticated, and return its login.
     */
    @RequestMapping(value = "/authenticate",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET  /account -> get the current user.
     */
    @RequestMapping(value = "/account",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<UserDTO> getAccount() {
        return Optional.ofNullable(userService.getUserWithAuthorities())
            .map(user -> {
                return new ResponseEntity<>(
                    new UserDTO(
                        null,
                        user.getTitle(),
                        user.getFirstName(),
                        user.getMiddleName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getGender(),
                        user.getZipcode(),
                        user.getLangKey(),
                        user.getAuthorities().stream().map(Authority::getName)
                            .collect(Collectors.toList())),
                HttpStatus.OK);
            })
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * POST  /account -> update the current user information.
     */
    @RequestMapping(value = "/account",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<String> saveAccount(@RequestBody UserDTO userDTO) {
        return userRepository
            .findOneByEmail(userDTO.getEmail())
            .filter(u -> u.getEmail().equals(SecurityUtils.getCurrentLogin()))
            .map(u -> {
                userService.updateUserInformation(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(),
                    userDTO.getLangKey());
                return new ResponseEntity<String>(HttpStatus.OK);
            })
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * POST  /change_password -> changes the current user's password
     */
    @RequestMapping(value = "/account/change_password",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<JSONObject> changePassword(@RequestBody String password) {
        JSONObject jsonObject = userService.changePassword(password);
        if(jsonObject.containsKey("ERROR")){
        	return ResponseEntity.badRequest().body(jsonObject);
        }
        return ResponseEntity.ok().body(jsonObject);
    }

    @RequestMapping(value = "/account/reset_password/init",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<JSONObject> requestPasswordReset(@RequestBody Map<String, String> body, HttpServletRequest request) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", "e-mail address not registered");
        return userService.requestPasswordReset(body.get("email"))
        		.map(user -> {
        			String baseUrl = request.getScheme() +
        					"://" +
        					request.getServerName() +
        					":" +
        					request.getServerPort();
        			mailService.sendPasswordResetMail(user, baseUrl);
        			jsonObject.put("message", "e-mail sent successfully.");
        			return ResponseEntity.ok().body(jsonObject);
               }).orElse(ResponseEntity.badRequest().body(jsonObject));
    }

    @RequestMapping(value = "/account/reset_password/finish",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<JSONObject> finishPasswordReset(@RequestParam(value = "key") String key, @RequestBody(required=true) Map<String,String> body) {
        body.put("key", key);
        JSONObject jsonObject = userService.completePasswordReset(body);
        if(jsonObject.containsKey("ERROR")){
        	return ResponseEntity.badRequest().body(jsonObject);
        }else{
        	return ResponseEntity.ok().body(jsonObject);
        }
    }

    private boolean checkPasswordLength(String password) {
      return (!StringUtils.isEmpty(password) && password.length() >= UserDTO.PASSWORD_MIN_LENGTH && password.length() <= UserDTO.PASSWORD_MAX_LENGTH);
    }
    
    /**
     * POST  /update_emailpassword -> changes the current user's email,password
     */
    @RequestMapping(value = "/account/update_emailpassword",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<JSONObject> updateEmailOrPassword(@RequestBody(required=true) Map<String,String> params,@RequestHeader(value="x-auth-token",required=true)String authToken) {
    	params.put("x-auth-token", authToken);
    	JSONObject errorsJsonObject = userService.updateEmailOrPassword(params);
    	if(null != errorsJsonObject.get("ERROR"))
    		return ResponseEntity.badRequest().body(errorsJsonObject);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    /**
     * POST  /update_emailpassword -> changes the current user's email,password
     */
    @RequestMapping(value = "/account/update_passwordsecurityquestion",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<JSONObject> updatePasswordSecurityQuestion(@RequestBody(required=true) Map<String,String> params) {
    	JSONObject errorsJsonObject = userService.updatePasswordSecurityQuestion(params);
    	if(null != errorsJsonObject.get("ERROR"))
    		return ResponseEntity.badRequest().body(errorsJsonObject);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
