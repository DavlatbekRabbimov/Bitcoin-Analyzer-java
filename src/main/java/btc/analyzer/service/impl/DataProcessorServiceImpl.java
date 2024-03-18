package btc.analyzer.service.impl;

import btc.analyzer.data.BitcoinNews;
import btc.analyzer.holder.BitcoinNewsHolder;
import btc.analyzer.holder.BitcoinPriceHolder;
import btc.analyzer.service.DataProcessorService;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class DataProcessorServiceImpl implements DataProcessorService {
    private final BitcoinPriceHolder bitcoinPriceHolder;
    private final BitcoinNewsHolder bitcoinNewsHolder;
    private final StanfordCoreNLP pipeline;

    public DataProcessorServiceImpl(
            BitcoinPriceHolder bitcoinPriceHolder,
            BitcoinNewsHolder bitcoinNewsHolder,
            StanfordCoreNLP pipeline) {
        this.bitcoinPriceHolder = bitcoinPriceHolder;
        this.bitcoinNewsHolder = bitcoinNewsHolder;
        this.pipeline = pipeline;
    }

    @Override
    public void processData() {
        boolean priceIncreased = bitcoinPriceHolder.endPrice() > bitcoinPriceHolder.startPrice();
        try {
            List<BitcoinNews> newsList = bitcoinNewsHolder.get();
            for (BitcoinNews newsHolder : newsList) {
                String content = newsHolder.getContent();
                content = removeHtmlTagsAndUrls(content);
                if (!content.isEmpty()) {
                    handleNewsSentiment(content, priceIncreased, newsHolder);
                }
            }
        }catch (Exception e) {
            log.error("Error: processData is failed! - {}", e.getMessage());
        }
    }

    private void handleNewsSentiment(String content, boolean priceIncreased, BitcoinNews newsHolder){
        try {
            int totalSentiment = 0;
            int weightedSentiment = 0;
            Annotation annotation = pipeline.process(content);
            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            int numOfSentences = sentences.size();
            int divisorWeighted = 0;

            for (int i = 0; i < numOfSentences; i++){
                Tree sentimentTree = sentences.get(i)
                        .get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
                int sentiment = RNNCoreAnnotations.getPredictedClass(sentimentTree);
                totalSentiment = totalSentiment + sentiment;
                if(i == 0 || i == numOfSentences -1) {
                    weightedSentiment += sentiment * numOfSentences;
                    divisorWeighted += numOfSentences;
                }
                else {
                    weightedSentiment += sentiment;
                    divisorWeighted += 1;
                }
            }
            if (divisorWeighted != 0) {
                double weightedAverageSentiment = (double) weightedSentiment / divisorWeighted;
                boolean isPositive = weightedAverageSentiment > 2;
                newsHolder.setPositive(isPositive);
                if ((priceIncreased && isPositive) || (!priceIncreased && !isPositive)){
                    newsHolder.setContent(content);
                }
            }
        } catch (Exception e){
            log.error("Error: Handle news sentiment is failed! - {}", e.getMessage());
        }
    }
    private String removeHtmlTagsAndUrls(String text) {
        String noHtml = text.replaceAll("<[^>]+>", "");
        return noHtml.replaceAll("http\\S+|www\\.\\S+", "");
    }

}

