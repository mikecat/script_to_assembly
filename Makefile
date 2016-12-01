TARGET = ScriptToAssembly.jar
CLASSES = \
	ScriptToAssembly.class \
	ScriptParser.class \
	DataType.class \
	PrimitiveType.class \
	PointerType.class \
	ArrayType.class \
	FunctionType.class \
	Expression.class \
	IntegerLiteral.class \
	StringLiteral.class \
	Identifier.class \
	UnaryOperator.class \
	BinaryOperator.class \
	Function.class \
	Instruction.class \
	NormalExpression.class \
	SyntaxException.class \
	SystemLimitException.class \
	FunctionBuilder.class \
	NoneType.class \
	Variable.class \
	InstructionBuilder.class \
	InfiniteLoop.class \
	InfiniteLoopBuilder.class \
	WhileLoop.class \
	WhileLoopBuilder.class \
# blank line (allow \ after the last class)

$(TARGET): $(CLASSES) jar-manifest.txt
	jar cfm $(TARGET) jar-manifest.txt *.class

%.class: %.java
	javac -encoding UTF-8 $<

.PHONY: clean
clean:
	rm -f $(TARGET) $(CLASSES)
