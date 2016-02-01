import sys

length=1
number=1

for line in sys.stdin:
        if not line:
            break
        array = line.split('\t')
        consecutive=1
        tmp=1
        for i in range(3,33):
            if(int(array[i].split(':')[1]) < int(array[i-1].split(':')[1])):
                tmp += 1
            else:
                tmp = 1
            consecutive = max(consecutive,tmp)
        if length == consecutive:
            number += 1
        elif consecutive> length:
            number = 1
            length = consecutive
print(number)

