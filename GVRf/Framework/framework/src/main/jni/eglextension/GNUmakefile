
SUBDIRS = \
		  msaa \

.PHONY: subdirs $(SUBDIRS)

subdirs: $(SUBDIRS)

$(SUBDIRS):
		$(MAKE) -C $@

clean: 
	rm -rf $(OBJS) $(OBJSDIR); \
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir clean; \
	done
	
