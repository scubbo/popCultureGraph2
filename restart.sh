#!/bin/bash
if ps aux | grep popculturegraph | grep -v grep > /dev/null
then
  : # Do nothing - http://unix.stackexchange.com/a/133976/30828
else
  mv "/var/popCultureGraph/logfile" /var/popCultureGraph/logs/logfile_$(date +%F-%T) >/dev/null 2>&1
  for file in /var/popCultureGraph/hs_err_*; do
    if [ -e "$file" ]; then
      mv "$file" "/var/popCultureGraph/logs/";
    fi
  done
  cd /var/popCultureGraph
  export PATH=$PATH:/opt/gradle/bin
  /var/popCultureGraph/startup.sh >>/var/popCultureGraph/logfile 2>&1 &
fi
