import sys
import math

def area_diagram(rsquared=6, xmin = 0, xmax = 10, ymin = 0, ymax = 10, xcenter = 4, ycenter = 4):
    themap = []
    count = 0
    for x in xrange(xmin, xmax+1):
        row = []
        for y in xrange(ymin, ymax+1):
            if (x-xcenter)**2 + (y-ycenter)**2 <= rsquared:
                row.append(1)
            else:
                row.append(0)
        themap.append(row)
    return themap

if __name__ == '__main__':
    rsquaredmin = int(sys.argv[1])
    rsquaredmax = int(sys.argv[2])
    for rsquared in range(rsquaredmin, rsquaredmax+1):
        r = int(math.sqrt(rsquared))+1

        themap = area_diagram(rsquared, -r, r, -r, r, 0, 0)
        print '\n'.join([''.join([str(i) for i in row]) for row in themap])

