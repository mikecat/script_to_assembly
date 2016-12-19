import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

public class ScriptToAssembly {
	private List<String> inputFileName = new java.util.ArrayList<String>();
	private String outputFileName = null;
	private List<String> libraryDir = new java.util.ArrayList<String>();
	private String targetName = null;
	private int ttl = 10;
	private AssemblyGenerator asmGen = null;
	private boolean debug = false;

	public void addInputFileName(String inputFileName) {
		this.inputFileName.add(inputFileName);
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
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

	public void setAssemblyGenerator(AssemblyGenerator asmGen) {
		this.asmGen = asmGen;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void doWork() throws Exception {
		// ターゲットによって決まるデータの大きさを設定する
		if (asmGen != null) {
			DataType.setSystemIntSize(asmGen.getSystemIntSize());
			DataType.setPointerSize(asmGen.getPointerSize());
			DataType.setFunctionSize(asmGen.getFunctionSize());
		}
		// パーサを初期化する
		ScriptParser parser = new ScriptParser();
		parser.setLibraryDir(libraryDir);
		parser.setDebug(debug);
		if (inputFileName.isEmpty()) {
			// 標準入力を処理する
			parser.parse(new BufferedReader(new InputStreamReader(System.in)), null, ttl);
		} else {
			Iterator<String> itr = inputFileName.iterator();
			while (itr.hasNext()) {
				// 入力のファイルを開く
				File inputFile = new File(itr.next());
				BufferedReader br = new BufferedReader(new FileReader(inputFile));
				// パース処理を実行する
				parser.parse(br, inputFile, ttl);
				// 入力のファイルを閉じる
				br.close();
			}
		}

		// アセンブリ言語を出力する
		if (asmGen != null) {
			Writer writer;
			if (outputFileName == null) {
				writer = new OutputStreamWriter(System.out);
			} else {
				writer = new FileWriter(outputFileName);
			}
			asmGen.generateAssembly(new BufferedWriter(writer),
				parser.getStaticVariableDefinitionList(),
				parser.getFunctionDefinitionList());
		}
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
		System.out.println("  --debug : turn on stack trace");
		System.out.println("  -h : print this help");
		System.out.println("  -v : print version information");
	}

	public static void printVersion() {
		System.out.println("Sorry, version information isn't available.");
	}

	public static void main(String[] args) {
		boolean debug = false;
		try {
			ScriptToAssembly sta = new ScriptToAssembly();
			// オプションを読み込む
			boolean wantHelp = false;
			boolean wantVersion = false;
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-i")) {
					if (i + 1 < args.length) {
						sta.addInputFileName(args[++i]);
					} else {
						throw new IllegalArgumentException("file name not specified for -i");
					}
				} else if (args[i].equals("-o")) {
					if (i + 1 < args.length) {
						sta.setOutputFileName(args[++i]);
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
				} else if (args[i].equals("--debug")) {
					sta.setDebug(true);
					debug = true;
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
				sta.setAssemblyGenerator(new SimpleIA32Generator());
				sta.doWork();
			}
		} catch (Exception e) {
			if (debug) e.printStackTrace(); else System.err.println(e);
		}
	}
}
