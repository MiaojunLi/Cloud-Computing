import sys

title=None
viewNum = 0
for line in sys.stdin:
        if not line:
            break
        array = line.split('\t')
        if int(array[19].split(':')[1])>viewNum:
            title = array[1]
            viewNum = int(array[19].split(':')[1])

print '%s\t%s' % (title,viewNum)