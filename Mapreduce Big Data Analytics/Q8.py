import sys

file1=open("film.txt")
file2 = open('tv.txt')
film =[]
while 1:
    line = file1.readline()
    if not line:
        break
    array=line.split('\t')
    film.append(array[1].split('(')[0])
file1.close()

tv=[]
while 1:
    line = file2.readline()
    if not line:
        break
    array = line.split('\t')
    tv.append(array[1].split('(')[0])

file2.close()

# print(film)
# print(tv)

res=0
for i in film:
    if i in tv:
        res += 1
print(res)