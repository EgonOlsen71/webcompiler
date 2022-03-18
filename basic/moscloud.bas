0 rem moscloud - egonolsen71/2022
5 gosub 1000:gosub 56500:gosub 62000:gosub 45800
10 gosub 57000:gosub 10000:if ff%=0 then 10
20 gosub 40500
30 gosub 44000:gosub 52000
60 goto 60000

1000 rem setup screen
1010 print chr$(147);chr$(14);chr$(8);
1020 poke 53280,6:poke53281,6:poke 646,1
1030 return

10000 rem read and transfer file
10010 gosub 40100:da$="":dc%=0:ff%=1
10020 gosub 51000:tc=0:cn%=0:ck%=0:if er%<>0 then gosub 13000:ff%=0:return
10030 poke 2023,4:gosub 50000
10040 if do%=0 then if er%=0 then gosub 11000:goto 10030
10050 if er%<>0 then 56000
10060 if do%=1 then gosub 10500:gosub 12000
10070 gosub 51500
10080 return

10500 rem append chunk
10510 if len(da$)=0 then return
10520 dt$(dc%)=da$:da$="":dc%=dc%+1
10530 return

11000 rem store byte and maybe transfer
11010 da$=da$+by$:cn%=cn%+1:tc=tc+1
11020 if len(da$)>64 then gosub 10500
11030 if cn%=ml% then gosub 10500:gosub 12000
11040 return

12000 rem transfer chunk
12005 if cn%=0 then return
12010 ck%=ck%+1:print "Processing chunk";ck%;"...";
12030 gosub 41000:gosub 42000:print mg$
12040 cn%=0:da$="":dc%=0:return

13000 rem press any key
13010 print:print"Press any key!"
13020 get a$:if a$="" then 13020
13030 return

39000 rem low/highbyte. Value in tb, result in lb% and hb%
39010 lb%=tb and 255:hb%=int(tb/256):return

40000 rem select program
40010 print chr$(147);
40015 input "Filename";of$:if of$="" then 40015
40020 return

40100 rem create random filename
40120 for i=0 to 10:dt$(i)="":next
40130 rn=int(rnd(1)*9999999)
40140 t$=str$(rn):t$=right$(t$,len(t$)-1)
40150 r$=str$(peek(53248+18)):r$=right$(r$,len(r$)-1)
40160 tf$=t$+"-"+r$+"-"+of$+".bin"
40180 return

40500 rem compile program
40502 if tc<2 then print:print "Error: File not found!":goto 60000
40505 print "Starting remote compiler...";
40510 tl=0:dc%=0:ur$=gu$+"WiCompile"
40520 ur$=ur$+"?file="+tf$
40521 if xm%>0 then ur$=ur$+"&bigram=1"
40522 if sa>0 then n=sa:gosub 40900:ur$=ur$+"&sa="+ns$
40523 if hs<=0 then 40526
40524 n=hs:gosub 40900:hs$=ns$:n=he:gosub 40900
40525 ur$=ur$+"&mh="+hs$+"-"+ns$
40526 if cl%>0 then n=cl%:gosub 40900:ur$=ur$+"&cl="+ns$
40530 gosub 46500:gosub 41500
40550 gosub 42000:print "ok"
40560 print "Waiting for result...";
40570 tl=0:dc%=0:ur$=gu$+"WiCompile"
40580 ur$=ur$+"?poll=1&file="+tf$
40590 gosub 46500:gosub 41500
40600 gosub 42000:if mg$="no" then gosub 40800:print".";:goto 40590
40610 if left$(mg$,4)="OOM:" then mg$="Binary too large!":goto 43000
40650 tf$=mg$:print "ok"
40660 return

40800 rem wait for a second
40810 ti$="000000":ot=ti+90:poke 2023,23
40820 if ti<ot then 40820
40830 poke 2023,32:return

40900 rem number to string
40910 ns$=str$(n)
40920 if n<0 then return
40930 ns$=right$(ns$, len(ns$)-1):return

41000 rem upload file part
41010 gosub 45500
41020 gosub 46500
41030 gosub 41500:return

41500 rem send and receive data
41510 poke 2023,18:poke 171,tt%:sys us,bu
41520 if peek(171)=0 then 56000
41530 poke 171,tt%:sys ug,bu+200
41540 if peek(171)=0 then 56000
41550 gosub 46000:if le% then 56200
41560 er%=0:br%=peek(169)+256*peek(170):poke 2023,32:return

42000 rem grab reply
42010 mg$="":if br%=0 then return
42020 for i=bu+200 to bu+199+br%
42030 dd%=peek(i)
42040 gosub 47300
42060 mg$=mg$+chr$(dd%)
42070 next
42080 if len(mg$)<=5 then return
42090 a$=left$(mg$,5):if a$="error" or a$="Error" then 43000
42100 return

