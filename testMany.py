import sys
import os
import subprocess
import re

_maps2 = [ 'fieldsofgreen', 'almsman', 'troll', 'moo', 'temple', 'siege', 'backdoor', 'bakedpotato', 'blocky', 'castles', 'flags', 'cadmic', 'fenced' ] 
_maps = _maps2[:6]

_teamA = 'mergebot'
_teamB = 'examplefuncsplayer'

_replayFile = 'gatherspeed.rms'

BUILDFILE = 'multitestbuild.xml'
CONFFILE = 'multibc.conf'

conf_template = '''
# Match server settings
bc.server.throttle=yield
bc.server.throttle-count=50

# Game engine settings
bc.engine.debug-methods=false
bc.engine.silence-a=true
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

def runMatches(maps, teamA, teamB, replayFile):
    updateMultiBCConf(maps, teamA, teamB, replayFile)

    print 'running matches...'
    output = subprocess.check_output(['ant', '-f', BUILDFILE, 'file']).split('\n')
    linenum = 0

    #print output

    ret = []
    
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

def main(maps, teamA, teamB, replayFile):
    results = runMatches(maps, teamA, teamB, replayFile)
    print '\n'.join(results)
                
    
if __name__ == '__main__':
    main(_maps, _teamA, _teamB, _replayFile)
