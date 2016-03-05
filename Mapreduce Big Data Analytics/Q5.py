import sys


file1=open("q5")
title_Twitter = file1.readline().split('\n')[0]
viewNum1 = [0 for i in range(31)]
title_Apple = file1.readline().split('\n')[0]
viewNum2 = [0 for i in range(31)]
file1.close()
for line in sys.stdin:
        if not line:
            break
        array = line.split('\t')
        if array[1]==title_Twitter:
            for i in range(2, 33):
                viewNum1[i-2] = int(array[i].split(':')[1])
        if array[1] == title_Apple:
            for i in range(2,33):
                viewNum2[i-2] = int(array[i].split(':')[1])
res=0
for i in range(31):
    if viewNum1[i] > viewNum2[i]:
        res += 1
print '%s' % (res)

