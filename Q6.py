import sys

file1=open("q6")

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
            for i in range(2, 33):
                record[array[1]] = max(record[array[1]], int(array[i].split(':')[1]))

res =[]
for i in record.keys():
    res.append([i, record[i]])
res.sort(key=lambda e: e[1], reverse=True)

result = ''
for i in res:
    result += i[0]+','
print(result[0:-1])
