
SUBDIRS = \
		  exporter \
		  picker \
		  renderer \

.PHONY: subdirs $(SUBDIRS)

subdirs: $(SUBDIRS)

$(SUBDIRS):
		$(MAKE) -C $@

picker: renderer
exporter: picker
