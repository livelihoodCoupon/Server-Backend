package com.livelihoodcoupon.collector.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livelihoodcoupon.collector.dto.RegionKeywordDto;
import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.collector.repository.PlaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvExportService {

	private static final Logger log = LoggerFactory.getLogger(CsvExportService.class);
	private final PlaceRepository placeRepository;

	/**
	 * DB에 저장된 모든 (지역, 키워드) 조합의 데이터를 각각의 CSV 파일로 생성합니다.
	 */
	public void exportAllRegionsToCsv() {
		log.info("전체 (지역, 키워드) 조합의 CSV 파일 내보내기 작업을 시작합니다.");

		List<RegionKeywordDto> regionKeywords = placeRepository.findDistinctRegionAndKeyword();
		if (regionKeywords.isEmpty()) {
			log.info("내보낼 데이터가 DB에 없습니다.");
			return;
		}

		log.info("총 {}개 (지역, 키워드) 조합에 대한 CSV 파일 생성을 시작합니다.", regionKeywords.size());

		for (RegionKeywordDto dto : regionKeywords) {
			exportSingleRegionToCsv(dto.getRegionName(), dto.getKeyword());
		}

		log.info("전체 (지역, 키워드) 조합의 CSV 파일 내보내기 작업을 완료했습니다.");
	}

	/**
	 * 특정 지역과 키워드의 데이터를 CSV 파일로 생성합니다.
	 * @param regionName 지역 이름
	 * @param keyword 키워드
	 */
	@Transactional(readOnly = true)
	public void exportSingleRegionToCsv(String regionName, String keyword) {
		String filename = String.format("data/csv/%s_%s_places_data.csv",
			regionName.replace(" ", "_"),
			keyword.replace(" ", "_"));
		log.info("[{}-{}] CSV 파일 저장을 시작합니다. 파일명: {}", regionName, keyword, filename);

		new File("data/csv").mkdirs();

		AtomicLong count = new AtomicLong(0);

		try (Stream<PlaceEntity> placesStream = placeRepository.streamByRegionAndKeyword(regionName, keyword);
			 PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {

			writer.println(
				"placeId,region,placeName,roadAddress,lotAddress,lat,lng,phone,categoryName,keyword,categoryGroupCode,categoryGroupName,placeUrl");

			placesStream.forEach(place -> {
				writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%f,%f,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
					place.getPlaceId(),
					place.getRegion(),
					place.getPlaceName(),
					place.getRoadAddress(),
					place.getLotAddress(),
					place.getLat(),
					place.getLng(),
					place.getPhone(),
					place.getCategory(),
					place.getKeyword(),
					place.getCategoryGroupCode(),
					place.getCategoryGroupName(),
					place.getPlaceUrl()
				);
				count.getAndIncrement();
			});

			log.info("[{}-{}] CSV 파일 저장을 완료했습니다. (총 {}건)", regionName, keyword, count.get());
		} catch (IOException e) {
			log.error("[{}-{}] CSV 파일 저장 중 오류가 발생했습니다: {}", regionName, keyword, e.getMessage());
		}
	}
}
