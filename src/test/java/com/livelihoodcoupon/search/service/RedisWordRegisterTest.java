package com.livelihoodcoupon.search.service;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisWordRegisterTest {
	@Mock
	RedisTemplate<String, String> redisTemplate;

	@Mock
	private RedisService redisService;

	@InjectMocks
	private RedisWordRegister redisWordRegister;

	@Mock
	private HashOperations<String, String, String> hashOps;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		redisWordRegister = new RedisWordRegister(redisService);

		// RedisService.getRedisTemplate() mock
		doReturn(redisTemplate).when(redisService).getRedisTemplate();

		// final 메서드 stub는 doReturn
		doReturn(false).when(redisTemplate).hasKey(anyString());
		doReturn(hashOps).when(redisTemplate).opsForHash();

		// saveWord는 실제로 아무 동작 안 함
		doNothing().when(redisService).saveWord(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("reids 단어등록 성공")
	void fileWordRegister_shouldSaveWords_whenFileHasContent() throws IOException, IOException {
		// given
		String redisKey = "address";

		// ClassPathResource를 mock해서 입력 스트림 제공
		String fileContent = "서울시\n강남구\n";
		InputStream fakeInput = new ByteArrayInputStream(fileContent.getBytes());

		ClassPathResource mockResource = mock(ClassPathResource.class);
		when(mockResource.getInputStream()).thenReturn(fakeInput);

		// ClassPathResource 생성 부분을 스파이로 대체
		// -> fileWordRegister에서 new ClassPathResource(...) 대신 mockResource 사용
		RedisWordRegister spyRegister = spy(redisWordRegister);
		doReturn(mockResource).when(spyRegister).createResource(anyString());

		// when
		spyRegister.fileWordRegister(redisKey);

		// then
		// RedisService.saveWord가 각 단어에 대해 호출됐는지 확인
		verify(redisService).saveWord("서울시", "address", "", "");
		verify(redisService).saveWord("강남구", "address", "", "");
	}

}
