RUNS = c2k1800 c2k2500 c2k2000 c2k3000 \
       c5k2500 

OUTS = $(addsuffix _out.csv, $(RUNS))
TXTS = $(addsuffix _log.txt, $(RUNS))

%_out.csv : market.jar %.txt
	ver $*_log.txt $*_out.csv
	java -jar market.jar $*.txt 

all : $(OUTS)

market.jar :: ../src/market.jar ; cp $< $@

tidy :
	ver -r $(TXTS) $(OUTS)

clean :
	@rm -f *.jar $(TXTS) $(OUTS)

check :
	ver -d $(OUTS) $(TXTS)
