package btc.analyzer.holder;

import btc.analyzer.data.BitcoinNews;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BitcoinNewsHolder {
    List<BitcoinNews> btcNewsHolder = new ArrayList<>();

    public void add(BitcoinNews news){
        btcNewsHolder.add(news);
    }

    public void clear(){
        btcNewsHolder.clear();
    }

    public List<BitcoinNews> get(){
        return btcNewsHolder;
    }

}
