#!/bin/bash

alias l ll
alias g "git status"
alias ga "git add"
alias gc "git commit -a"
alias run "./run_hybrid.sh >& log & ; tail -f log"
alias top "top -ualdenio"
alias stop "pkill run_hybrid"
alias kill "./kill_hybrid.sh"

nodes=(0 1 2 3)
for n in {0..3} ; do
  alias r${n} "ssh replica${n}"
  alias c${n} "ssh cliente${n}"
done;

echo 'installed aliases'

