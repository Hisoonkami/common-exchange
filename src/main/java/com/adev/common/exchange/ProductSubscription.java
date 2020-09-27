package com.adev.common.exchange;

import java.util.ArrayList;
import java.util.List;

/**
 * Use to specify subscriptions during the connect phase
 * For instancing, use builder @link {@link ProductSubscriptionBuilder}
 */
public class ProductSubscription {
    private List<String> orderBook;
    private List<String> trades;
    private List<String> ticker;
    private String currentCollect;

    private ProductSubscription(ProductSubscriptionBuilder builder) {
        this.orderBook = builder.orderBook;
        this.trades = builder.trades;
        this.ticker = builder.ticker;
        this.currentCollect = builder.currentCollect;
    }

    public List<String> getOrderBook() {
        return orderBook;
    }

    public List<String> getTrades() {
        return trades;
    }

    public List<String> getTicker() {
        return ticker;
    }
    
    public String getCurrentCollect() {
        return currentCollect;
    }

    public static ProductSubscriptionBuilder create() {
        return new ProductSubscriptionBuilder();
    }

    public static class ProductSubscriptionBuilder {
        private List<String> orderBook;
        private List<String> trades;
        private List<String> ticker;
        private String currentCollect;

        private ProductSubscriptionBuilder() {
            orderBook = new ArrayList<>();
            trades = new ArrayList<>();
            ticker = new ArrayList<>();
        }

        public ProductSubscriptionBuilder addOrderbook(String pair) {
            orderBook.add(pair);
            return this;
        }

        public ProductSubscriptionBuilder addTrades(String pair) {
            trades.add(pair);
            return this;
        }

        public ProductSubscriptionBuilder addTicker(String pair) {
            ticker.add(pair);
            return this;
        }
        
        public ProductSubscriptionBuilder setCurrentCollect(String currentCollect) {
        	this.currentCollect = currentCollect;
            return this;
        }

        public ProductSubscriptionBuilder addAll(String pair) {
            orderBook.add(pair);
            trades.add(pair);
            ticker.add(pair);
            return this;
        }

        public ProductSubscription build() {
            return new ProductSubscription(this);
        }
    }
}