43000 rem fatal error
43010 gosub 51500:print "error":print:print mg$:goto 60000

44000 rem download compiled program
44010 print "Downloading file...";:gosub 48000
44020 tl=0:pt%=0:dc%=0
44025 gosub 44500
44030 gosub 46500:gosub 41500
44040 nc%=peek(bu+200):poke 2023,16
44050 da$="":p%=0:for i=bu+201 to bu+199+br%
44060 da$=da$+chr$(peek(i))
44070 if len(da$)>32 then gosub 49000:gosub 44300
44080 next:if len(da$)>0 then gosub 49000
44090 if nc%>0 then pt%=nc%:goto 44025
44100 gosub 48500:poke 2023,32:print "ok":return

44300 rem progress indicator and such
44310 da$="":p%=p%+1:poke 2023,16:if p%>11 then p%=0:print".";
44320 return

44500 rem contruct download url
44510 pt$=str$(pt%):ur$=gu$+"Download?size=2048"
44520 ur$=ur$+"&part="+right$(pt$,len(pt$)-1)+"&file="+tf$
44530 return

45500 rem create send url (almost...)
45505 tl=0:for i=0 to dc%-1:tl=tl+len(dt$(i)):next
45510 tb=tl:gosub 39000
45525 ur$=gu$+"ChunkedUpload"
45530 ur$=ur$+"?file="+tf$+"&data="
45560 ur$=ur$+"<$"+chr$(lb%)+chr$(hb%)
45580 return

45700 rem create cloud url
45710 ur$="http://jpct.de/mospeed/ipget.php":return

45800 rem get actual ip
45810 if len(gu$)<>0 then 45850
45820 print chr$(147);"Getting remote address...":gosub 45700
45830 gosub 46500:gosub 41500:gosub 42000
45840 gu$=mg$
45850 print:print "Address: ";gu$:return

46000 rem check load error
46010 le%=(peek(169)=2) and (peek(bu+200)=33) and (peek(bu+201)=48)
46020 if le% then lv%=peek(171):sys ui: sys ur
46030 return

46500 rem store url in memory
46510 poke 2023,16:lm=len(ur$):ls=lm-4:for t=1 to lm
46520 b3=bu+3:dd%=asc(mid$(ur$,t,1)):if dc%>0 then if t>=ls then 46560 
46540 gosub 47300
46560 poke b3+t,dd%
46570 next:gosub 47000
46575 if dc%=0 then poke 2023,32:return
46580 t=bu+2+t:for i=0 to dc%-1:da$=dt$(i)
46590 for p=1 to len(da$):dd%=asc(mid$(da$,p,1))
46600 poke t+p,dd%:next
46610 t=t+p-1:next
46620 poke 2023,32:return

47000 rem store length in memory
47010 d=len(ur$)+4+tl:s=bu+1:gosub 47100
47020 return

47100 rem store d in s (lo/hi)
47105 tb=d:gosub 39000
47110 poke s,lb%:poke s+1,hb%:return

47300 rem convert ascii-petscii
47310 if dd%>=65 then if dd%<=90 then dd%=dd%+32:return
47320 if dd%>=97 then if dd%<=122 then dd%=dd%-32
47330 return

48000 rem open target file
48010 nf$="++"+left$(of$,14)
48015 open 15,dn%,15,"s:"+nf$:close 15
48020 open 2,dn%,2,nf$+",prg,w"
48030 return

48500 rem close target file
48510 close 2:return

49000 rem write into target file
49010 poke 2023,4:print#2,da$;:return

50000 rem read character/byte from file
50010 so=st:if so=64 then do%=1:return
50020 if so<>0 then er%=1:return
50030 get by$:if len(by$)=0 then by$=ll$
50040 return

51000 rem open file in of$ as channel 2
51005 print "Loading '";of$;"'..."
51010 open 2,dn%,2,of$:open 15,dn%,15:input#15,ec,em$:close 15:close 2
51020 if ec<>0 then print em$:er%=1:do%=0:return
51040 open 2,dn%,2,of$:poke 781,2:sys 65478
51050 do%=0:er%=0:return

51500 rem close channel 2
51510 sys 65484:close 2
51520 return

52000 rem print end message
52010 print:print"Compiled file: "+nf$
52020 print "Press RETURN to load it now!":print
52030 print "load";chr$(34);nf$;chr$(34);",";dn%;",1";
52040 poke 631,145:poke 632,145:poke 198,2:return

55000 rem init wic64
55005 print chr$(147);"Initializing wic64...";
55010 sys ui: rem init
55020 sys uc: rem check presence
55030 gosub 56000
55035 poke bu,87:poke bu+3,15: rem "w" mode, http get
55040 print "ok"
55050 return

56000 rem wic64 error?
56010 if peek(171)<>0 then return
56030 poke 2023,32:print:print "Communication error!":print
56040 print "Either there's no wic64 present"
56050 print "or the connection has timed out!"
56060 goto 60000

