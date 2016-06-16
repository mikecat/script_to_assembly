import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;

public class ScriptToAssembly {
	private String inputFile = null;
	private String outputFile = null;
	private List<String> libraryDir = new java.util.ArrayList<String>();
	private String targetName = null;
	private int ttl = 10;

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public void addLibraryDir(String libraryDir) {
		this.libraryDir.add(libraryDir);
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public void doWork() throws Exception {
		// 入力のファイルを開く
		BufferedReader br;
		String inputFileName;
		if (inputFile == null) {
			br = new BufferedReader(new InputStreamReader(System.in));
			inputFileName = "(stdin)";
		} else {
			br = new BufferedReader(new FileReader(inputFile));
			inputFileName = inputFile;
		}
		// パース処理を実行する
		ScriptParser parser = new ScriptParser();
		parser.parse(br, inputFileName, ttl);
		// 入力のファイルを閉じる
		br.close();
	}

	public static void printHelp() {
		System.out.println("Usage: java ScriptToAssembly [options]");
		System.out.println();
		System.out.println("options:");
		System.out.println("  -i file_name : specify input file name (use stdin if not specified)");
		System.out.println("  -o file_name : specify output file name (use stdout if not specified)");
		System.out.println("  -l directory_name : add library directory");
		System.out.println("  -t target_name : specify target name");
		System.out.println("  --ttl ttl_value : specify maximum depth of include (default: 10)");
		System.out.println("  -h : print this help");
		System.out.println("  -v : print version information");
	}

	public static void printVersion() {
		System.out.println("Sorry, version information isn't available.");
	}

	public static void main(String[] args) {
		try {
			ScriptToAssembly sta = new ScriptToAssembly();
			// オプションを読み込む
			boolean wantHelp = false;
			boolean wantVersion = false;
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-i")) {
					if (i + 1 < args.length) {
						sta.setInputFile(args[++i]);
					} else {
						throw new IllegalArgumentException("file name not specified for -i");
					}
				} else if (args[i].equals("-o")) {
					if (i + 1 < args.length) {
						sta.setOutputFile(args[++i]);
					} else {
						throw new IllegalArgumentException("file name not specified for -o");
					}
				} else if (args[i].equals("-l")) {
					if (i + 1 < args.length) {
						sta.addLibraryDir(args[++i]);
					} else {
						throw new IllegalArgumentException("directory name not specified for -l");
					}
				} else if (args[i].equals("-t")) {
					if (i + 1 < args.length) {
						sta.setTargetName(args[++i]);
					} else {
						throw new IllegalArgumentException("target name not specified for -t");
					}
				} else if (args[i].equals("--ttl")) {
					if (i + 1 < args.length) {
						sta.setTtl(Integer.parseInt(args[++i]));
					} else {
						throw new IllegalArgumentException("TTL value not specified for --ttl");
					}
				} else if (args[i].equals("-h")) {
					wantHelp = true;
				} else if (args[i].equals("-v")) {
					wantVersion = true;
				}
			}

			if (wantHelp) {
				printHelp();
			} else if (wantVersion) {
				printVersion();
			} else {
				sta.doWork();
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}
