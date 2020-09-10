perf stat -d java -jar ../dist/BFT-Hibrid_with_status-executable.jar 6 10 1000 0 0 10000000 150
perf stat -d java -jar ../dist/BFT-smart-parallel-executable.jar 1000 10 6  0 0 10000000 false 150
perf stat -d java -jar ../dist/BFT-smart-parallel-executable.jar 1000 10 6  0 0 10000000 true 150
perf stat -d java -XX:-RestrictContended -jar ../dist/BFT-Hibrid_contended-executable.jar  6 10 1000 0 0 10000000 150
perf stat -d java -jar ../dist/BFT-Hibrid_contended-executable.jar  6 10 1000 0 0 10000000 150
perf stat -d java -jar ../dist/BFT-Hibrid_padding-executable.jar  6 10 1000 0 0 10000000 150
