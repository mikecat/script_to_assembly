import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class ScriptParser {
	private String[] libraryDir = new String[0];
	private boolean debug = false;

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

	public void resetParseStatus() {
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
				String[] actionAndData = line.split("\\s", 2);
				String action = actionAndData.length > 0 ? actionAndData[0] : "";
				String data = actionAndData.length > 1 ? actionAndData[1] : null;

				// 指示に従って動く
				if (action.equals("include")) {
					if (!include(fileName, lineCount, ttl, data)) return false;
				} else if (action.equals("uselib")) {
					if (!uselib(fileName, lineCount, ttl, data)) return false;
				} else if (action.equals("function")) {
					if (isInFunction) {
						throw new SyntaxException("nested function isn't allowed");
					} else {
						String[] functionNameAndType = data.split("\\s", 2);
						isInFunction = true;
						currentFunction = new FunctionBuilder(functionNameAndType[0],
							functionNameAndType.length > 1 ? Type.parseType(functionNameAndType[1]) : null);
					}
				} else if (action.equals("endfunction")) {
					if (isInFunction) {
						isInFunction = false;
					} else {
						throw new SyntaxException("endfunction without function");
					}
				} else {
					// キーワードが無かったので、式とみなす
					if (isInFunction) {
						Expression exp = Expression.parse(line);
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
