package com.hillrom.vest.service;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.hillrom.vest.domain.UserSecurityQuestion;
import com.hillrom.vest.repository.SecurityQuestionRepository;
import com.hillrom.vest.repository.UserRepository;
import com.hillrom.vest.repository.UserSecurityQuestionRepository;

@Service
@Transactional
public class UserSecurityQuestionService {

	@Inject
	private UserSecurityQuestionRepository userSecurityQuestionRepository;
	
	@Inject
	private UserRepository userRepository;
	
	@Inject
	private SecurityQuestionRepository questionRepository;
	
	public Optional<UserSecurityQuestion> findOneByUserId(Long userId){
		return userSecurityQuestionRepository.findOneByUserId(userId);
	}
	
	public Optional<UserSecurityQuestion> findOneByQuestionId(Long questionId){
		return userSecurityQuestionRepository.findOneByQuestionId(questionId);
	}
	
	public Optional<UserSecurityQuestion> findOneByUserIdAndQuestionId(Long userId,Long questionId){
		return userSecurityQuestionRepository.findOneByUserIdAndQuestionId(userId,questionId);
	}
	
	public Optional<UserSecurityQuestion> save(Long userId,Long questionId,String answer){
		if(null == userId || null == questionId || null == answer)
			return Optional.empty();
		UserSecurityQuestion userSecurityQuestion = new  UserSecurityQuestion();
		userSecurityQuestion.setAnswer(answer);
		userSecurityQuestion.setSecurityQuestion(questionRepository.findOne(questionId));
		userSecurityQuestion.setUser(userRepository.findOne(userId));
		return Optional.of(userSecurityQuestionRepository.save(userSecurityQuestion));
	}
	
	public Optional<UserSecurityQuestion> update(Long userId,Long questionId,String answer){
		if(null == userId || null == questionId || null == answer)
			return Optional.empty();
		return  findOneByUserIdAndQuestionId(userId,questionId).map(existingQ ->{
			existingQ.setAnswer(answer);
			return userSecurityQuestionRepository.save(existingQ);
		});
	}
}
