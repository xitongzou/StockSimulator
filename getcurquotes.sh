#!/bin/sh
month=`date +%b`
day=`date +%e`
year=`date +%Y`
while read line
do
  wget -O list/$line.csv "http://www.google.com/finance/historical?startdate=Jan+1%%2C+2001&enddate=$month+$day%%2C+$year&num=30&output=csv&q=$line";
done < stocks.txt
exit 0
