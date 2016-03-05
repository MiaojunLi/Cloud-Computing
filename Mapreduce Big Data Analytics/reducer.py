#!/usr/bin/env python
import os
import string
import sys
# file1 = open('12.txt')
# (filepath,filename)=os.path.split(file1.name)
# print os.path.splitext(filename)[0].split('-')[1]

# date=os.environ["mapreduce_map_input_file"].split('-')[1]

currentword = None
currentTotalCount = 0
array = None
memo = [0 for i in range(0, 31)]
date = None
# while 1:
for line in sys.stdin:
    if not line:
        break
    array = line.split('\t')
    if array[0] == currentword:
        currentword = array[0]
        date = array[1].split('^^')[1]
        viewNum = string.atoi(array[1].split('^^')[0])
        currentTotalCount += viewNum
        memo[int(date[6:8])-1] += int(viewNum)

    else:
        if currentword is not None:
            if currentTotalCount > 100000:
                # print '%s\t%s' % (currentTotalCount, currentword),
                result= str(currentTotalCount) + '\t' + currentword
                for i in range(1,32):
                    if i < 10:
                        result += '\t2015120' + str(i) + ':' + str(memo[i-1])
                    else:
                        result += '\t201512' + str(i) + ':' + str(memo[i-1])
                print result
        memo = [0 for i in range(31)]

        currentword = array[0]
        date = array[1].split('^^')[1]
        viewNum = string.atoi(array[1].split('^^')[0])
        currentTotalCount = viewNum
        memo[int(date[6:8])-1] += int(viewNum)


if currentTotalCount > 100000:
    result = str(currentTotalCount) + '\t' + currentword
    for i in range(1, 32):
        if i < 10:
            result += '\t2015120' + str(i) + ':' + str(memo[i-1])
        else:
            result += '\t201512' + str(i) + ':' + str(memo[i-1])
    print result
