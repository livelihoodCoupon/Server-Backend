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

/**
 * Elasticsearch 인덱스 관리를 위한 관리자용 컨트롤러.
 * <p>
 * 인덱스 생성, 삭제 등의 관리 기능을 제공합니다.
 * 데이터 색인(indexing)과 관련된 배치 작업은
 * {@link com.livelihoodcoupon.batch.controller.ElasticsearchBatchController}를 참조하십시오.
 * </p>
 */
@RestController
@RequestMapping("/admin/es")
@RequiredArgsConstructor
@Profile("!test")
public class ElasticAdminController {

	private final ElasticIndexService elasticIndexService;

	/**
	 * Elasticsearch에 필요한 인덱스들을 생성합니다.
	 * <p>
	 * places, places_autocomplete 등 사전에 정의된 매핑에 따라 인덱스를 생성합니다.
	 * 이 작업은 데이터를 색인하기 전에 반드시 선행되어야 합니다.
	 * </p>
	 * @return 인덱스 생성 성공 메시지
	 */
	@PostMapping("/indices")
	public ResponseEntity<CustomApiResponse<?>> createIndices() {
		elasticIndexService.createIndices();
		return ResponseEntity.ok(CustomApiResponse.success("인덱스 생성 절차가 성공적으로 완료되었습니다."));
	}

	/**
	 * Elasticsearch의 인덱스들을 삭제합니다.
	 * <p>
	 * <strong>주의:</strong> 인덱스에 저장된 모든 데이터가 영구적으로 삭제됩니다.
	 * </p>
	 * @return 인덱스 삭제 성공 메시지
	 * @throws IOException Elasticsearch 통신 오류 발생 시
	 */
	@DeleteMapping("/indices")
	public ResponseEntity<CustomApiResponse<?>> deleteIndices() throws IOException {
		elasticIndexService.deleteIndices();
		return ResponseEntity.ok(CustomApiResponse.success("인덱스 삭제 절차가 성공적으로 완료되었습니다."));
	}
}