56200 rem load error
56230 gosub 51500:poke 2023,32:print:print "load error (";lv%;")!"
56260 goto 60000

56500 rem check for api presence...
56505 dn%=peek(186)
56510 lf%=peek(49152)=76 and peek(49153)=30 and peek(49154)=192
56520 if lf%=0 then print chr$(147);"Loading...":load "universal",dn%,1
56530 return

57000 rem display main menu
57010 print chr$(147);:poke 2023,32
57012 print "MOSCloud - a remote BASIC compiler"
57015 print "by EgonOlsen71 / 2022":print
57020 print "F1 - Select source: ";of$
57025 print "F2 - Select drive:";dn%
57030 print "F3 - Show directory"
57040 print "F5 - Options"
57050 print "F8 - Quit":print
57055 print "F7 - Compile!"
57060 get a$:if a$="" then 57060
57070 a%=asc(a$):if a%=133 then gosub 40000:goto 57010
57080 if a%=134 then gosub 58000:goto 57010
57090 if a%=135 then gosub 57500:goto 57010
57100 if a%=136 then print chr$(147);:return
57110 if a%=140 then print chr$(147);:print"Have a nice BASIC!":goto 60000
57120 if a%=137 then 57200
57130 goto 57060
57200 dn%=dn%+1:if dn%=10 then dn%=8
57210 goto 57010

57500 rem options screen
57510 print chr$(147);"MOSCloud - Options":print
57520 print "F1 - Start address:";:if sa<=0 then print " Default":goto 57530
57525 print sa
57530 print "F3 - Memory hole at:";:if hs<=0 then print " None":goto 57536
57535 print hs;" -";he
57536 print "F4 - Use extended memory:";:if xm%=0 then print " No":goto 57540
57537 print " Yes"
57540 print "F5 - Compact level:";:if cl%<=0 then print " Default":goto 57580
57545 print cl%
57580 print "F8 - Refresh remote server:":print"     ";right$(gu$,len(gu$)-7)
57590 print:print "F7 - Exit options menu"
57700 get a$:if a$="" then 57700
57710 a%=asc(a$):if a%=140 then gu$="":gosub 45800:goto 57510
57720 if a%=133 then gosub 60100:goto 57510
57730 if a%=134 then gosub 60200:goto 57510
57740 if a%=135 then gosub 60300:goto 57510
57750 if a%=138 then xm%=(xm%+1) and 1:goto 57510
57790 if a%=136 then return
57800 goto 57700

58000 rem print directory
58010 print chr$(147);
58020 open 1,dn%,0,"$":poke 781,1:sys 65478:get a$,a$
58030 get a$,a$,h$,l$:if st then sys 65484:close 1:goto 58070
58040 print asc(h$+ll$)+256*asc(l$+ll$);
58050 get a$,b$:if b$ then print a$b$;:if st=0 then 58050
58060 print a$:goto 58030
58070 gosub 13000
58080 return

60000 rem end program
60010 poke 45,0:poke 46,10:poke 47,0:poke 48,10:poke 49,0:poke 50,10
60020 poke 55,0:poke 56,160:poke 51,0:poke 52,160:print chr$(9);:end

60100 rem enter start address
60105 print chr$(147);"Enter a new start address!":print
60110 input "Start address (in decimal)";sa
60115 if sa=0 then sa=-1:return
60120 if sa<2049 or sa>53247 then print"Invalid start address!":goto 60110
60130 return

60200 rem enter memory hole
60205 print chr$(147);"Enter addresses of the memory hole!"
60208 print "This memory section remains unused.":print
60210 input "Start of hole (in decimal)";hs
60215 if hs=0 then hs=-1:he=-1:return
60220 if hs<2049 or hs>53247 then print"Invalid start address!":goto 60210
60230 input "End of hole (in decimal)";he
60240 if he<2049 or he>53247 then print"Invalid end address!":goto 60230
60250 if hs>he then ht=he:he=hs:hs=ht
60260 return

60300 rem enter compact level
60305 print chr$(147);"Enter compact level!"
60306 print "Lower is more compact but slower.":print
60310 input "Compact level (3-6)";cl%
60315 if cl%=0 then cl=-1:return
60320 if cl%<3 or cl%>6 then print "Invalid level!":goto 60310
60330 return

62000 rem init
62010 ll$=chr$(0):i=rnd(0)
62020 tt%=64:bu=49976:ui=49152
62030 ur=49155:us=49152+18:ug=49152+21
62040 uc=49152+24:sa=-1:hs=-1:he=-1:cl%=-1:xm%=0
62050 ml%=1792:dim dt$(40)
62060 gu$=""
62065 rem gu$="http://192.168.178.20:8080/WebCompiler/"
62070 of$="test"
62100 gosub 55000:return
