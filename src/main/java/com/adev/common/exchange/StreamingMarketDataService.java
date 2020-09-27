package com.adev.common.exchange;

import com.adev.common.base.domian.Kline;
import com.adev.common.base.domian.OrderBook;
import com.adev.common.base.domian.Ticker;
import com.adev.common.base.domian.Trade;
import io.reactivex.Observable;

public interface StreamingMarketDataService {
    /**
     * Get an order book representing the current offered exchange rates (market depth).
     *
     * @param currencyPair Currency pair of the order book
     * @return {@link Observable} that emits {@link OrderBook} when exchange sends the update.
     */
    Observable<OrderBook> getOrderBook(String currencyPair, Object... args);

    /**
     * Get a ticker representing the current exchange rate.
     *
     * @param currencyPair Currency pair of the ticker
     * @return {@link Observable} that emits {@link Ticker} when exchange sends the update.
     */
    Observable<Ticker> getTicker(String currencyPair, Object... args);

    /**
     * Get the trades performed by the exchange.
     *
     * @param currencyPair Currency pair of the trades
     * @return {@link Observable} that emits {@link Trade} when exchange sends the update.
     */
    Observable<Trade> getTrades(String currencyPair, Object... args);

    Observable<Kline> getCurrentKLine(String currencyPair, Object... args);

    Observable<Kline> getHistoryKLine(String currencyPair, Object... args);
}
