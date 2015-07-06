#!/usr/bin/env python
import sys
try:
	count_file = sys.argv[1]
	gold_file  = sys.argv[2]
except:
	print("weight_gold.py count_file gold_file", sys.stderr)
	sys.exit(-1)

d = {}
skip = 0
for l in open(count_file):
	if skip < 3:
		skip += 1
		continue
	count = reduce(lambda x, y: x + y, map(lambda x: int(x), l.strip().split('\t')[1:]))
	key = l.split('\t')[0].split('/')[0]

	d[l.split('\t')[0].split('/')[0]] = count

# print(d)
for l in open(gold_file):
	print("%s\t%d" % (l.strip(), d[l.strip()]))