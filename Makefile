CLASSES = \
	ScriptToAssembly.class \
	ScriptParser.class \
# blank line (allow \ after the last class)

ScriptToAssembly.jar: $(CLASSES) jar-manifest.txt
	jar cfm $@ jar-manifest.txt $(CLASSES)

%.class: %.java
	javac -encoding UTF-8 $<
