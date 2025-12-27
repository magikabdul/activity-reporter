package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.lufthansa.model.ReportResponse;
import cloud.cholewa.reporter.lufthansa.repository.LufthansaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LufthansaReportService {

    private final LufthansaRepository lufthansaRepository;

    public Mono<List<ReportResponse>> getMonthlyReport(final int year, final int month) {
        return Mono.empty();
    }
}
