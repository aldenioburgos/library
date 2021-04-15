#!/bin/bash

alias l ll
alias g git status
alias ga git add
alias gc git commit -a
alias gp git push https://aldenioburgos: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@github.com/aldenioburgos/library.git

nodes=(0 1 2 3)
for n in {0..3} ; do
  alias r${n} ssh replica${n}
  alias c${n} ssh cliente${n}
done;

echo 'installed aliases'

