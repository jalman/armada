import sys
import os
import subprocess
import re

_maps2 = [ 'fieldsofgreen', 'almsman', 'troll', 'moo', 'temple', 'siege', 'backdoor', 'bakedpotato', 'blocky', 'castles', 'flagsoftheworld', 'cadmic', 'fenced' ] 
_maps = _maps2[:]

_teamA = 'mergebot'
_teamB = 'emptyplayer'

_replayFile = 'testmany.rms'
_saveFile = 'testmanygamelog.txt'

BUILDFILE = 'multitestbuild.xml'
CONFFILE = 'multibc.conf'

conf_template = '''
# Match server settings
bc.server.throttle=yield
bc.server.throttle-count=50

# Game engine settings
bc.engine.debug-methods=false
bc.engine.silence-a=false
bc.engine.silence-b=true
bc.engine.gc=true
bc.engine.gc-rounds=200
bc.engine.upkeep=true
bc.engine.breakpoints=true
bc.engine.bytecodes-used=true

# Headless settings - for "ant file"
bc.game.maps={}
bc.game.team-a={}
bc.game.team-b={}
bc.server.save-file={}

# Transcriber settings
bc.server.transcribe-input=matches\\match.rms
bc.server.transcribe-output=matches\\transcribed.txt
'''

def updateMultiBCConf(maps, teamA, teamB, replayFile='match.rms'):
    print 'updating {} with maps {}, teams {} vs {}, and replayfile {}'.format(CONFFILE, maps, teamA, teamB, replayFile)
    with open(CONFFILE, 'w') as f:
        f.write(conf_template.format(','.join(maps), teamA, teamB, replayFile))

MATCH_START_TEXT = '- Match Starting -'
SERVER_MSG_START = '     [java] [server]'

WIN_LINE_REGEX = re.compile(r' \([AB]\) wins')

def runMatches(maps, teamA, teamB, replayFile, saveFile):
    updateMultiBCConf(maps, teamA, teamB, replayFile)

    print 'running matches...'
    output = subprocess.check_output(['ant', '-f', BUILDFILE, 'file'])
    linenum = 0

    with open(saveFile, 'w') as f:
        f.write(output)

    ret = []
    
    output = output.split('\n')
    for i in xrange(len(maps)):
        mapresult = ''
        while(output[linenum].find('- Match Starting -') < 0):
            linenum += 1
        linenum += 1
        mapresult += output[linenum][len(SERVER_MSG_START):].strip()
        mapresult += ': '
        while(re.search(WIN_LINE_REGEX, output[linenum]) == None):
            linenum += 1
        mapresult += output[linenum][len(SERVER_MSG_START):].strip()

        ret.append(mapresult)

    return ret

def getRounds(result):
    search = "(round"
    index = result.index(search) + len(search)
    
    return int(result[index:-1])

def main(maps, teamA, teamB, replayFile, saveFile):
    if len(sys.argv) == 3:
        teamA = sys.argv[1]
        teamB = sys.argv[2]
        print teamA, teamB
    results = runMatches(maps, teamA, teamB, replayFile, saveFile)
    
    print '\n'.join(results)
    print "Total rounds: %d" % sum(map(getRounds, results))
    
if __name__ == '__main__':
    main(_maps, _teamA, _teamB, _replayFile, _saveFile)
