import sys
result = ''
for line in sys.stdin:
    if not line:
        break
    result += line[0:-1]+','
print(result[0:-1])
