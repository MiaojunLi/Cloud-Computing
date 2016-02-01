import sys

file1=open("q7")

record = {}
while 1:
    line = file1.readline()
    if not line:
        break
    record[line[0:-1]] = 0
file1.close()

for line in sys.stdin:
        if not line:
            break
        array = line.split('\t')
        if array[1] in record.keys():
            record[array[1]] = array[0]
for key in record.keys():
    print(key + '\t' + record[key])