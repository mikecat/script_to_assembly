import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class ScriptParser {
	private String[] libraryDir = new String[0];
	private boolean debug = false;

	private List<Variable> variableDefinitionList;
	private List<Function> functionDefinitionList;

	private boolean isInFunction;
	private FunctionBuilder currentFunction;

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
	}

	public boolean parse(BufferedReader br, String fileName, int ttl) {
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
					if (data == null) {
						throw new SyntaxException("file name to include not found");
					}
					if (!include(fileName, lineCount, ttl, data)) return false;
				} else if (action.equals("uselib")) {
					if (data == null) {
						throw new SyntaxException("file name to use not found");
					}
					if (!uselib(fileName, lineCount, ttl, data)) return false;
				} else if (action.equals("function")) {
					if (isInFunction) {
						throw new SyntaxException("nested function isn't allowed");
					} else {
						if (data == null) {
							throw new SyntaxException("function name not found");
						}
						String[] functionNameAndType = data.split("\\s+", 2);
						if (functionNameAndType.length < 2) {
							throw new SyntaxException("function return type not found");
						}
						isInFunction = true;
						currentFunction = new FunctionBuilder(
							functionNameAndType[0], Type.parseType(functionNameAndType[1]));
					}
				} else if (action.equals("endfunction")) {
					if (isInFunction) {
						// 関数の定義を確定させて登録する
						Function definedFunction = currentFunction.toFunction();
						functionDefinitionList.add(definedFunction);
						currentFunction = null;
						isInFunction = false;
					} else {
						throw new SyntaxException("endfunction without function");
					}
				} else if (action.equals("var")) {
					if (data == null) {
						throw new SyntaxException("variable name not found");
					}
					String[] variableNameAndType = data.split("\\s+", 2);
					if (variableNameAndType.length < 2) {
						throw new SyntaxException("variable type not found");
					}
					Variable var = new Variable(variableNameAndType[0], Type.parseType(variableNameAndType[1]));
					if (isInFunction) {
						currentFunction.addVariable(var);
					} else {
						variableDefinitionList.add(var);
					}
				} else {
					// キーワードが無かったので、式とみなす
					if (isInFunction) {
						Expression exp = Expression.parse(line);
						currentFunction.addInstruction(new NormalExpression(exp));
					} else {
						throw new SyntaxException("expression isn't allowed outside function");
					}
				}
			}
		} catch (Exception e) {
			System.err.println("following error occured at file " + fileName + ", line " + lineCount);
			if (debug) e.printStackTrace(); else System.err.println(e);
			return false;
		}
		return true;
	}

	private boolean include(String fileName, int lineNumber, int ttl, String data) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(data));
		if (!parse(br, data, ttl - 1)) {
			System.err.println("... included from file " + fileName + ", line " + lineNumber);
			br.close();
			return false;
		}
		br.close();
		return true;
	}

	private boolean uselib(String fileName, int lineNumber, int ttl, String data) throws IOException {
		for (int i = 0; i < libraryDir.length; i++) {
			File file = new File(libraryDir[i], data);
			if (file.exists()) {
				return include(fileName, lineNumber, ttl, file.getPath());
			}
		}
		throw new SyntaxException("file " + data + " not found in library path(es)");
	}
}
