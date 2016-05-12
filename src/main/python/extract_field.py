#!/usr/bin/env python
import sys
for l in open(sys.argv[1]):
    print(l.split('\t')[0].strip())
