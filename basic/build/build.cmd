call mospeed ..\moscloud.bas -sysbuffer=53000 -generatesrc=true
call moscrunch ++moscloud.prg -addfiles=..\res\universal.prg
del moscloud.d64
..\res\c1541 -format moscloud,ml d64 moscloud.d64
call ..\res\c1541 ..\build\moscloud.d64 -write ++moscloud-c.prg moscloud,p
call ..\res\c1541 ..\build\moscloud.d64 -write ..\res\test.prg test,p
call ..\res\c1541 ..\build\moscloud.d64 -write ..\res\error.prg errortest,p
cd ..\build
