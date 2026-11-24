package com.easetrading.api.instrument;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentCsvTest {

    private static final String CSV = """
        exchange,exchange_token,trading_symbol,groww_symbol,name,instrument_type,segment,series,isin,underlying_symbol,underlying_exchange_token,lot_size,expiry_date,strike_price,tick_size,freeze_quantity,is_reserved,buy_allowed,sell_allowed
        NSE,2885,RELIANCE,NSE-RELIANCE,"Reliance Industries, Ltd",EQ,CASH,EQ,INE002A01018,,,1,,,0.1,,0,1,1
        BSE,500325,RELIANCE,BSE-RELIANCE,Reliance Industries Ltd,EQ,CASH,A,INE002A01018,,,1,,,0.05,,0,1,1
        NSE,NIFTY,NIFTY,NSE-NIFTY,Nifty 50,IDX,CASH,,,,,1,,,0.05,,0,0,0
        NSE,35001,BANKNIFTY25DEC27000PE,NSE-BANKNIFTY-24Dec25-27000-PE,BANKNIFTY,PE,FNO,,,BANKNIFTY,26009,35,2025-12-24,27000,0.05,,0,1,1
        NSE,11536,TCS-EQ,NSE-TCS,Tata Consultancy Services,EQ,CASH,EQ,INE467B01029,,,1,,,0.1,,0,1,1
        """;

    @Test
    void keepsOnlyNseCashEquities() throws Exception {
        List<Instrument> rows = InstrumentCsv.parseNseEquities(new BufferedReader(new StringReader(CSV)));
        assertEquals(2, rows.size());

        Instrument rel = rows.get(0);
        assertEquals("2885", rel.getToken());
        assertEquals("RELIANCE", rel.getSymbol());
        assertEquals("Reliance Industries, Ltd", rel.getName()); // quoted comma handled
        assertEquals("NSE-RELIANCE", rel.getGrowwSymbol());
        assertEquals("INE002A01018", rel.getIsin());
        assertEquals(0.1, rel.getTickSize());
    }

    @Test
    void displaySymbolDropsEqSuffixButTradingSymbolKeepsIt() throws Exception {
        Instrument tcs = InstrumentCsv.parseNseEquities(new BufferedReader(new StringReader(CSV))).get(1);
        assertEquals("TCS", tcs.getSymbol());
        assertEquals("TCS-EQ", tcs.getTradingSymbol());
    }
}
