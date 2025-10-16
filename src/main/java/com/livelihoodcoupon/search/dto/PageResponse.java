package com.livelihoodcoupon.search.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;
import lombok.Setter;

/**
 * 검색목록을 페이징에서 사용하는 dto
 **/
@Getter
@Setter
public class PageResponse<T> {

	//@Schema(description = "페이지 내용", example = "리스트값을 배열로.")
	private List<T> content;

	//@Schema(description = "현재페이지", example = "1")
	private int currentPage;

	//@Schema(description = "총페이지", example = "1")
	private int totalPages;

	//@Schema(description = "총갯수", example = "10")
	private long totalElements;

	//@Schema(description = "시작페이지 번호", example = "1")
	private int startPage;

	//@Schema(description = "끝페이지 번호", example = "1")
	private int endPage;

	//@Schema(description = "이전글여부", example = "true")
	private boolean hasPrev;

	//@Schema(description = "다음글여부", example = "false")
	private boolean hasNext;

	//@Schema(description = "페이지블록수", example = "10")
	private int blockSize;

	//@Schema(description = "첫페이지여부", example = "true")
	private boolean isFirst;

	//@Schema(description = "끝페이지여부", example = "false")
	private boolean isLast;

	private double searchCenterLat;
	private double searchCenterLng;

	public PageResponse(Page<T> page, int blockSize, double searchCenterLat, double searchCenterLng) {
		this.content = page.getContent();
		this.currentPage = page.getNumber() + 1;

		this.totalPages = page.getTotalPages();
		this.totalElements = page.getTotalElements();

		int tempEnd = (int)(Math.ceil((double)currentPage / blockSize) * blockSize);
		this.startPage = Math.max(tempEnd - blockSize + 1, 1);
		this.endPage = Math.min(tempEnd, totalPages);

		this.hasPrev = startPage > 1;
		this.hasNext = endPage < totalPages;
		this.blockSize = blockSize;
		this.isFirst = page.isFirst();
		this.isLast = page.isLast();
		this.searchCenterLat = searchCenterLat;
		this.searchCenterLng = searchCenterLng;
	}

}
