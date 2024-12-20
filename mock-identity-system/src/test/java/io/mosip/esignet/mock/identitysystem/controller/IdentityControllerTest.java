/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.esignet.mock.identitysystem.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.mosip.esignet.mock.identitysystem.dto.*;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.esignet.mock.identitysystem.service.IdentityService;
import io.mosip.esignet.mock.identitysystem.util.ErrorConstants;

@RunWith(SpringRunner.class)
@WebMvcTest(value = IdentityController.class)
public class IdentityControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	IdentityService identityService;

	public static final String UTC_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	ObjectMapper objectMapper = new ObjectMapper();

	IdentityData identityRequest;

	@Before
	public void init() {
		identityRequest = new IdentityData();
		identityRequest.setIndividualId("123456789");
		identityRequest.setEmail("test@gmail.com");
		LanguageValue langValue = new LanguageValue();
		langValue.setLanguage("eng");
		langValue.setValue("ind");
		identityRequest.setCountry(Arrays.asList(langValue));
		identityRequest.setDateOfBirth("20021990");
		identityRequest.setEncodedPhoto("testencodedphoto");
		identityRequest.setGender(Arrays.asList(langValue));
		identityRequest.setLocality(Arrays.asList(langValue));
		identityRequest.setPostalCode("12011");
		identityRequest.setPin("1289001");
		identityRequest.setRegion(Arrays.asList(langValue));
		identityRequest.setFullName(Arrays.asList(langValue));
		identityRequest.setGivenName(Arrays.asList(langValue));
		identityRequest.setFamilyName(Arrays.asList(langValue));
		identityRequest.setStreetAddress(Arrays.asList(langValue));
		identityRequest.setPhone("9090909090");
	}

	@Test
	public void createIdentity_withValidIdentity_returnSuccessResponse() throws Exception {
		RequestWrapper<IdentityData> requestWrapper = new RequestWrapper<IdentityData>();
		ZonedDateTime requestTime = ZonedDateTime.now(ZoneOffset.UTC);
		requestWrapper.setRequestTime(requestTime.format(DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN)));
		requestWrapper.setRequest(identityRequest);

		Mockito.doNothing().when(identityService).addIdentity(identityRequest);

		mockMvc.perform(post("/identity").content(objectMapper.writeValueAsString(requestWrapper))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.response.status").value("mock identity data created successfully"));
	}

	@Test
	public void createIdentity_withInvalidIdentity_returnErrorResponse() throws Exception {
		RequestWrapper<IdentityData> requestWrapper = new RequestWrapper<IdentityData>();
		ZonedDateTime requestTime = ZonedDateTime.now(ZoneOffset.UTC);
		requestWrapper.setRequestTime(requestTime.format(DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN)));
		identityRequest.setIndividualId(null);
		requestWrapper.setRequest(identityRequest);

		Mockito.doNothing().when(identityService).addIdentity(identityRequest);

		mockMvc.perform(post("/identity").content(objectMapper.writeValueAsString(requestWrapper))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.errors").isNotEmpty())
				.andExpect(jsonPath("$.errors[*].errorCode").value(Matchers.containsInAnyOrder(ErrorConstants.INVALID_REQUEST, ErrorConstants.INVALID_INDIVIDUAL_ID)));
	}
	
	@Test
	public void getIdentity_withValidId_returnSuccessResponse() throws Exception {
		Mockito.when(identityService.getIdentity(Mockito.anyString())).thenReturn(identityRequest);

		mockMvc.perform(get("/identity/{individualId}", "123456789")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.response.individualId").value("123456789"));
	}

	@Test
	public void addVerifiedClaims_withValidDetails_returnSuccessResponse() throws Exception {

		RequestWrapper<VerifiedClaimRequestDto> requestWrapper = new RequestWrapper<VerifiedClaimRequestDto>();
		ZonedDateTime requestTime = ZonedDateTime.now(ZoneOffset.UTC);
		requestWrapper.setRequestTime(requestTime.format(DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN)));

		VerifiedClaimRequestDto verifiedClaimRequestDto = new  VerifiedClaimRequestDto();
		verifiedClaimRequestDto.setActive(true);
		verifiedClaimRequestDto.setIndividualId("123456789");
		Map<String, JsonNode> verificationDetail = new HashMap<>();

		ObjectNode emailVerification = objectMapper.createObjectNode();
		emailVerification.put("trust_framework", "testTrustFramework");
		verificationDetail.put("testClaim", emailVerification);
		verifiedClaimRequestDto.setVerificationDetail(verificationDetail);

		requestWrapper.setRequest(verifiedClaimRequestDto);

		Mockito.doNothing().when(identityService).addVerifiedClaim(verifiedClaimRequestDto);
		Mockito.when(identityService.getIdentity(Mockito.anyString())).thenReturn(identityRequest);

		mockMvc.perform(post("/identity/add-verified-claim").content(objectMapper.writeValueAsString(requestWrapper))
						.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.response.status").value("Verified Claim added successfully"));
	}

	@Test
	public void addVerifiedClaim_withInvalidClaim_returnErrorResponse() throws  Exception {
		RequestWrapper<VerifiedClaimRequestDto> requestWrapper = new RequestWrapper<VerifiedClaimRequestDto>();
		ZonedDateTime requestTime = ZonedDateTime.now(ZoneOffset.UTC);
		requestWrapper.setRequestTime(requestTime.format(DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN)));

		VerifiedClaimRequestDto verifiedClaimRequestDto = new  VerifiedClaimRequestDto();
		verifiedClaimRequestDto.setActive(true);
		verifiedClaimRequestDto.setIndividualId("123456789");
		Map<String, JsonNode> verificationDetail = new HashMap<>();
		verifiedClaimRequestDto.setVerificationDetail(verificationDetail);

		requestWrapper.setRequest(verifiedClaimRequestDto);

		Mockito.doNothing().when(identityService).addVerifiedClaim(verifiedClaimRequestDto);

		mockMvc.perform(post("/identity/add-verified-claim").content(objectMapper.writeValueAsString(requestWrapper))
						.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.errors").isNotEmpty())
				.andExpect(jsonPath("$.errors[0].errorCode").value(ErrorConstants.INVALID_REQUEST));
	}

	@Test
	public void updateIdentity_withValidIdentity_thenPass() throws Exception {
		RequestWrapper<IdentityData> requestWrapper = new RequestWrapper<IdentityData>();
		ZonedDateTime requestTime = ZonedDateTime.now(ZoneOffset.UTC);
		requestWrapper.setRequestTime(requestTime.format(DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN)));
		requestWrapper.setRequest(identityRequest);

		Mockito.doNothing().when(identityService).updateIdentity(identityRequest);

		mockMvc.perform(put("/identity").content(objectMapper.writeValueAsString(requestWrapper))
						.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.response.status").value("mock Identity data updated successfully"));
	}

}
