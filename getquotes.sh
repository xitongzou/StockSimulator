#!/bin/sh
while read line
do
  wget -O list/${line:54}.csv $line;
done < stockfiles.txt
exit 0
