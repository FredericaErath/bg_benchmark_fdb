#!/bin/bash

# 默认参数（可通过命令行传入覆盖）
WORKLOAD="workloads/SymmetricVeryLowUpdateActions"
POPULATE_WORKLOAD="workloads/populateDB_1000"
LATENCY="0.1"
PERC="99"
STALENESS="0.01"
DURATION="60"
DIRECTORY="exp_socialites_very_low_1000"
MINIMUM="50"
OBJECTIVE="socialites"
VALIDATION="true"

# 解析命令行参数（如果需要支持更多参数可以继续加）
while [[ $# -gt 0 ]]; do
  case $1 in
    -workload) WORKLOAD="$2"; shift ;;
    -populateWorkload) POPULATE_WORKLOAD="$2"; shift ;;
    -latency) LATENCY="$2"; shift ;;
    -perc) PERC="$2"; shift ;;
    -staleness) STALENESS="$2"; shift ;;
    -duration) DURATION="$2"; shift ;;
    -directory) DIRECTORY="$2"; shift ;;
    -minimum) MINIMUM="$2"; shift ;;
    -objective) OBJECTIVE="$2"; shift ;;
    -validation) VALIDATION="$2"; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
  shift
done

# Step 1
echo "Running Step 1: JanusGraphBGCoord..."
java -cp "build/classes:lib/*" edu.usc.bg.JanusGraphBGCoord \
  -workload "$WORKLOAD" \
  -populateWorkload "$POPULATE_WORKLOAD" \
  -latency "$LATENCY" \
  -perc "$PERC" \
  -staleness "$STALENESS" \
  -duration "$DURATION" \
  -directory "$DIRECTORY" \
  -minimum "$MINIMUM" \
  -objective "$OBJECTIVE" \
  -validation "$VALIDATION"

# Step 2
echo "Running Step 2: LogRetryStatsFromDir..."
java -cp "build/classes:lib/*" scripts.LogRetryStatsFromDir "$DIRECTORY"

# Step 3
echo "Running Step 3: extract_rating_statistics.sh..."
# 传入 directory 参数给 extract_rating_statistics.sh
./extract_rating_statistics.sh "$DIRECTORY"
