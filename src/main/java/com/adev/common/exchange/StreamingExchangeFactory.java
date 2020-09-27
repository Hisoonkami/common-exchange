package com.adev.common.exchange;

import com.adev.common.exchange.exception.ExchangeException;

public enum StreamingExchangeFactory {
    INSTANCE;
    private StreamingExchangeFactory() {
    }

    public StreamingExchange createExchangeWithoutSpecification(String exchangeClassName) {
        try {
            Class exchangeProviderClass = Class.forName(exchangeClassName);
            if (StreamingExchange.class.isAssignableFrom(exchangeProviderClass)) {
                StreamingExchange exchange = (StreamingExchange)exchangeProviderClass.newInstance();
                return exchange;
            } else {
                throw new ExchangeException("Class '" + exchangeClassName + "' does not implement Exchange");
            }
        } catch (ClassNotFoundException var4) {
            throw new ExchangeException("Problem creating Exchange (class not found)", var4);
        } catch (InstantiationException var5) {
            throw new ExchangeException("Problem creating Exchange (instantiation)", var5);
        } catch (IllegalAccessException var6) {
            throw new ExchangeException("Problem creating Exchange (illegal access)", var6);
        }
    }

    public StreamingExchange createExchange(String exchangeClassName) {
        StreamingExchange exchange = this.createExchangeWithoutSpecification(exchangeClassName);
        return exchange;
    }
}
