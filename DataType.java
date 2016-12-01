public abstract class DataType {
	public static DataType parse(String data) {
		String trimmedData = data.trim();
		if (trimmedData.startsWith("*")) {
			// ポインタ
			DataType innerType = parse(trimmedData.substring(1));
			return new PointerType(innerType);
		} else if (trimmedData.startsWith("[")) {
			// 配列
			// 要素数が書かれている部分の範囲を求める
			int arrayClose = trimmedData.indexOf(']');
			if (arrayClose < 0) {
				throw new SyntaxException("[ found but corresponding ] not found");
			}
			// 要素数を取得する
			Expression arrayLength = Expression.parse(trimmedData.substring(1, arrayClose)).evaluate();
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
			DataType innerType = parse(trimmedData.substring(arrayClose + 1));
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
			} else if (trimmedData.equals("none")) {
				return new NoneType();
			} else if (trimmedData.equals("func")) {
				throw new SystemLimitException("type func is not implemented yet");
			} else {
				throw new SyntaxException("unknown type \"" + trimmedData + "\"");
			}
			return new PrimitiveType(width, signed);
		}
	}

	public abstract int getWidth();
}