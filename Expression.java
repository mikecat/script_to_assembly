import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

public abstract class Expression {
	private static class OperatorInExpression {
		private int precedence; // 小さいほど強い
		private boolean rightAssociative;

		public OperatorInExpression(int precedence, boolean rightAssociative) {
			this.precedence = precedence;
			this.rightAssociative = rightAssociative;
		}

		public int getPrecedence() {
			return precedence;
		}
		public boolean isRightAssociative() {
			return rightAssociative;
		}
		public boolean shouldPopBefore(OperatorInExpression op) {
			return precedence < op.precedence || (precedence == op.precedence && !rightAssociative);
		}
	}

	private static class BinaryOperatorInExpression extends OperatorInExpression {
		private BinaryOperator.Kind kind;
		public BinaryOperatorInExpression(BinaryOperator.Kind kind, int precedence, boolean rightAssociative) {
			super(precedence, rightAssociative);
			this.kind = kind;
		}

		public BinaryOperator.Kind getKind() {
			return kind;
		}
	}

	private static class UnaryOperatorInExpression extends OperatorInExpression {
		private UnaryOperator.Kind kind;
		public UnaryOperatorInExpression(UnaryOperator.Kind kind, int precedence, boolean rightAssociative) {
			super(precedence, rightAssociative);
			this.kind = kind;
		}

		public UnaryOperator.Kind getKind() {
			return kind;
		}
	}

	private static boolean initialized = false;
	private static int longestOperatorSize = 3;
	private static Map<String, OperatorInExpression> binaryOperators;
	private static Map<String, OperatorInExpression> unaryOperators;
	private static OperatorInExpression functionCall = new BinaryOperatorInExpression(BinaryOperator.Kind.OP_FUNCTION_CALL, 12, false);
	private static OperatorInExpression parenthesis = new BinaryOperatorInExpression(null, 12, false);

	private static void initializeOperatorList() {
		if (initialized) return;
		binaryOperators = new HashMap<String, OperatorInExpression>();

		binaryOperators.put("@", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_ARRAY, 2, false));

