import sys

title_Twitter = "Twitter"
viewNum1 = [0 for i in range(31)]
title_Apple = "Apple_Inc."
viewNum2 = [0 for i in range(31)]
for line in sys.stdin:
        if not line:
            break
        array = line.split('\t')
        if array[1]==title_Twitter:
            for i in range(2,33):
                viewNum1[i-2] = int(array[i].split(':')[1])
        if array[1]==title_Apple:
            for i in range(2,33):
                viewNum2[i-2] = int(array[i].split(':')[1])
res=0
for i in  range(31):
    if viewNum1[i]>viewNum2[i]:
        res+=1
print '%s' % (res)