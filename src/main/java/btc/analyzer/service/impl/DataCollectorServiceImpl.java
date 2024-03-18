package btc.analyzer.service.impl;

import btc.analyzer.data.AmountDays;
import btc.analyzer.data.BitcoinNews;
import btc.analyzer.data.BitcoinPrice;
import btc.analyzer.holder.BitcoinNewsHolder;
import btc.analyzer.holder.BitcoinPriceHolder;
import btc.analyzer.service.DataCollectorService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataCollectorServiceImpl implements DataCollectorService {

    private final RestTemplate restTemplate;
    private final BitcoinPriceHolder bitcoinPriceHolder;
    private final BitcoinNewsHolder bitcoinNewsHolder;
    private final AmountDays amountDays;
    private List<String> keywords;

    @Value("${bitcoin.keywords}")
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    public void collectData() {
        bitcoinPriceHolder.clear();
        bitcoinNewsHolder.clear();
        collectBitcoinPrices();
        collectBitcoinNews();
    }

    private void collectBitcoinPrices() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Double> bitcoinPriceHistory = (Map<String, Double>) Objects
                    .requireNonNull(collectTypeAsync("price").get().get("bpi"));

            for (Map.Entry<String, Double> entry : Objects.requireNonNull(bitcoinPriceHistory).entrySet()) {
                LocalDate date = LocalDate.parse(entry.getKey());
                Double price = Double.parseDouble(String.valueOf(entry.getValue()));
                bitcoinPriceHolder.add(new BitcoinPrice(date, price));
            }
        } catch (Exception e) {
            log.error("Error: Bitcoin prices is not collected! {}", e.getMessage());
        }
    }

    private void collectBitcoinNews() {
        String keywordPattern = String.join("|", keywords);
        Pattern pattern = Pattern.compile(keywordPattern);
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> articles = (List<Map<String, Object>>)
                    Objects.requireNonNull(collectTypeAsync("news").get().get("articles"));
            if (articles != null) {
                for (Map<String, Object> article : articles) {
                    BitcoinNews newsItem = new BitcoinNews();
                    newsItem.setContent((String) article.get("content"));
                    newsItem.setTitle((String) article.get("title"));
                    Matcher matcher = pattern.matcher(newsItem.getContent());
                    if (matcher.find()) {
                        bitcoinNewsHolder.add(newsItem);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error: Bitcoin news is not collected! {}", e.getMessage());
        }
    }

    private CompletableFuture<Map<String, Object>> collectTypeAsync(String type) {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(amountDays.getAmountDays());
        LocalDate startDateForNews = LocalDate.of(2024, 1, 10);

        String bitcoinPriceUrl =
                "https://api.coindesk.com/v1/bpi/historical/close.json?start=" + startDate.toString()
                        + "&end=" + endDate.toString();

        String bitcoinNewsUrl =
                "https://newsapi.org/v2/everything?q=биткоин&from=" + startDateForNews.toString()
                        + "&sortBy=publishedAt&apiKey=YourKey";

        String choiceType = type.equals("news") ? bitcoinNewsUrl : bitcoinPriceUrl;

        return CompletableFuture.supplyAsync(() -> {
            try {
                return restTemplate.exchange(
                        choiceType,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }
                ).getBody();
            } catch (Exception e) {
                log.error("Error: Collector type is failed! {}", e.getMessage());
                throw new RuntimeException("Error: Collector type future is failed!");
            }
        }, executor);
    }

    @PreDestroy
    public void setExecutor(){
        if (executor !=null ){
            executor.shutdown();
        }
    }

}
