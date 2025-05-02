#!/usr/bin/env bash
set -euo pipefail

# --- 配置区 ---
machines=(apt068 apt071 apt069)
user="Ziqif"
domain="apt.emulab.net"
LOGFILE="/var/log/fdb_copy_time.log"

# --- 恢复流程 ---
for host in "${machines[@]}"; do
  fqdn="${user}@${host}.${domain}"
  echo "------ RESTORE on ${fqdn} ------"

  ssh "${fqdn}" bash -l -c "'
    sudo touch \"$LOGFILE\" && sudo chown ${user}:\"$LOGFILE\"
    echo -e \"\n=== \$(date \"+%Y-%m-%d %H:%M:%S\") on \$(hostname) ===\" | tee -a \"$LOGFILE\"

    echo \"Stopping FoundationDB service...\" | tee -a \"$LOGFILE\"
    sudo service foundationdb stop

    echo \"Removing old data directory...\" | tee -a \"$LOGFILE\"
    sudo rm -rf /var/lib/foundationdb/data/

    echo \"\$(date '+%Y-%m-%d %H:%M:%S') START restore copy\" | tee -a \"$LOGFILE\"
    t_start=\$(date +%s)
    sudo cp -r /var/lib/foundationdb/copy/ /var/lib/foundationdb/data/
    t_end=\$(date +%s)
    echo \"\$(date '+%Y-%m-%d %H:%M:%S') END   restore copy, duration: \$((t_end-t_start))s\" | tee -a \"$LOGFILE\"

    echo \"Starting FoundationDB service...\" | tee -a \"$LOGFILE\"
    sudo service foundationdb start

    echo \"Restore finished on \$(hostname)!\" | tee -a \"$LOGFILE\"
  '"

  echo
done

echo "All hosts restored."
