import java.util.Deque;
import java.util.ArrayDeque;

public abstract class DataType {
	private static int systemIntSize = 4;
	private static int pointerSize = 4;
	private static int functionSize = 4;

	public static void setSystemIntSize(int systemIntSize) {
		DataType.systemIntSize = systemIntSize;
	}

	public static int getSystemIntSize() {
		return systemIntSize;
	}

	public static void setPointerSize(int pointerSize) {
		DataType.pointerSize = pointerSize;
	}

	public static int getPointerSize() {
		return pointerSize;
	}

	public static void setFunctionSize(int functionSize) {
		DataType.functionSize = functionSize;
	}

	public static int getFunctionSize() {
		return functionSize;
	}

	public static DataType parse(String data, ScriptParser tableObject) {
		String trimmedData = data.trim();
		if (trimmedData.startsWith("*")) {
			// ポインタ
			DataType innerType = parse(trimmedData.substring(1), tableObject);
			return new PointerType(innerType);
		} else if (trimmedData.startsWith("[")) {
			// 配列
			// 要素数が書かれている部分の範囲を求める
			int arrayClose = trimmedData.indexOf(']');
			if (arrayClose < 0) {
				throw new SyntaxException("[ found but corresponding ] not found");
			}
			// 要素数を取得する
			Expression arrayLength = Expression.parse(trimmedData.substring(1, arrayClose), tableObject).evaluate();
			if (!(arrayLength instanceof IntegerLiteral)) {
				throw new SyntaxException("array length must be integer constant");
			}
			long arrayLengthValue = ((IntegerLiteral)arrayLength).getValue();
			if (arrayLengthValue <= 0) {
				throw new SyntaxException("array length must be positive");
			}
			if (arrayLengthValue > Integer.MAX_VALUE) {
				throw new SystemLimitException("array length too big");
			}
			// 配列型を作成して返す
			DataType innerType = parse(trimmedData.substring(arrayClose + 1), tableObject);
			return new ArrayType(innerType, (int)arrayLengthValue);
		} else {
			// 基本型
			int width;
			boolean signed;
			if (trimmedData.equals("int8")) {
				width = 1;
				signed = true;
			} else if (trimmedData.equals("uint8") || trimmedData.equals("byte") || trimmedData.equals("char")) {
				width = 1;
				signed = false;
			} else if (trimmedData.equals("int16")) {
				width = 2;
				signed = true;
			} else if (trimmedData.equals("uint16")) {
				width = 2;
				signed = false;
			} else if (trimmedData.equals("int32") || trimmedData.equals("int")) {
				width = 4;
				signed = true;
			} else if (trimmedData.equals("uint32") || trimmedData.equals("uint")) {
				width = 4;
				signed = false;
			} else if (trimmedData.equals("sysint")) {
				width = DataType.getSystemIntSize();
				signed = true;
			} else if (trimmedData.equals("sysuint")) {
				width = DataType.getSystemIntSize();
				signed = false;
			} else if (trimmedData.equals("ptrint")) {
				width = DataType.getPointerSize();
				signed = true;
			} else if (trimmedData.equals("ptruint")) {
				width = DataType.getPointerSize();
				signed = false;
			} else if (trimmedData.equals("funcint")) {
				width = DataType.getFunctionSize();
				signed = true;
			} else if (trimmedData.equals("funcuint")) {
				width = DataType.getFunctionSize();
				signed = false;
			} else if (trimmedData.equals("none")) {
				return new NoneType();
			} else if (trimmedData.startsWith("func")) {
				String funcType = trimmedData.substring(4).trim();
				if (funcType.startsWith("(")) {
					Deque<Character> parenthesisStack = new ArrayDeque<>();
					parenthesisStack.addFirst('(');
					int j = 1;
					for (; j < funcType.length() && !parenthesisStack.isEmpty(); j++) {
						boolean parenthesisMismatch = false;
						switch(funcType.charAt(j)) {
						case '(':
						case '{':
						case '[':
							parenthesisStack.addFirst(funcType.charAt(j));
							break;
						case ')':
							parenthesisMismatch = (parenthesisStack.removeFirst() != '(');
							break;
						case '}':
							parenthesisMismatch = (parenthesisStack.removeFirst() != '{');
							break;
						case ']':
							parenthesisMismatch = (parenthesisStack.removeFirst() != '[');
							break;
						}
						if (parenthesisMismatch) {
							throw new SyntaxException("parenthesis mismatch in type declaration");
						}
					}
					DataType returnType = parse(funcType.substring(1, j - 1), tableObject);
					return new FunctionType(returnType);
				} else {
					throw new SyntaxException(" no return type for func type specified");
				}
			} else {
				throw new SyntaxException("unknown type \"" + trimmedData + "\"");
			}
			return new PrimitiveType(width, signed);
		}
	}

	public abstract int getWidth();
}
