package stocks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.*;

/* This application makes stock predictions based on historical trends
* @Author Tong Zou
* */

public class StockApp {

    private static final Double BUYING_POWER = 10000.00;
    //set to the directory where you keep the list of csv files generated from Google Finance or Yahoo Finance
    private static final String DIRECTORY = "/StockApp/stocklist";
    //default is sell when stock losses exceed 25%
    private static final Double STOP_LIMIT = 0.75;
    //default is sell when stock gains greater than 5%
    private static final Double SELL_LIMIT = 1.05;
    //In general, stocks with a P/E multiple of 10-25 is good. >25 = overvalued. <10 = losing earnings.
    //High EPS is good. As high as possible. EPS >0.
    //low EPS, high P/E = overvalued. High EPS, low P/E = undervalued.
    //Take the P/E and divide by EPS. The higher the number, the more overvalued. The lower the number, the more undervalued.
    //Date,Open
    private static HashMap<String, String> dowJonesList = new HashMap<String, String>();
    private static HashMap<String, String> nasdaqList = new HashMap<String, String>();
    private static HashMap<String, Double> dowJonesPer = new HashMap<String, Double>();
    private static HashMap<String, Double> nasdaqPer = new HashMap<String, Double>();
    private static HashMap<String, String> SPList = new HashMap<String, String>();
    private static HashMap<String, Double> SPPer = new HashMap<String, Double>();

