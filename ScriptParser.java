import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class ScriptParser {

	public ScriptParser() {
	}

	public boolean parse(BufferedReader br, String fileName, int ttl) {
		if (ttl <= 0) {
			throw new IllegalStateException("TTL expired when trying to parse " + fileName);
		}
		String line;
		int lineCount = 1;
		try {
			for (; (line = br.readLine()) != null; lineCount++) {
				// 指示と内容に分割する
				String[] actionAndData = line.split("\\s", 2);
				String action = actionAndData.length > 0 ? actionAndData[0] : "";
				String data = actionAndData.length > 1 ? actionAndData[1] : null;

				// 指示に従って動く
				if (action.equals("include")) {
					if (!include(fileName, lineCount, ttl, data)) return false;
				} else {
					// キーワードが無かったので、式とみなす
				}
			}
		} catch (Exception e) {
			System.err.println("following error occured at file " + fileName + ", line " + lineCount);
			System.err.println(e);
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

}
