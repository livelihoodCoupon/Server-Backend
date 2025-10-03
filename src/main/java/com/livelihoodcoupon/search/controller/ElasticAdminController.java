package com.livelihoodcoupon.search.controller;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.search.service.ElasticIndexService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/es")
@RequiredArgsConstructor
@Profile("!test")
public class ElasticAdminController {

	private final ElasticIndexService elasticIndexService;

	@PostMapping("/indices")
	public ResponseEntity<CustomApiResponse<?>> createIndices() {
		elasticIndexService.createIndices();
		return ResponseEntity.ok(CustomApiResponse.success("인덱스 생성 절차가 성공적으로 완료되었습니다."));
	}

	@DeleteMapping("/indices")
	public ResponseEntity<CustomApiResponse<?>> deleteIndices() throws IOException {
		elasticIndexService.deleteIndices();
		return ResponseEntity.ok(CustomApiResponse.success("인덱스 삭제 절차가 성공적으로 완료되었습니다."));
	}
}
