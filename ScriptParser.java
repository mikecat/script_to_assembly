import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;

public class ScriptParser {
	private String[] libraryDir = new String[0];
	private boolean debug = false;

	private List<Variable> variableDefinitionList;
	private List<Function> functionDefinitionList;

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
		variableDefinitionList = new ArrayList<Variable>();
		functionDefinitionList = new ArrayList<Function>();
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
				} else if (action.equals("loop")) {
					processLoop();
				} else if (action.equals("endloop")) {
					processEndloop();
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
			isInFunction = true;
			currentFunction = new FunctionBuilder(nameAndType[0], DataType.parse(nameAndType[1]));
			instructionStack.clear();
			instructionStack.addFirst(currentFunction);
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
		Variable var = new Variable(nameAndType[0], DataType.parse(nameAndType[1]),
			isInFunction ? Variable.Kind.LOCAL_VARIABLE : Variable.Kind.GLOBAL_VARIABLE);
		if (isInFunction) {
			currentFunction.addVariable(var);
		} else {
			variableDefinitionList.add(var);
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
		Variable var = new Variable(nameAndType[0], DataType.parse(nameAndType[1]), Variable.Kind.ARGUMENT);
		currentFunction.addVariable(var);
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

	private void processExpression(String line) {
		disallowOutsideFunction("expression");
		Expression exp = Expression.parse(line);
		instructionStack.peekFirst().addInstruction(new NormalExpression(exp));
	}
}
