package btc.analyzer.controller;

import btc.analyzer.data.AmountDays;
import btc.analyzer.service.DataCollectorService;
import btc.analyzer.service.DataMakerDecisionService;
import btc.analyzer.service.DataProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("api")
public class BitcoinAgentController {

    private final DataCollectorService bitcoinCollectorService;
    private final DataProcessorService bitcoinProcessorService;
    private final DataMakerDecisionService bitcoinDecisionService;
    private final AmountDays amountDaysEntity;

    public BitcoinAgentController(
            DataCollectorService bitcoinCollectorService,
            DataProcessorService bitcoinProcessorService,
            DataMakerDecisionService bitcoinDecisionService,
            AmountDays amountDaysEntity) {
        this.bitcoinCollectorService = bitcoinCollectorService;
        this.bitcoinProcessorService = bitcoinProcessorService;
        this.bitcoinDecisionService = bitcoinDecisionService;
        this.amountDaysEntity = amountDaysEntity;
    }

    @PostMapping("/bitcoin-analysis")
    public ResponseEntity<String> postBitcoinAnalysis(@RequestParam int amountDays) {
        this.amountDaysEntity.setAmountDays(amountDays);
        try {
            bitcoinCollectorService.collectData();
            bitcoinProcessorService.processData();
            return ResponseEntity.ok(bitcoinDecisionService.makeDecision());
        } catch (RuntimeException e) {
            log.error("Произошла ошибка: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Произошла ошибка при анализе данных Bitcoin.");
        }
    }
}
