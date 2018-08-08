RUNS = c2k1800 c2k1800b c2k1800d c2k1800e1 c2k1800e2 c2k1800e3 \
       c2k2500 c2k2000 c2k3000 c5k2500 c2k1800d_s c2k1800e1_s c2k1800f

OUTS = $(addsuffix _out.csv, $(RUNS))
NETS = $(addsuffix _net.csv, $(RUNS))
TXTS = $(addsuffix _log.txt, $(RUNS))

%_out.csv : market.jar %.txt netmap.csv
	ver $*_log.txt $*_out.csv $*_net.csv
	java -jar market.jar $*.txt 

all : $(OUTS)

market.jar :: ../src/market.jar ; cp $< $@

tidy :
	ver -r $(TXTS) $(OUTS) $(NETS)

clean :
	@rm -f *.jar $(TXTS) $(OUTS) $(NETS)

check :
	ver -d $(OUTS) $(TXTS) $(NETS)

restore :
	make tidy
	make clean
	git checkout $(OUTS) $(TXTS) $(NETS)
