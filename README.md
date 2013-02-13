StockSimulator
==============

A Stock Simulator Analysis Application

Started during the fall of 2010 but never quite finished, I wanted to make a reasonably accurate prediction software for buying and selling mid to big cap stocks. The algorithm consistently gets over 80% success rate on average for big-mid cap stocks from running simulations using stock historical data during Jan 2001 – Aug 2012. This project was not completed due to the recent deprecation of Google Finance API and unreliability of the Yahoo Finance Stock quotes API, basically having no good open source way of getting stock information reliably. The basic concept is not hard – buy mid-big cap stable stocks, hold for very long periods, buy/sell based on market factors and not on stock specific factors, and have alot of cash reserves.

The application works as follows:

1) A cron job reaches out to the Google Finance API to download historical stock information in XML or JSON or CSV and store them in a directory.
2) The Java application runs, it takes in each CSV file (haven’t made it work with XML or JSON yet) and generates buying, selling, shorting and covering simulation runs for each stock and stores it in a separate folder with a summary of when it bought/sold, when you should buy/sell it, the support/resistance prices, the standard deviation of the price and volatility, total profit and loss for the simulation run, the return on investment and success rate for the simulation run, and some sample commission fees for the simulation run.
3) It also generates a summary file that lists stocks in order of return and predicting success for the simulation run, and computes the averages of each.

The stock algorithm first calculates the 30 day support and resistance prices, and a 10 day moving average (I should expand this to 30-50 day as an improvement). So whatever stock is being analyzed needs at least 30 days of history. It calculates the variance from 30 days, and the standard deviation from that, and determines whether its volatile or not.
Then, for each day:

if it detects a downtrend and holding a stock:

  -it sells if the stock has has reached or exceeded its sell limit or stop limit.

-else if it detects a uptrend:

  -if its a bear day (a bear day is when indices fall more than 0.8%) and it has no holdings of a stock:
    -then it buys the stock if there's an uptrend. If the stock is volatile, then sell price is 110% of the buying price, else sell price is 105% of the buying price. Stop limit is 75% of the buying price.
    -if the stocks sinks more than 95% of its average rolling price, sell price is 105% of the buying price. Stop limit is 75% of the buying price.

  -if it has holdings of a stock:
    -if its a bullday (which is when indices rise more than 0.4%):
       -it sells when the stock has reached or exceeded its sell limit or stop limit.

Then it recalculates the rolling averages for 10-days, computes if its a downtrend (down for more than 5 days consecutively), or uptrend (up for more than 5 days consecutively), recalculates support and resistance prices, and calculates the volatility, P&L, ROI, Success rate and Commission.

Future plans
If there is another good free Finance API I can use, then I would probably redo the algorithm to work with 30 or 50 day moving averages, tweak it more, make it ingest JSON/XML instead of CSV and use DB persistance rather than file persistance. Also reading from properties files is a lot nicer than hard coding magic numbers in the app. Anyways I’ve left it alone for now, but more automation would definitely be good. Adding more fundamentals into the equation (for example, buying only stocks with a high EPS and a P/E ratio between 10-25) would be nice. I envision the day where I can have it automatically update everyday, and have a friendly GUI where I can query for any stock and it tells me all the analytical details about the stock right away, with a cool graphical chart of the simulation. So I hope by open sourcing this project other people can help out.

-Tong Zou