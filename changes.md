## 2.3.1

- adjust thread pool for environment context update

the pool works like it did before 2.1.0 except it only uses 2 threads
this also fixes an issue where small and medium sphere update tasks would run less often than intended

- run environment context update at most every second

saves some wattage, probably has little to no effect on FPS/TPS

## 2.3.0

- fix another 2 bugs in how music was chosen - these should be the last!

the bugs resulted in custom music being less likely to play than intended and some custom tracks being skipped

- add a "Custom Music Weight Scale" option, to tune the likelihood of playing custom music

this is by default set to 250% as custom music was very rare to play, since there's so little of it