package btc.analyzer.holder;

import btc.analyzer.data.BitcoinPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BitcoinPriceHolder {

    private final List<BitcoinPrice> btcHolder = new ArrayList<>();


    public void add(BitcoinPrice price){
        btcHolder.add(price);
    }

    public List<BitcoinPrice> get(){
        return btcHolder;
    }

    public void clear(){
        btcHolder.clear();
    }

    public double startPrice(){
        return btcHolder.get(0).getPrice();
    }

    public double endPrice() {
        return btcHolder.get(btcHolder.size() - 1).getPrice();
    }
}
