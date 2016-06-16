TARGET = ScriptToAssembly.jar
CLASSES = \
	ScriptToAssembly.class \
	ScriptParser.class \
	Type.class \
# blank line (allow \ after the last class)

$(TARGET): $(CLASSES) jar-manifest.txt
	jar cfm $(TARGET) jar-manifest.txt $(CLASSES)

%.class: %.java
	javac -encoding UTF-8 $<

.PHONY: clean
clean:
	rm -f $(TARGET) $(CLASSES)