		binaryOperators.put("*", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_MUL, 3, false));
		binaryOperators.put("/", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_DIV, 3, false));
		binaryOperators.put("%", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_MOD, 3, false));

		binaryOperators.put("+", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_ADD, 4, false));
		binaryOperators.put("-", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_SUB, 4, false));

		binaryOperators.put("<<", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LEFT_SHIFT, 5, false));
		binaryOperators.put(">>", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_RIGHT_SHIFT_ARITIMETIC, 5, false));
		binaryOperators.put(">>>", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_RIGHT_SHIFT_LOGICAL, 5, false));
		binaryOperators.put("^<", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LEFT_ROTATE, 5, false));
		binaryOperators.put(">^", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_RIGHT_ROTATE, 5, false));

		binaryOperators.put("&", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_BIT_AND, 6, false));

		binaryOperators.put("|", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_BIT_OR, 7, false));
		binaryOperators.put("^", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_BIT_XOR, 7, false));

		binaryOperators.put("=", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_ASSIGN, 8, true));

		binaryOperators.put(">", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_GT, 9, false));
		binaryOperators.put(">=", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_GTE, 9, false));
		binaryOperators.put("<", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LT, 9, false));
		binaryOperators.put("<=", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LTE, 9, false));
		binaryOperators.put("==", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_EQUAL, 9, false));
		binaryOperators.put("!=", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_NOT_EQUAL, 9, false));

		binaryOperators.put("&&", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LOGICAL_AND, 10, false));
		binaryOperators.put("||", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LOGICAL_OR, 10, false));

		binaryOperators.put(",", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_FUNCTION_ARGS_SEPARATOR, 11, true));

		unaryOperators = new HashMap<String, OperatorInExpression>();
		unaryOperators.put("-", new UnaryOperatorInExpression(UnaryOperator.Kind.UNARY_MINUS, 1, true));
		unaryOperators.put("+", new UnaryOperatorInExpression(UnaryOperator.Kind.UNARY_PLUS, 1, true));
		unaryOperators.put("!", new UnaryOperatorInExpression(UnaryOperator.Kind.UNARY_LOGICAL_NOT, 1, true));
		unaryOperators.put("~", new UnaryOperatorInExpression(UnaryOperator.Kind.UNARY_BIT_NOT, 1, true));
		unaryOperators.put("*", new UnaryOperatorInExpression(UnaryOperator.Kind.UNARY_DEREFERENCE, 1, true));
		unaryOperators.put("&", new UnaryOperatorInExpression(UnaryOperator.Kind.UNARY_ADDRESS, 1, true));
		unaryOperators.put("#", new UnaryOperatorInExpression(UnaryOperator.Kind.UNARY_SIZE, 1, true));

		initialized = true;
	}

	public abstract Type getType();

	public static Expression parse(String expression) {
		initializeOperatorList();
		Deque<Expression> valueStack = new LinkedList<Expression>();
		Deque<OperatorInExpression> expStack = new LinkedList<OperatorInExpression>();
		Deque<Integer> functionNestStack = new LinkedList<Integer>();

		boolean expectNumber = true;
		int functionNest = 1;
		for (int i = 0; i < expression.length(); i++) {
			Map<String, OperatorInExpression> operators = expectNumber ? unaryOperators : binaryOperators;
			// 空白文字は無視
			if (expression.substring(i, i + 1).matches("\\A\\s\\z")) continue;

			// 演算子か? (一番長い演算子を取得する)
			OperatorInExpression operator = null;
			for (int j = longestOperatorSize; j > 0; j--) {
				if (i + j > expression.length()) continue;
				String key = expression.substring(i, i + j);
				if (key.equals(",") && !expectNumber && functionNest != 0) {
					// 関数の引数以外では関数の引数の区切りを認識させない
					continue;
				}
				if ((operator = operators.get(key)) != null) {
					i += j - 1;
					break;
				}
			}
			if (operator != null) {
				// 演算子である
				while (expStack.size() > 0 && expStack.peekFirst().shouldPopBefore(operator)) {
					OperatorInExpression popOperator = expStack.removeFirst();
					if (popOperator instanceof BinaryOperatorInExpression) {
						Expression right = valueStack.removeFirst();
						Expression left = valueStack.removeFirst();
						valueStack.addFirst(new BinaryOperator(((BinaryOperatorInExpression)popOperator).getKind(), left, right));
					} else {
						Expression operand = valueStack.removeFirst();
						valueStack.addFirst(new UnaryOperator(((UnaryOperatorInExpression)popOperator).getKind(), operand));
					}
				}
				expStack.addFirst(operator);
				expectNumber = true;
				continue;
			}

			// 括弧か?
			if (expression.charAt(i) == '(') {
				if (expectNumber) {
					// 優先順位を変える括弧
					expStack.addFirst(parenthesis);
					functionNestStack.addFirst(functionNest);
					functionNest++;
					expectNumber = true;
				} else {
					// 関数呼び出し
					expStack.addFirst(functionCall);
					functionNestStack.addFirst(functionNest);
					functionNest = 0;
					expectNumber = true;
				}
				continue;
			}

			// 閉じカッコか?
			if (expression.charAt(i) == ')' && !expectNumber) {
				BinaryOperatorInExpression parenthesisFound = null;
				while (expStack.size() > 0) {
					OperatorInExpression popOperator = expStack.removeFirst();
					if (popOperator instanceof BinaryOperatorInExpression) {
						if (((BinaryOperatorInExpression)popOperator).getKind() == BinaryOperator.Kind.OP_FUNCTION_CALL ||
						((BinaryOperatorInExpression)popOperator).getKind() == null) {
							parenthesisFound = (BinaryOperatorInExpression)popOperator;
							break;
						}
						Expression right = valueStack.removeFirst();
						Expression left = valueStack.removeFirst();
						valueStack.addFirst(new BinaryOperator(((BinaryOperatorInExpression)popOperator).getKind(), left, right));
					} else {
						Expression operand = valueStack.removeFirst();
						valueStack.addFirst(new UnaryOperator(((UnaryOperatorInExpression)popOperator).getKind(), operand));
					}
				}
				if (parenthesisFound == null) {
					throw new RuntimeException("parenthesis closed before opening");
				}
				if (parenthesisFound.getKind() != null) {
					// 関数呼び出しである
					Expression right = valueStack.removeFirst();
					Expression left = valueStack.removeFirst();
					valueStack.addFirst(new BinaryOperator(BinaryOperator.Kind.OP_FUNCTION_CALL, left, right));
				}
				expectNumber = false;
				functionNest = functionNestStack.removeFirst();
				continue;
			}

			// 数値リテラルか?
			if ('0' <= expression.charAt(i) && expression.charAt(i) <= '9') {
				if (!expectNumber) {
					throw new RuntimeException("integer found where not expected");
				}
				int j = i + 1;
				while (j <= expression.length() && expression.substring(i, j).matches("\\A([0-9]+|0[bB][01]*|0[xX][0-9a-fA-F]*)\\z")) {
					j++;
				}
				j--;
				String token = expression.substring(i, j);
				i = j - 1;
				long number = 0;
				if (token.charAt(0) == '0') {
					if (token.equals("0")) {
						number = 0;
					} else if (token.charAt(1) == 'b' || token.charAt(1) == 'B') {
						number = Long.parseLong(token.substring(2), 2);
					} else if (token.charAt(1) == 'x' || token.charAt(1) == 'X') {
						number = Long.parseLong(token.substring(2), 16);
					} else {
						number = Long.parseLong(token.substring(1), 8);
					}
				} else {
					number = Long.parseLong(token, 10);
				}
				valueStack.addFirst(new IntegerLiteral(number, 4, true));
				expectNumber = false;
				continue;
			} else if (expression.charAt(i) == '\'') {
				if (!expectNumber) {
					throw new RuntimeException("character literal found where not expected");
				}
				int j = i + 1;
				for (; j < expression.length(); j++) {
					if (expression.charAt(j) == '\'') break;
				}
				if (j >= expression.length()) {
					throw new RuntimeException("character literal not ended");
				}
				// 8ビット符号なし整数
				valueStack.addFirst(new IntegerLiteral(expression.charAt(i + 1) & 0xff, 1, false));
				i = j;
				expectNumber = false;
				continue;
			}

			// 文字列リテラルか?
			if (expression.charAt(i) == '"') {
				if (!expectNumber) {
					throw new RuntimeException("string literal found where not expected");
				}
				int j = i + 1;
				for (; j < expression.length(); j++) {
					if (expression.charAt(j) == '"') break;
				}
				if (j >= expression.length()) {
					throw new RuntimeException("string literal not ended");
				}
				valueStack.addFirst(new StringLiteral(expression.substring(i + 1, j)));
				i = j;
				expectNumber = false;
				continue;
			}

			// 識別子か?
			if (expression.substring(i, i + 1).matches("\\A[_a-zA-Z]\\z")) {
				if (!expectNumber) {
					throw new RuntimeException("string literal found where not expected");
				}
				throw new RuntimeException("identifier not implemented yet");
			}

			// その他
			throw new RuntimeException("illegal character " + expression.charAt(i) + " in expression");
		}

		if (expectNumber) {
			throw new RuntimeException("required number not found in expression");
		}

		// 残りの演算子を処理する
		while (expStack.size() > 0) {
			OperatorInExpression popOperator = expStack.removeFirst();
			if (popOperator instanceof BinaryOperatorInExpression) {
				if (((BinaryOperatorInExpression)popOperator).getKind() == BinaryOperator.Kind.OP_FUNCTION_CALL ||
				((BinaryOperatorInExpression)popOperator).getKind() == null) {
					throw new RuntimeException("parenthesis opened but not closed");
				}
				Expression right = valueStack.removeFirst();
				Expression left = valueStack.removeFirst();
				valueStack.addFirst(new BinaryOperator(((BinaryOperatorInExpression)popOperator).getKind(), left, right));
			} else {
				Expression operand = valueStack.removeFirst();
				valueStack.addFirst(new UnaryOperator(((UnaryOperatorInExpression)popOperator).getKind(), operand));
			}
		}
		return valueStack.removeFirst();
	}
}
