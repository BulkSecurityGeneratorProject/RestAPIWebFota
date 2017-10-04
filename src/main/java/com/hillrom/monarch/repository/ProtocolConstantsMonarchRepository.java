package com.hillrom.monarch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hillrom.vest.domain.ProtocolConstants;
import com.hillrom.vest.domain.ProtocolConstantsMonarch;

public interface ProtocolConstantsMonarchRepository extends
		JpaRepository<ProtocolConstantsMonarch, Long> {

}
