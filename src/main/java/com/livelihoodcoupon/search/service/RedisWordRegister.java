package com.livelihoodcoupon.search.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RedisWordRegister {
	private final RedisService redisService;

	public RedisWordRegister(RedisService redisService) {
		this.redisService = redisService;
	}

	public void wordRegister() {

		//if (!redisService.getRedisTemplate().hasKey("word:서울시")) {
		//address 등록
		redisService.saveWord("서울시", "address", "시", "");
		redisService.saveWord("부산시", "address", "시", "");
		redisService.saveWord("대구시", "address", "시", "");
		redisService.saveWord("인천시", "address", "시", "");
		redisService.saveWord("광주시", "address", "시", "");
		redisService.saveWord("대전시", "address", "시", "");
		redisService.saveWord("울산시", "address", "시", "");
		redisService.saveWord("경기시", "address", "시", "");

		redisService.saveWord("충청북도", "address", "시", "");
		redisService.saveWord("충청남도", "address", "시", "");
		redisService.saveWord("전라북도", "address", "시", "");
		redisService.saveWord("전라남도", "address", "시", "");
		redisService.saveWord("경상북도", "address", "시", "");
		redisService.saveWord("경상남도", "address", "시", "");
		redisService.saveWord("제주", "address", "시", "");
		redisService.saveWord("세종", "address", "시", "");

		log.info("address 시 등록");

		redisService.saveWord("강남구", "address", "구,군", "서울");
		redisService.saveWord("강동구", "address", "구,군", "서울");
		redisService.saveWord("강북구", "address", "구,군", "서울");
		redisService.saveWord("강서구", "address", "구,군", "서울");
		redisService.saveWord("관악구", "address", "구,군", "서울");
		redisService.saveWord("광진구", "address", "구,군", "서울");
		redisService.saveWord("구로구", "address", "구,군", "서울");
		redisService.saveWord("금천구", "address", "구,군", "서울");
		redisService.saveWord("노원구", "address", "구,군", "서울");
		redisService.saveWord("도봉구", "address", "구,군", "서울");
		redisService.saveWord("동대문구", "address", "구,군", "서울");
		redisService.saveWord("동작구", "address", "구,군", "서울");
		redisService.saveWord("마포구", "address", "구,군", "서울");
		redisService.saveWord("서대문구", "address", "구,군", "서울");
		redisService.saveWord("서초구", "address", "구,군", "서울");
		redisService.saveWord("성동구", "address", "구,군", "서울");
		redisService.saveWord("성북구", "address", "구,군", "서울");
		redisService.saveWord("송파구", "address", "구,군", "서울");
		redisService.saveWord("양천구", "address", "구,군", "서울");
		redisService.saveWord("영등포구", "address", "구,군", "서울");
		redisService.saveWord("용산구", "address", "구,군", "서울");
		redisService.saveWord("은평구", "address", "구,군", "서울");
		redisService.saveWord("종로구", "address", "구,군", "서울");

		log.info("address 구.군 등록");

		redisService.saveWord("가회동", "address", "동,면", "종로구");
		redisService.saveWord("견지동", "address", "동,면", "종로구");
		redisService.saveWord("경운동", "address", "동,면", "종로구");
		redisService.saveWord("계동", "address", "동,면", "종로구");
		redisService.saveWord("공평동", "address", "동,면", "종로구");
		redisService.saveWord("관수동", "address", "동,면", "종로구");
		redisService.saveWord("관철동", "address", "동,면", "종로구");
		redisService.saveWord("관훈동", "address", "동,면", "종로구");
		redisService.saveWord("교남동", "address", "동,면", "종로구");
		redisService.saveWord("교북동", "address", "동,면", "종로구");
		redisService.saveWord("구기동", "address", "동,면", "종로구");
		redisService.saveWord("궁정동", "address", "동,면", "종로구");
		redisService.saveWord("권농동", "address", "동,면", "종로구");
		redisService.saveWord("낙원동", "address", "동,면", "종로구");
		redisService.saveWord("내수동", "address", "동,면", "종로구");
		redisService.saveWord("내자동", "address", "동,면", "종로구");
		redisService.saveWord("누상동", "address", "동,면", "종로구");
		redisService.saveWord("누하동", "address", "동,면", "종로구");
		redisService.saveWord("당주동", "address", "동,면", "종로구");
		redisService.saveWord("도렴동", "address", "동,면", "종로구");
		redisService.saveWord("돈의동", "address", "동,면", "종로구");
		redisService.saveWord("동숭동", "address", "동,면", "종로구");
		redisService.saveWord("명륜1가", "address", "동,면", "종로구");
		redisService.saveWord("명륜2가", "address", "동,면", "종로구");
		redisService.saveWord("명륜3가", "address", "동,면", "종로구");
		redisService.saveWord("명륜4가", "address", "동,면", "종로구");
		redisService.saveWord("묘동", "address", "동,면", "종로구");
		redisService.saveWord("무악동", "address", "동,면", "종로구");
		redisService.saveWord("봉익동", "address", "동,면", "종로구");
		redisService.saveWord("부암동", "address", "동,면", "종로구");
		redisService.saveWord("사간동", "address", "동,면", "종로구");
		redisService.saveWord("사직동", "address", "동,면", "종로구");
		redisService.saveWord("삼청동", "address", "동,면", "종로구");
		redisService.saveWord("서린동", "address", "동,면", "종로구");
		redisService.saveWord("세종로", "address", "동,면", "종로구");
		redisService.saveWord("소격동", "address", "동,면", "종로구");
		redisService.saveWord("송월동", "address", "동,면", "종로구");
		redisService.saveWord("송현동", "address", "동,면", "종로구");
		redisService.saveWord("수송동", "address", "동,면", "종로구");
		redisService.saveWord("숭인1동", "address", "동,면", "종로구");
		redisService.saveWord("숭인2동", "address", "동,면", "종로구");
		redisService.saveWord("숭인동", "address", "동,면", "종로구");
		redisService.saveWord("신교동", "address", "동,면", "종로구");
		redisService.saveWord("신문로1가", "address", "동,면", "종로구");
		redisService.saveWord("신문로2가", "address", "동,면", "종로구");
		redisService.saveWord("신영동", "address", "동,면", "종로구");
		redisService.saveWord("안국동", "address", "동,면", "종로구");
		redisService.saveWord("연건동", "address", "동,면", "종로구");
		redisService.saveWord("연지동", "address", "동,면", "종로구");
		redisService.saveWord("예지동", "address", "동,면", "종로구");
		redisService.saveWord("옥인동", "address", "동,면", "종로구");
		redisService.saveWord("와룡동", "address", "동,면", "종로구");
		redisService.saveWord("운니동", "address", "동,면", "종로구");
		redisService.saveWord("원남동", "address", "동,면", "종로구");
		redisService.saveWord("원서동", "address", "동,면", "종로구");
		redisService.saveWord("이화동", "address", "동,면", "종로구");
		redisService.saveWord("익선동", "address", "동,면", "종로구");
		redisService.saveWord("인사동", "address", "동,면", "종로구");
		redisService.saveWord("인의동", "address", "동,면", "종로구");
		redisService.saveWord("장사동", "address", "동,면", "종로구");
		redisService.saveWord("재동", "address", "동,면", "종로구");
		redisService.saveWord("적선동", "address", "동,면", "종로구");
		redisService.saveWord("종로1가", "address", "동,면", "종로구");
		redisService.saveWord("종로2가", "address", "동,면", "종로구");
		redisService.saveWord("종로3가", "address", "동,면", "종로구");
		redisService.saveWord("종로4가", "address", "동,면", "종로구");
		redisService.saveWord("종로5가", "address", "동,면", "종로구");
		redisService.saveWord("종로6가", "address", "동,면", "종로구");
		redisService.saveWord("중학동", "address", "동,면", "종로구");
		redisService.saveWord("창성동", "address", "동,면", "종로구");
		redisService.saveWord("창신1동", "address", "동,면", "종로구");
		redisService.saveWord("창신2동", "address", "동,면", "종로구");
		redisService.saveWord("창신3동", "address", "동,면", "종로구");
		redisService.saveWord("창신동", "address", "동,면", "종로구");
		redisService.saveWord("청운동", "address", "동,면", "종로구");
		redisService.saveWord("청진동", "address", "동,면", "종로구");
		redisService.saveWord("체부동", "address", "동,면", "종로구");
		redisService.saveWord("충신동", "address", "동,면", "종로구");
		redisService.saveWord("통의동", "address", "동,면", "종로구");
		redisService.saveWord("통인동", "address", "동,면", "종로구");
		redisService.saveWord("팔판동", "address", "동,면", "종로구");
		redisService.saveWord("평동", "address", "동,면", "종로구");
		redisService.saveWord("평창동", "address", "동,면", "종로구");
		redisService.saveWord("필운동", "address", "동,면", "종로구");
		redisService.saveWord("행촌동", "address", "동,면", "종로구");
		redisService.saveWord("혜화동", "address", "동,면", "종로구");
		redisService.saveWord("홍지동", "address", "동,면", "종로구");
		redisService.saveWord("홍파동", "address", "동,면", "종로구");
		redisService.saveWord("화동", "address", "동,면", "종로구");
		redisService.saveWord("효자동", "address", "동,면", "종로구");
		redisService.saveWord("효제동", "address", "동,면", "종로구");
		redisService.saveWord("훈정동", "address", "동,면", "종로구");
		log.info("address 동,면 등록");
		//}

		//if (!redisService.getRedisTemplate().hasKey("word:음식점")) {
		redisService.saveWord("음식점", "category", "1차", "");
		redisService.saveWord("숙박", "category", "1차", "");
		redisService.saveWord("카페", "category", "1차", "");
		redisService.saveWord("편의점", "category", "1차", "");
		redisService.saveWord("마트", "category", "1차", "");
		redisService.saveWord("병원", "category", "1차", "");
		redisService.saveWord("약국", "category", "1차", "");
		redisService.saveWord("주차장", "category", "1차", "");
		redisService.saveWord("주유소", "category", "1차", "");
		log.info("category 1차 등록");

		redisService.saveWord("간식", "category", "2차", "음식점");
		redisService.saveWord("분식", "category", "2차", "음식점");
		redisService.saveWord("뷔페", "category", "2차", "음식점");
		redisService.saveWord("술집", "category", "2차", "음식점");
		redisService.saveWord("아시아음식", "category", "2차", "음식점");
		redisService.saveWord("양식", "category", "2차", "음식점");
		redisService.saveWord("일식", "category", "2차", "음식점");
		redisService.saveWord("중식", "category", "2차", "음식점");
		redisService.saveWord("패스트푸드", "category", "2차", "음식점");
		redisService.saveWord("패밀리레스토랑", "category", "2차", "음식점");
		redisService.saveWord("피자", "category", "2차", "음식점");
		redisService.saveWord("치킨", "category", "2차", "음식점");
		redisService.saveWord("한식", "category", "2차", "음식점");
		log.info("category 음식점 2차 등록");

		redisService.saveWord("리조트", "category", "2차", "숙박");
		redisService.saveWord("호텔", "category", "2차", "숙박");
		redisService.saveWord("모텔", "category", "2차", "숙박");
		redisService.saveWord("펜션", "category", "2차", "숙박");
		redisService.saveWord("게스트하우스", "category", "2차", "숙박");
		redisService.saveWord("캠핑", "category", "2차", "숙박");
		redisService.saveWord("야영", "category", "2차", "숙박");
		log.info("category 숙박 2차 등록");
		//}
	}
}