    //Use query:http://finance.google.com/finance/historical?q=NASDAQ:LONG&startdate=Jun+1,2009&enddate=Jun+1,2010&output=csv
    //http://www.google.com/finance/historical?startdate=Jan+1%2C+2001&enddate=Nov+15%2C+2012&num=30&output=csv&q=
    //have a DB with different tables; each table represents a stock or index. then everyday, a cron job
    //runs that updates the table with the day's stock quotes (via RESTful JSON API), then this java file runs and parses the list of historical data
    //then it outputs the results to a file (or graphical/web output, if a GUI/web integration is added).
    public static void main(String args[]) {
        try {

            //look through dir
            File directory = new File(DIRECTORY);
            String fileNames[] = directory.list();
            PriorityQueue<Double> returnsList = new PriorityQueue<Double>();
            PriorityQueue<Double> successList = new PriorityQueue<Double>();
            Map<String, Double> stockReturnsList = new HashMap<String, Double>();
            Map<String, Double> stockPreList = new HashMap<String, Double>();

            //init indices
            calculateIndices();

            for (int x = 0; x < fileNames.length; x++) {

                if (fileNames[x].endsWith(".csv")) {

                    // Open the file that is the first
                    // command line parameter
                    FileInputStream fstream = new FileInputStream(DIRECTORY + "/" + fileNames[x]);

                    //create analysis file - this is called [STOCK_SYMBOL]-analysis.txt by default
                    //this implementation writes to a file in the analysis folder for each stock analysed,
                    //but this could be replaced by console output or web output, etc
                    FileWriter fstream_w = new FileWriter(DIRECTORY + "/analysis/" + fileNames[x] + "-analysis.txt");
                    BufferedWriter out = new BufferedWriter(fstream_w);

                    //Skip indices
                    if (fileNames[x].equals("DJIA.csv") || fileNames[x].equals("NASDAQ.csv") || fileNames[x].equals("SP500.csv") ||
                        !fileNames[x].contains(".csv")) {
                        continue;
                    }

                    // Get the object of DataInputStream
                    DataInputStream in = new DataInputStream(fstream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    br.readLine();
                    String strLine;
                    String stock = fileNames[x].substring(0, fileNames[x].indexOf("."));
                    out.write("-------------------------------------");
                    out.newLine();
                    out.write("Analyzing predictions for " + stock + ": ");
                    out.newLine();
                    out.write("-------------------------------------");
                    out.newLine();
                    ArrayList<String> priceList = new ArrayList<String>();
                    ArrayList<String> dateList = new ArrayList<String>();
                    //Read File Line By Line
                    while ((strLine = br.readLine()) != null) {
                        String[] str = strLine.split(",");
                        String date = str[0];
                        //Weird on Aug 9, 2011, stock prices were 0??
                        if (!date.equals("9-Aug-11")) {
                            dateList.add(date);
                            //buy at the open
                            priceList.add(str[1]);
                        }
                    }

                    Collections.reverse(priceList);
                    Collections.reverse(dateList);

                    Double avg = 0.0;
                    Double buyPrice = 0.0;
                    Double support = Double.parseDouble(priceList.get(0));
                    Double resistance = 0.00;
                    Double sellLim = 0.0;
                    Double stopLim = 0.0;
                    Double profit = 0.0;
                    Double total = 0.0;
                    Integer numShares = 0;
                    Boolean bought = false;
                    Boolean sold = false;
                    Boolean shortSell = false;
                    Boolean enableShortSell = false;
                    Double nextPrice = 0.0;
                    Integer trades = 0;
                    Integer success = 0;
                    //this is a sample commission. Set it to whatever your brokerage uses.
                    Double commission = 9.95;
                    Double variance = 0.0;
                    Double varAvg = 0.0;
                    Double stdDev = 0.0;
                    Double volatily = 0.0;
                    Integer trendCount = 0;
                    Double prevAvg = 0.0;
                    Boolean downTrend = false;
                    Boolean upTrend = false;
                    ArrayQueue<Double> avgList = new ArrayQueue<Double>();

                    //Calculate Support (lowest of 30 day avg)
                    //Calculate Resistance (highest of 30 day avg)
                    //Calculate 10-day moving average
                    for (int i = 0; i < 30; i++) {
                        Double tempInt = Double.parseDouble(priceList.get(i));
                        varAvg += tempInt;

                        if (i < 10) {
                            support = Math.min(support, tempInt);
                            resistance = Math.max(resistance, tempInt);
                            avg += tempInt;
                        }
                    }

                    avg = avg / 10;
                    varAvg = varAvg / 30;

                    //calc variance for 30 days
                    for (int i = 0; i < 30; i++) {
                        Double tempInt = Double.parseDouble(priceList.get(i));
                        variance += Math.pow((tempInt - varAvg), 2);
                    }

                    stdDev = Math.sqrt(variance / 29);
                    volatily = stdDev / avg;

                    //populate avgList
                    for (int i = 0; i < 5; i++) {
                        avgList.enqueue(avg);
                    }

                    //Main loop
                    for (int i = 10; i < priceList.size(); i++) {
                        nextPrice = Double.parseDouble(priceList.get(i));

                        //if it detects a downtrend
                        if (downTrend) {
                            //stock is bearish
                            Boolean cond = !bought && nextPrice <= support;
                            if (volatily > 0.2) {
                                cond = !bought;
                            }

                            //if short sell is enabled
                            //disabled this whole section for now. Short selling is complicated.
                            if (cond && enableShortSell) {
                                shortSell = true;
                                buyPrice = nextPrice;
                                bought = true;
                                sold = false;
                                sellLim = buyPrice * 1.1;
                                stopLim = buyPrice * 0.95;
                                out.write(dateList.get(i) + ": Shorting " + numShares + " Shares at: " + nextPrice);
                                out.newLine();
                                trades++;
                            } else if (bought && !sold && enableShortSell) {
                                if (shortSell) {
                                    if (nextPrice >= sellLim) {
                                        profit = (numShares * buyPrice - numShares * nextPrice);
                                        total += profit;
                                        sold = true;
                                        bought = false;
                                        shortSell = false;
                                        out.write(dateList.get(i) + ": Covered " + numShares + " Shares at: " + nextPrice);
                                        out.newLine();
                                        trades++;
                                    } else if (nextPrice <= stopLim) {
                                        profit = (numShares * buyPrice - numShares * nextPrice);
                                        total += profit;
                                        sold = true;
                                        bought = false;
                                        shortSell = false;
                                        out.write(dateList.get(i) + ": Covered " + numShares + " Shares at: " + nextPrice);
                                        out.newLine();
                                        trades++;
                                        success++;
                                    }
                                } else {
                                    //if stock is above the sell limit
                                    if (nextPrice >= sellLim) {
                                        profit = (numShares * nextPrice - numShares * buyPrice);
                                        total += profit;
                                        sold = true;
                                        bought = false;
                                        out.write(dateList.get(i) + ": Sold " + numShares + " Shares at: " + nextPrice);
                                        out.newLine();
                                        trades++;
                                        success++;
                                    }
                                    //if stock falls below the stop limit, sell.
                                    else if (nextPrice <= stopLim) {
                                        profit = (numShares * nextPrice - numShares * buyPrice);
                                        total += profit;
                                        sold = true;
                                        bought = false;
                                        out.write(dateList.get(i) + ": Sold " + numShares + " Shares at: " + nextPrice);
                                        out.newLine();
                                        trades++;
                                    }
                                }
                            }
                        } else {
                            //this is when it buys
                            //change to 0.95 to whatever your buy point is
                            if (!bought) {
                                String date = dateList.get(i);
                                Double index1 = SPPer.get(date);
                                Double index2 = dowJonesPer.get(date);
                                Double index3 = nasdaqPer.get(date);
                                Boolean bearDay = false;
                                if (index1 != null && index2 != null && index3 != null) {
                                    bearDay = index1 <= -0.8 && index2 <= -0.8 && index3 <= -0.8;
                                }
                                Boolean buyCond = upTrend && nextPrice <= avg;
                                if (volatily > 0.2) {
                                    buyCond = upTrend;
                                }
                                //stock is bullish
                                if (buyCond && bearDay) {
                                    buyPrice = nextPrice;
                                    Double num = BUYING_POWER / nextPrice;
                                    numShares = new Integer(num.intValue());
                                    bought = true;
                                    sold = false;
                                    if (volatily >= 0.3) {
                                        sellLim = buyPrice * 1.1;
                                    } else {
                                        sellLim = buyPrice * SELL_LIMIT;
                                    }
                                    stopLim = buyPrice * STOP_LIMIT;
                                    out.write(dateList.get(i) + ": Bought " + numShares + " Shares at: " + buyPrice);
                                    out.newLine();
                                    trades++;
                                    //stock is neutral
                                } else if (nextPrice <= avg * 0.95 && bearDay) {
                                    buyPrice = nextPrice;
                                    Double num = BUYING_POWER / nextPrice;
                                    numShares = new Integer(num.intValue());
                                    bought = true;
                                    sold = false;
                                    sellLim = buyPrice * SELL_LIMIT;
                                    stopLim = buyPrice * STOP_LIMIT;
                                    out.write(dateList.get(i) + ": Bought " + numShares + " Shares at: " + buyPrice);
                                    out.newLine();
                                    trades++;
                                }
                            } else if (bought && !sold) {
                                if (shortSell) {
                                    if (nextPrice >= sellLim) {
                                        profit = (numShares * buyPrice - numShares * nextPrice);
                                        total += profit;
                                        sold = true;
                                        bought = false;
                                        shortSell = false;
                                        out.write(dateList.get(i) + ": Covered " + numShares + " Shares at: " + nextPrice);
                                        out.newLine();
                                        trades++;
                                    } else if (nextPrice <= stopLim) {
                                        profit = (numShares * buyPrice - numShares * nextPrice);
                                        total += profit;
                                        sold = true;
                                        bought = false;
                                        shortSell = false;
                                        out.write(dateList.get(i) + ": Covered " + numShares + " Shares at: " + nextPrice);
                                        out.newLine();
                                        trades++;
                                        success++;
                                    }
                                } else {
                                    String date = dateList.get(i);
                                    Double index1 = SPPer.get(date);
                                    Double index2 = dowJonesPer.get(date);
                                    Double index3 = nasdaqPer.get(date);
                                    //use this variable for higher returns but less success rate
                                    Boolean bullDay = false;
                                    if (index1 != null && index2 != null && index3 != null) {
                                        bullDay = index1 > 0.4 && index2 > 0.4 && index3 > 0.4;
                                    }

                                    //if stock is above the sell limit
                                    if (nextPrice >= sellLim && bullDay) {
                                        profit = (numShares * nextPrice - numShares * buyPrice);
                                        total += profit;
                                        sold = true;
                                        bought = false;
                                        out.write(dateList.get(i) + ": Sold " + numShares + " Shares at: " + nextPrice);
                                        out.newLine();
                                        trades++;
                                        success++;
                                    }

                                    //if stock falls below the stop limit and its a bull day (very likely something bad about the stock), sell.
                                    else if (nextPrice <= stopLim && bullDay) {
                                        profit = (numShares * nextPrice - numShares * buyPrice);
                                        total += profit;
                                        sold = true;
                                        bought = false;
                                        out.write(dateList.get(i) + ": Sold " + numShares + " Shares at: " + nextPrice);
                                        out.newLine();
                                        trades++;
                                    }
                                }
                            }
                        }

                        //recompute 10-day rolling average
                        prevAvg = avg;
                        avg = avg * 10;
                        avg -= Double.parseDouble(priceList.get(i - 10));
                        avg += nextPrice;
                        avg = avg / 10;
                        avgList.dequeue();
                        avgList.enqueue(avg);

                        //Detect trends
                        if (avg < prevAvg) {
                            upTrend = false;
                            if (trendCount < 10) {
                                trendCount++;
                            }
                        } else if (avg > prevAvg) {
                            downTrend = false;
                            if (trendCount > 0) {
                                trendCount--;
                            }
                        }

                        Integer isDown = 0;
                        Integer isUp = 0;
                        for (int j = 0; j < avgList.size() - 1; j++) {
                            if (avgList.peek(j) <= avgList.peek(j + 1)) {
                                isUp++;
                            } else if (avgList.peek(j) >= avgList.peek(j + 1)) {
                                isDown++;
                            }
                        }

                        //if stock goes down for 5 days consecutively, its on a downtrend
                        if (isDown > 3) {
                            downTrend = true;
                            //    System.out.println("Downtrend. ");
                        }

                        //if a stock goes up for 5 days consecutively, its on an uptrend
                        else if (isUp > 3) {
                            upTrend = true;
                            //    System.out.println("Uptrend. ");
                        }

                        //recalculate support + resistance + variance for 30-day
                        support = Double.parseDouble(priceList.get(i - 10));
                        resistance = Double.parseDouble(priceList.get(i - 10));

                        if (i > 30) {
                            variance -= Math.pow((Double.parseDouble(priceList.get(i - 1 - 30)) - varAvg), 2);
                            varAvg = 0.0;
                            for (int j = 0; j < 30; j++) {
                                Double tempInt = Double.parseDouble(priceList.get(i - (30 - j)));
                                varAvg += tempInt;
                            }

                            varAvg = varAvg / 30;
                            variance += Math.pow(nextPrice - varAvg, 2);
                            stdDev = Math.sqrt(variance / 29);
                            volatily = stdDev / avg;
                        }

                        for (int j = 0; j < 10; j++) {
                            Double tempInt = Double.parseDouble(priceList.get(i - (10 - j)));
                            support = Math.min(support, tempInt);
                            resistance = Math.max(resistance, tempInt);
                        }
                    }

                    if (downTrend) {
                        out.write("Stock is on a downtrend. Do not buy it. Expect to short it/sell it. ");
                        out.newLine();
                    } else if (upTrend) {
                        out.write("Stock is on an uptrend. Expect to buy it around $" + avg);
                        out.newLine();
                        out.write(" when S&P 500 index <= " + (Double.parseDouble(SPList.get(dateList.get(dateList.size() - 1))) * 0.992) +
                            ",");
                        out.newLine();
/*                        out.write(" when DJIA index <= " +
                            (Double.parseDouble(dowJonesList.get(dateList.get(dateList.size() - 2))) * 0.992) + ",");
                        out.newLine();*/
/*                        out.write(" when NASDAQ index <= " +
                            (Double.parseDouble(nasdaqList.get(dateList.get(dateList.size() - 2))) * 0.992) + ".");
                        out.newLine();*/
                    } else {
                        out.write("Stock is neutral. Buy in around $" + avg * 0.95);
                        out.newLine();
                        out.write(" when S&P 500 index <= " + (Double.parseDouble(SPList.get(dateList.get(dateList.size() - 1))) * 0.992) +
                            ",");
                        out.newLine();
/*                        out.write(" when DJIA index <= " +
                            (Double.parseDouble(dowJonesList.get(dateList.get(dateList.size() - 2))) * 0.992) + ",");
                        out.newLine();*/
/*                        out.write(" when NASDAQ index <= " +
                            (Double.parseDouble(nasdaqList.get(dateList.get(dateList.size() - 2))) * 0.992) + ".");
                        out.newLine();*/
                    }

                    if (!sold && bought) {
                        if (shortSell) {
                            out.write("Next cover price is " + stopLim);
                            out.newLine();
                        } else {
                            out.write("Sell it at $" + stopLim + " or below, or $" + sellLim + " or above ");
                            out.newLine();
                            out.write(" when S&P 500 index >= " +
                                (Double.parseDouble(SPList.get(dateList.get(dateList.size() - 1))) * 0.992) +
                                ",");
                            out.newLine();
/*                            out.write(" when DJIA index >= " +
                                (Double.parseDouble(dowJonesList.get(dateList.get(dateList.size() - 2))) * 0.992) + ",");
                            out.newLine();*/
                          /*  out.write(" when NASDAQ index >= " +
                                (Double.parseDouble(nasdaqList.get(dateList.get(dateList.size() - 2))) * 0.992) + ".");
                            out.newLine();*/
                        }
                    }
                    out.write("Support price is " + support);
                    out.newLine();
                    out.write("Resistance price is " + resistance);
                    out.newLine();
                    out.write("Std Dev = " + stdDev + ".");
                    out.newLine();
                    if (volatily <= 0.1) {
                        out.write("The stock is not volatile.");
                        out.newLine();
                    } else if (volatily > 0.1 && volatily < 0.2) {
                        out.write("The stock is moderately volatile.");
                        out.newLine();
                    } else {
                        out.write("The stock  is highly volatile.");
                        out.newLine();
                    }

                    out.write("Total P&L for this stock is $" + total + ".");
                    out.newLine();
                    out.write("Return on Investment is " + total / 100.00 + "%");
                    out.newLine();
                    returnsList.add(total / 100.00);
                    stockReturnsList.put(stock, total / 100.00);

                    if (trades > 0) {
                        Double num = (double) success.intValue() / ((double) trades.intValue() / 2);
                        numShares = new Integer(num.intValue());
                        successList.add(num * 100);
                        stockPreList.put(stock, num * 100);
                        out.write("Success rate was " + num * 100 + "%.");
                        out.newLine();
                    }
                    out.write("Commission was $" + commission * trades + " for " + trades + " trades.");
                    out.newLine();
                    br.close();
                    out.close();
                }
            }

            //This part writes the summary to a file called SUMMARY.txt
            FileWriter fstream_w = new FileWriter(DIRECTORY + "/analysis/" + "SUMMARY.txt");
            BufferedWriter out = new BufferedWriter(fstream_w);
            out.write("----------------------------------------------");
            out.newLine();
            out.write("Top stocks in order of returns were: ");
            out.newLine();
            Double avgReturns = 0.0;
            Double avgPredict = 0.0;
            for (Double ret : returnsList) {
                //gets the first stock that has this return
                String stockName = getKeyByValue(stockReturnsList, ret);
                out.write(stockName + " " + ret + "%.");
                out.newLine();
                avgReturns += ret;
                stockReturnsList.remove(stockName);
            }
            out.write("----------------------------------------------");
            out.newLine();
            out.write("Top stocks in order of predicting success were: ");
            out.newLine();
            for (Double pre : successList) {
                //gets the first stock that has this prediction
                String stockName = getKeyByValue(stockPreList, pre);
                out.write(stockName + " " + pre + "%.");
                out.newLine();
                avgPredict += pre;
                stockPreList.remove(stockName);
            }

            out.write("----------------------------------------------");
            out.newLine();
            avgReturns = avgReturns / (returnsList.size());
            avgPredict = avgPredict / (successList.size());

            out.write("Average returns across all stocks is " + avgReturns + "%.");
            out.newLine();
            out.write("Average prediction rate across all stocks is " + avgPredict + "%.");
            out.newLine();
            out.close();
        } catch (Exception e) {//Catch exception if any
            e.printStackTrace();
            System.err.println("Error: " + e.getStackTrace() + e.getCause());
        }
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void calculateIndices() {

        try {

            String[] indices = new String[]{DIRECTORY + "/DJIA.csv", DIRECTORY + "/NASDAQ.csv", DIRECTORY + "/SP500.csv"};

            for (int x = 0; x < indices.length; x++) {
                // Open the file that is the first
                // command line parameter
                FileInputStream fstream = new FileInputStream(indices[x]);

                // Get the object of DataInputStream
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                br.readLine();
                String strLine;
                String index = indices[x].substring(0, indices[x].indexOf("."));
                System.out.println("-------------------------------------");
                System.out.println("Analyzing " + index + ": ");
                System.out.println("-------------------------------------");
                String date = "";

                //Read File Line By Line
                while ((strLine = br.readLine()) != null) {
                    String[] str = strLine.split(",");
                    String[] str2 = strLine.split("\"");
                    String open = str2[1].replaceAll("[,]", "");
                    //Weird on Aug 9, 2011, stock prices were 0??
                    if (!str[0].equals("9-Aug-11")) {

                        if (index.contains("DJIA")) {
                            if (!dowJonesList.isEmpty()) {
                                double num1 = Double.parseDouble(dowJonesList.get(date));
                                double num2 = Double.parseDouble(open);
                                Double percentDiff = ((num2 - num1) / num2) * 100;
                                dowJonesPer.put(str[0], percentDiff);
                            }

                            date = str[0];
                            dowJonesList.put(date, open);
                        } else if (index.contains("NASDAQ")) {
                            if (!nasdaqList.isEmpty()) {
                                double num1 = Double.parseDouble(nasdaqList.get(date));
                                double num2 = Double.parseDouble(open);
                                Double percentDiff = ((num2 - num1) / num2) * 100;
                                nasdaqPer.put(str[0], percentDiff);
                            }

                            date = str[0];
                            nasdaqList.put(date, open);
                        } else if (index.contains("SP500")) {
                            if (!SPList.isEmpty()) {
                                double num1 = Double.parseDouble(SPList.get(date));
                                double num2 = Double.parseDouble(open);
                                Double percentDiff = ((num2 - num1) / num2) * 100;
                                SPPer.put(str[0], percentDiff);
                            }
                            date = str[0];
                            SPList.put(date, open);
                        }
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}