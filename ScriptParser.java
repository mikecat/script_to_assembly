import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class ScriptParser {
	private String[] libraryDir = new String[0];
	private boolean debug = false;

	private List<Identifier> variableDefinitionList;
	private List<Function> functionDefinitionList;

	private Map<String, Identifier> globalIdentifierDeclarationList;
	private Map<String, Identifier> localIdentifierDeclarationList;

	private boolean isInFunction;
	private FunctionBuilder currentFunction;
	private Deque<InstructionBuilder> instructionStack;

	public ScriptParser() {
		resetParseStatus();
	}

	public void setLibraryDir(List<String> libraryDir) {
		this.libraryDir = libraryDir.toArray(new String[libraryDir.size()]);
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public Function[] getFunctionDefinitionList() {
		return functionDefinitionList.toArray(new Function[functionDefinitionList.size()]);
	}

	public void resetParseStatus() {
		variableDefinitionList = new ArrayList<Identifier>();
		functionDefinitionList = new ArrayList<Function>();
		globalIdentifierDeclarationList = new HashMap<String, Identifier>();
		localIdentifierDeclarationList = new HashMap<String, Identifier>();
		isInFunction = false;
		currentFunction = null;
		instructionStack = new ArrayDeque<InstructionBuilder>();
	}

	public boolean parse(BufferedReader br, File file, int ttl) {
		String fileName = (file == null ? "(stdin)" : file.getName());
		if (ttl <= 0) {
			throw new SystemLimitException("TTL expired when trying to parse " + fileName);
		}
		String line;
		int lineCount = 1;
		try {
			for (; (line = br.readLine()) != null; lineCount++) {
				// インデントを消す (末尾の空白も消える)
				line = line.trim();
				// 空行は無視する
				if (line.equals("")) continue;
				// 指示と内容に分割する
				String[] actionAndData = line.split("\\s+", 2);
				String action = actionAndData.length > 0 ? actionAndData[0] : "";
				String data = actionAndData.length > 1 ? actionAndData[1] : null;

				// 指示に従って動く
				if (action.equals("include")) {
					if (!processInclude(file, fileName, lineCount, ttl, data)) return false;
				} else if (action.equals("uselib")) {
					if (!processUselib(fileName, lineCount, ttl, data)) return false;
				} else if (action.equals("function")) {
					processFunction(data);
				} else if (action.equals("endfunction")) {
					processEndfunction(data);
				} else if (action.equals("var")) {
					processVar(data);
				} else if (action.equals("param") || action.equals("argument")) {
					processParam(data);
				} else if (action.equals("vardeclare")) {
					processVardeclare(data);
				} else if (action.equals("funcdeclare")) {
					processFuncdeclare(data);
				} else if (action.equals("address")) {
					processAddress(data);
				} else if (action.equals("loop")) {
					processLoop();
				} else if (action.equals("endloop")) {
					processEndloop();
				} else if (action.equals("while")) {
					processWhile(data);
				} else if (action.equals("endwhile")) {
					processEndwhile();
				} else if (action.equals("if")) {
					processIf(data);
				} else if (action.equals("elseif")) {
					processElseif(data);
				} else if (action.equals("else")) {
					processElse();
				} else if (action.equals("endif")) {
					processEndif();
				} else if (action.equals("return")) {
					processReturn(data);
				} else if (action.equals("break")) {
					processBreak(data);
				} else if (action.equals("continue")) {
					processContinue(data);
				} else { // キーワードが無かったので、式とみなす
					processExpression(line);
				}
			}
		} catch (Exception e) {
			System.err.println("following error occured at file " + fileName + ", line " + lineCount);
			if (debug) e.printStackTrace(); else System.err.println(e);
			return false;
		}
		return true;
	}

	public Identifier lookupIdentifier(String name) {
		if (isInFunction) {
			Identifier hit = localIdentifierDeclarationList.get(name);
			if (hit != null) return hit;
		}
		return globalIdentifierDeclarationList.get(name);
	}

	private void disallowOutsideFunction(String name) {
		if (!isInFunction) {
			throw new SyntaxException(name + " isn't allowed outside function");
		}
	}

	private boolean processInclude(File file, String fileName, int lineNumber, int ttl, String data) throws IOException {
		if (data == null) {
			throw new SyntaxException("file name to include not found");
		}
		File fileToRead;
		if (file == null) {
			fileToRead = new File(data);
		} else {
			fileToRead = new File(file.getParentFile(), data);
		}
		BufferedReader br = new BufferedReader(new FileReader(fileToRead));
		if (!parse(br, fileToRead, ttl - 1)) {
			System.err.println("... included from file " + fileName + ", line " + lineNumber);
			br.close();
			return false;
		}
		br.close();
		return true;
	}

	private boolean processUselib(String fileName, int lineNumber, int ttl, String data) throws IOException {
		if (data == null) {
			throw new SyntaxException("file name to use not found");
		}
		for (int i = 0; i < libraryDir.length; i++) {
			File libFile = new File(libraryDir[i], data);
			if (libFile.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(libFile));
				if (!parse(br, libFile, ttl - 1)) {
					System.err.println("... included from file " + fileName + ", line " + lineNumber);
					br.close();
					return false;
				}
				br.close();
				return true;
			}
		}
		throw new SyntaxException("file " + data + " not found in library path(es)");
	}

	private void processFunction(String data) {
		if (isInFunction) {
			throw new SyntaxException("nested function isn't allowed");
		} else {
			if (data == null) {
				throw new SyntaxException("function name not found");
			}
			String[] nameAndType = data.split("\\s+", 2);
			if (nameAndType.length < 2) {
				throw new SyntaxException("function return type not found");
			}
			DataType returnType = DataType.parse(nameAndType[1], this);
			DataType thisFunctionType = new FunctionType(returnType);
			Identifier existingFunction = lookupIdentifier(nameAndType[0]);
			if (existingFunction != null) {
				if (existingFunction.getDataType().equals(thisFunctionType)) {
					// 同じ名前の宣言が既にあり、型が同じ → 定義されているかを調べる
					for (Iterator<Function> it = functionDefinitionList.iterator(); it.hasNext(); ) {
						if (it.next().getName().equals(nameAndType[0])) {
							// 同じ名前の関数が定義されている
							throw new SyntaxException("function " + nameAndType[0] + " is already defined");
						}
					}
				} else {
					// 同じ名前の宣言が既にあり、型が違う
					throw new SyntaxException("declaration of function " + nameAndType[0] + " conflicts");
				}
			}
			// 関数の宣言を登録する
			Variable newFunction = new Variable(nameAndType[0],
				thisFunctionType, true, functionDefinitionList.size());
			globalIdentifierDeclarationList.put(nameAndType[0], newFunction);
			// 関数の定義を開始する
			isInFunction = true;
			currentFunction = new FunctionBuilder(nameAndType[0], returnType);
			instructionStack.clear();
			instructionStack.addFirst(currentFunction);
			localIdentifierDeclarationList.clear();
		}
	}

	private void processEndfunction(String data) {
		if (isInFunction) {
			// 制御構造が終わっていなかったらエラーを出す
			if (!(instructionStack.peekFirst() instanceof FunctionBuilder)) {
				throw new SyntaxException("unterminated " + instructionStack.peekFirst().getInstructionName());
			}
			// 関数の定義を確定させて登録する
			Function definedFunction = currentFunction.toFunction();
			functionDefinitionList.add(definedFunction);
			currentFunction = null;
			isInFunction = false;
		} else {
			throw new SyntaxException("endfunction without function");
		}
	}

	private void processVar(String data) {
		if (data == null) {
			throw new SyntaxException("variable name not found");
		}
		String[] nameAndType = data.split("\\s+", 2);
		if (nameAndType.length < 2) {
			throw new SyntaxException("variable type not found");
		}
		if (isInFunction) {
			// ローカル変数の重複チェック
			Identifier existingIdentifier = lookupIdentifier(nameAndType[0]);
			if (existingIdentifier != null && !existingIdentifier.isGlobal()) {
				throw new SyntaxException("local variable " + nameAndType[0] + " is already defined");
			}
			// ローカル変数を作成して登録する
			Identifier var = currentFunction.addVariable(nameAndType[0], DataType.parse(nameAndType[1], this));
			localIdentifierDeclarationList.put(nameAndType[0], var);
		} else {
			DataType varType = DataType.parse(nameAndType[1], this);
			// グローバル変数の重複チェック
			Identifier existingIdentifier = lookupIdentifier(nameAndType[0]);
			if (existingIdentifier != null) {
				if (existingIdentifier.getDataType().equals(varType)) {
					// 同じ名前の宣言が既にあり、型が同じ → 定義されているかを調べる
					for (Iterator<Identifier> it = variableDefinitionList.iterator(); it.hasNext(); ) {
						if (it.next().getName().equals(nameAndType[0])) {
							// 同じ名前の変数が定義されている
							throw new SyntaxException("variable " + nameAndType[0] + " is already defined");
						}
					}
					for (Iterator<Function> it = functionDefinitionList.iterator(); it.hasNext(); ) {
						if (it.next().getName().equals(nameAndType[0])) {
							// 同じ名前の関数が定義されている
							throw new SyntaxException(nameAndType[0] + " is already defined as function");
						}
					}
				} else {
					// 同じ名前の宣言が既にあり、型が違う
					throw new SyntaxException("declaration of variable " + nameAndType[0] + " conflicts");
				}
			}
			// グローバル変数を作成して登録する
			Variable var = new Variable(nameAndType[0], varType, true, variableDefinitionList.size());
			variableDefinitionList.add(var);
			globalIdentifierDeclarationList.put(nameAndType[0], var);
		}
	}

	private void processParam(String data) {
		disallowOutsideFunction("parameter");
		if (data == null) {
			throw new SyntaxException("parameter name not found");
		}
		String[] nameAndType = data.split("\\s+", 2);
		if (nameAndType.length < 2) {
			throw new SyntaxException("parameter type not found");
		}
		// 引数の重複チェック
		Identifier existingIdentifier = lookupIdentifier(nameAndType[0]);
		if (existingIdentifier != null && !existingIdentifier.isGlobal()) {
			throw new SyntaxException("local variable " + nameAndType[0] + " is already defined");
		}
		// 引数を作成して登録する
		Identifier var = currentFunction.addVariable(nameAndType[0], DataType.parse(nameAndType[1], this));
		localIdentifierDeclarationList.put(nameAndType[0], var);
	}

	private void processVardeclare(String data) {
		if (data == null) {
			throw new SyntaxException("variable name not found");
		}
		String[] nameAndType = data.split("\\s+", 2);
		if (nameAndType.length < 2) {
			throw new SyntaxException("variable type not found");
		}
		DataType varType = DataType.parse(nameAndType[1], this);
		Identifier existingIdentifier = lookupIdentifier(nameAndType[0]);
		if (isInFunction) {
			// ローカル変数と重複しているかチェック
			if (existingIdentifier != null) {
				if (!existingIdentifier.isGlobal()) {
					throw new SyntaxException("local variable " + nameAndType[0] + " is already defined");
				} else if (!existingIdentifier.getDataType().equals(varType)) {
					throw new SyntaxException("declaration of variable " + nameAndType[0] + " conflicts");
				}
			} else {
				// ローカルの宣言を作成して登録する
				Variable var = new Variable(nameAndType[0], varType, false, -1);
				localIdentifierDeclarationList.put(nameAndType[0], var);
			}
		} else {
			// グローバル変数の重複チェック
			if (existingIdentifier != null && !existingIdentifier.getDataType().equals(varType)) {
				// 同じ名前の宣言が既にあり、型が違う
				throw new SyntaxException("declaration of variable " + nameAndType[0] + " conflicts");
			}
			// グローバル変数を作成して登録する
			Variable var = new Variable(nameAndType[0], varType, true, -1);
			globalIdentifierDeclarationList.put(nameAndType[0], var);
		}
	}

	private void processFuncdeclare(String data) {
		if (data == null) {
			throw new SyntaxException("function name not found");
		}
		String[] nameAndType = data.split("\\s+", 2);
		if (nameAndType.length < 2) {
			throw new SyntaxException("function type not found");
		}
		DataType returnType = DataType.parse(nameAndType[1], this);
		DataType functionType = new FunctionType(returnType);
		Identifier existingIdentifier = lookupIdentifier(nameAndType[0]);
		if (isInFunction) {
			// ローカル変数と重複しているかチェック
			if (existingIdentifier != null) {
				if (!existingIdentifier.isGlobal()) {
					throw new SyntaxException("local variable " + nameAndType[0] + " is already defined");
				} else if (!existingIdentifier.getDataType().equals(functionType)) {
					throw new SyntaxException("declaration of function " + nameAndType[0] + " conflicts");
				}
			} else {
				// ローカルの宣言を作成して登録する
				Variable var = new Variable(nameAndType[0], functionType, false, -1);
				localIdentifierDeclarationList.put(nameAndType[0], var);
			}
		} else {
			// グローバルの重複チェック
			if (existingIdentifier != null && !existingIdentifier.getDataType().equals(functionType)) {
				// 同じ名前の宣言が既にあり、型が違う
				throw new SyntaxException("declaration of function " + nameAndType[0] + " conflicts");
			}
			// グローバルの宣言を作成して登録する
			Variable var = new Variable(nameAndType[0], functionType, true, -1);
			globalIdentifierDeclarationList.put(nameAndType[0], var);
		}
	}

	private void processAddress(String data) {
		if (data == null) {
			throw new SyntaxException("address variable name not found");
		}
		String[] nameAndType = data.split("\\s+", 2);
		if (nameAndType.length < 2) {
			throw new SyntaxException("address variable type not found");
		}
		String[] typeAndValue = nameAndType[1].split("\\s*:\\s*", 2);
		if (typeAndValue.length < 2) {
			throw new SyntaxException("address variable value not found");
		}
		DataType varType = DataType.parse(typeAndValue[0], this);
		Expression address = Expression.parse(typeAndValue[1], this).evaluate();
		if (!(address instanceof IntegerLiteral)) {
			throw new SyntaxException("address have to be an constant");
		}
		AddressVariable newValue = new AddressVariable(nameAndType[0], varType,
			!isInFunction, ((IntegerLiteral)address).getValue());
		Identifier existingIdentifier = lookupIdentifier(nameAndType[0]);
		if (isInFunction) {
			// ローカル変数の重複チェック
			if (existingIdentifier != null && !existingIdentifier.isGlobal()) {
				throw new SyntaxException("local variable " + nameAndType[0] + " is already defined");
			}
			// ローカル変数を登録する
			localIdentifierDeclarationList.put(nameAndType[0], newValue);
		} else {
			// グローバル変数の重複チェック
			if (existingIdentifier != null) {
				if (existingIdentifier.getDataType().equals(varType)) {
					// 同じ名前の宣言が既にあり、型が同じ → 定義されているかを調べる
					for (Iterator<Identifier> it = variableDefinitionList.iterator(); it.hasNext(); ) {
						if (it.next().getName().equals(nameAndType[0])) {
							// 同じ名前の変数が定義されている
							throw new SyntaxException("variable " + nameAndType[0] + " is already defined");
						}
					}
					for (Iterator<Function> it = functionDefinitionList.iterator(); it.hasNext(); ) {
						if (it.next().getName().equals(nameAndType[0])) {
							// 同じ名前の関数が定義されている
							throw new SyntaxException(nameAndType[0] + " is already defined as function");
						}
					}
				} else {
					// 同じ名前の宣言が既にあり、型が違う
					throw new SyntaxException("declaration of variable " + nameAndType[0] + " conflicts");
				}
			}
			// グローバル変数を作成して登録する
			globalIdentifierDeclarationList.put(nameAndType[0], newValue);
		}
	}

	private void processLoop() {
		disallowOutsideFunction("loop");
		// 無限ループを開始する
		instructionStack.addFirst(new InfiniteLoopBuilder());
	}

	private void processEndloop() {
		disallowOutsideFunction("endloop");
		if (instructionStack.peekFirst() instanceof InfiniteLoopBuilder) {
			// 作成した無限ループを取って
			InfiniteLoopBuilder ilb = (InfiniteLoopBuilder)instructionStack.removeFirst();
			// 1階層上の命令列に入れる
			instructionStack.peekFirst().addInstruction(ilb.toInfiniteLoop());
		} else {
			throw new SyntaxException("unterminated " + instructionStack.peekFirst().getInstructionName());
		}
	}

	private void processWhile(String data) {
		disallowOutsideFunction("while");
		// whileループを開始する
		Expression condition = Expression.parse(data, this);
		instructionStack.addFirst(new WhileLoopBuilder(condition));
	}

	private void processEndwhile() {
		disallowOutsideFunction("endwhile");
		if (instructionStack.peekFirst() instanceof WhileLoopBuilder) {
			// 作成したwhileループを取って
			WhileLoopBuilder ilb = (WhileLoopBuilder)instructionStack.removeFirst();
			// 1階層上の命令列に入れる
			instructionStack.peekFirst().addInstruction(ilb.toWhileLoop());
		} else {
			throw new SyntaxException("unterminated " + instructionStack.peekFirst().getInstructionName());
		}
	}

	private void processIf(String data) {
		disallowOutsideFunction("if");
		if (data == null) {
			throw new SyntaxException("condition doesn't exist for if");
		}
		// 条件分岐を開始する
		Expression condition = Expression.parse(data, this);
		instructionStack.addFirst(new ConditionalBranchBuilder(condition));
	}

	private void processElseif(String data) {
		disallowOutsideFunction("elseif");
		if (data == null) {
			throw new SyntaxException("condition doesn't exist for elseif");
		}
		if (instructionStack.peekFirst() instanceof ConditionalBranchBuilder) {
			// 直前の条件分岐を取って
			ConditionalBranchBuilder nextBuilder = (ConditionalBranchBuilder)instructionStack.removeFirst();
			if (nextBuilder.isElseMode()) {
				throw new SyntaxException("elseif after else");
			} else {
				// elseifのリンク先として登録しつつ、新しい条件分岐を開始する
				nextBuilder.enterElseMode();
				Expression condition = Expression.parse(data, this);
				instructionStack.addFirst(new ConditionalBranchBuilder(condition, nextBuilder));
			}
		} else {
			throw new SyntaxException("elseif without if");
		}
	}

	private void processElse() {
		disallowOutsideFunction("else");
		if (instructionStack.peekFirst() instanceof ConditionalBranchBuilder) {
			// 直前の条件分岐を取らずに
			ConditionalBranchBuilder cbBuilder = (ConditionalBranchBuilder)instructionStack.peekFirst();
			if (cbBuilder.isElseMode()) {
				throw new SyntaxException("else after else");
			} else {
				// elseの後モードに移行する
				cbBuilder.enterElseMode();
			}
		} else {
			throw new SyntaxException("else without if");
		}
	}

	private void processEndif() {
		disallowOutsideFunction("endif");
		if (instructionStack.peekFirst() instanceof ConditionalBranchBuilder) {
			// 作成した条件分岐を取って
			ConditionalBranchBuilder cbBuilder = (ConditionalBranchBuilder)instructionStack.removeFirst();
			// 1階層上の命令列に入れる
			instructionStack.peekFirst().addInstruction(cbBuilder.toConditionalBranch());
		} else {
			throw new SyntaxException("endif without if");
		}
	}

	private void processReturn(String data) {
		disallowOutsideFunction("return");
		if (data != null) {
			Expression exp = Expression.parse(data, this);
			instructionStack.peekFirst().addInstruction(new ReturnInstruction(exp));
		} else {
			instructionStack.peekFirst().addInstruction(new ReturnInstruction());
		}
	}

	private void processBreak(String data) {
		disallowOutsideFunction("break");
		int level;
		if (data == null) {
			level = 1;
		} else {
			Expression exp = Expression.parse(data, this).evaluate();
			if (exp instanceof IntegerLiteral) {
				long value = ((IntegerLiteral)exp).getValue();
				if (value <= 0) {
					throw new SyntaxException("break level has to be positive");
				} else if (value > Integer.MAX_VALUE) {
					throw new SystemLimitException("break level too large");
				} else {
					level = (int)value;
				}
			} else {
				throw new SyntaxException("break level has to be constant");
			}
		}
		instructionStack.peekFirst().addInstruction(new BreakInstruction(level));
	}

	private void processContinue(String data) {
		disallowOutsideFunction("continue");
		int level;
		if (data == null) {
			level = 1;
		} else {
			Expression exp = Expression.parse(data, this).evaluate();
			if (exp instanceof IntegerLiteral) {
				long value = ((IntegerLiteral)exp).getValue();
				if (value <= 0) {
					throw new SyntaxException("continue level has to be positive");
				} else if (value > Integer.MAX_VALUE) {
					throw new SystemLimitException("continue level too large");
				} else {
					level = (int)value;
				}
			} else {
				throw new SyntaxException("break level has to be constant");
			}
		}
		instructionStack.peekFirst().addInstruction(new ContinueInstruction(level));
	}

	private void processExpression(String line) {
		disallowOutsideFunction("expression");
		Expression exp = Expression.parse(line, this);
		instructionStack.peekFirst().addInstruction(new NormalExpression(exp));
	}
}
