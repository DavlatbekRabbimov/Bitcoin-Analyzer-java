package btc.analyzer.service.impl;

import btc.analyzer.data.AmountDays;
import btc.analyzer.data.BitcoinNews;
import btc.analyzer.holder.BitcoinNewsHolder;
import btc.analyzer.holder.BitcoinPriceHolder;
import btc.analyzer.service.DataMakerDecisionService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataMakerDecisionServiceImpl implements DataMakerDecisionService {

    private final BitcoinPriceHolder bitcoinPriceHolder;
    private final BitcoinNewsHolder bitcoinNewsHolder;
    private final AmountDays amountDays;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    public String makeDecision() {
        try {
            double start = bitcoinPriceHolder.startPrice();
            double end = bitcoinPriceHolder.endPrice();
            double changedPrice = (end - start) / start * 100;
            changedPrice = Math.round(changedPrice * 100.0) / 100.0;

            String priceTextResponse = String.format(
                    "Изменение цены Bitcoin за последние %d дней составило %.2f",
                    amountDays.getAmountDays(),
                    changedPrice
            );
            String reasonTextResponse = "%.\n\nФакторы, влияющие на изменение цен:\n";
            StringBuilder decision = new StringBuilder(priceTextResponse.concat(reasonTextResponse));

            List<BitcoinNews> positiveNewsList = new ArrayList<>();
            List<BitcoinNews> negativeNewsList = new ArrayList<>();

            List<Future<Void>> futures = new ArrayList<>();
            for (BitcoinNews news : bitcoinNewsHolder.get()) {
                futures.add(executor.submit(() -> {
                    if (!news.getContent().isEmpty()) {
                        if (news.isPositive()) {
                            synchronized (positiveNewsList) {
                                positiveNewsList.add(news);
                            }
                        } else {
                            synchronized (negativeNewsList) {
                                negativeNewsList.add(news);
                            }
                        }
                    }
                    return null;
                }));
            }

            for (Future<Void> future : futures) {
                future.get();
            }

            if(changedPrice > 0) {
                for (BitcoinNews positive: positiveNewsList) {
                    decision.append("Позитивный фактор: ").append(positive.getTitle()).append("\n");
                }
            }

            if(changedPrice < 0) {
                for(BitcoinNews negative : negativeNewsList){
                    decision.append("Негативный фактор: ").append(negative.getTitle()).append("\n");
                }
            }

            if (changedPrice == 0){
                decision.append("Отмечается отсутствие публикаций о динамике цен на биткоин, " +
                        "будь то рост или снижение!");
            }

            return decision.toString();
        } catch (Exception e) {
            log.error("Error: makeDecision is failed! - {}", e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
