package com.livelihoodcoupon.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.livelihoodcoupon.search.entity.ParkingLotDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ElasticParkingLotService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ElasticParkingLotServiceTest {

    @Mock
    private ElasticsearchClient client;

    @InjectMocks
    private ElasticParkingLotService service;

    @Test
    @DisplayName("ID로 주차장 문서 조회 성공")
    void getParkingLotById_whenFound_shouldReturnDocument() throws IOException {
        // given
        String docId = "123";
        ParkingLotDocument expectedDoc = ParkingLotDocument.builder()
                .id(123L)
                .parkingLotNm("테스트 주차장")
                .build();

        GetResponse<ParkingLotDocument> getResponse = mock(GetResponse.class);

        when(getResponse.found()).thenReturn(true);
        when(getResponse.source()).thenReturn(expectedDoc);
        when(client.get(any(Function.class), eq(ParkingLotDocument.class))).thenReturn(getResponse);

        // when
        ParkingLotDocument resultDoc = service.getParkingLotById(docId);

        // then
        assertThat(resultDoc).isNotNull();
        assertThat(resultDoc.getParkingLotNm()).isEqualTo(expectedDoc.getParkingLotNm());
    }

    @Test
    @DisplayName("ID로 주차장 문서 조회 실패 - 문서를 찾을 수 없음")
    void getParkingLotById_whenNotFound_shouldReturnNull() throws IOException {
        // given
        String docId = "404";
        GetResponse<ParkingLotDocument> getResponse = mock(GetResponse.class);

        when(getResponse.found()).thenReturn(false);
        when(client.get(any(Function.class), eq(ParkingLotDocument.class))).thenReturn(getResponse);

        // when
        ParkingLotDocument resultDoc = service.getParkingLotById(docId);

        // then
        assertThat(resultDoc).isNull();
    }
}
