import java.util.Deque;
import java.util.LinkedList;
import java.util.ArrayDeque;
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

	private static class CastOperatorInExpression extends OperatorInExpression {
		private DataType destType;
		public CastOperatorInExpression(DataType destType, int precedence, boolean rightAssociative) {
			super(precedence, rightAssociative);
			this.destType = destType;
		}

		public DataType getDestType() {
			return destType;
		}
	}

	private static boolean initialized = false;
	private static int longestOperatorSize = 3;
	private static Map<String, OperatorInExpression> binaryOperators;
	private static Map<String, OperatorInExpression> unaryOperators;
	private static OperatorInExpression functionCall = new BinaryOperatorInExpression(BinaryOperator.Kind.OP_FUNCTION_CALL, 15, false);
	private static OperatorInExpression parenthesis = new BinaryOperatorInExpression(null, 15, false);

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

		binaryOperators.put("^", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_BIT_XOR, 7, false));

		binaryOperators.put("|", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_BIT_OR, 8, false));

		binaryOperators.put(">", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_GT, 9, false));
		binaryOperators.put(">=", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_GTE, 9, false));
		binaryOperators.put("<", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LT, 9, false));
		binaryOperators.put("<=", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LTE, 9, false));

		binaryOperators.put("==", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_EQUAL, 10, false));
		binaryOperators.put("!=", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_NOT_EQUAL, 10, false));

		binaryOperators.put("&&", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LOGICAL_AND, 11, false));

		binaryOperators.put("||", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_LOGICAL_OR, 12, false));

		binaryOperators.put("=", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_ASSIGN, 13, true));

		binaryOperators.put(",", new BinaryOperatorInExpression(BinaryOperator.Kind.OP_FUNCTION_ARGS_SEPARATOR, 14, true));

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

	public static Expression parse(String expression, ScriptParser tableObject) {
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
			// キャスト演算子か？
			if (operator == null && expression.charAt(i) == '{') {
				int j = i + 1;
				Deque<Character> parenthesisStack = new ArrayDeque<>();
				parenthesisStack.addFirst('{');
				for (; j < expression.length() && !parenthesisStack.isEmpty(); j++) {
					boolean parenthesisMismatch = false;
					switch(expression.charAt(j)) {
					case '(':
					case '{':
					case '[':
						parenthesisStack.addFirst(expression.charAt(j));
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
						throw new SyntaxException("parenthesis mismatch in cast operator");
					}
				}
				if (!parenthesisStack.isEmpty()) {
					throw new SyntaxException("unterminated cast operator");
				}
				operator = new CastOperatorInExpression(
					DataType.parse(expression.substring(i + 1, j - 1), tableObject), 1, true);
				i = j - 1;
			}
			if (operator != null) {
				// 演算子である
				while (expStack.size() > 0 && expStack.peekFirst().shouldPopBefore(operator)) {
					OperatorInExpression popOperator = expStack.removeFirst();
					if (popOperator instanceof BinaryOperatorInExpression) {
						Expression right = valueStack.removeFirst();
						Expression left = valueStack.removeFirst();
						if (((BinaryOperatorInExpression)popOperator).getKind() == BinaryOperator.Kind.OP_FUNCTION_CALL) {
							valueStack.addFirst(new FunctionCallOperator(left, right));
						} else {
							valueStack.addFirst(new BinaryOperator(((BinaryOperatorInExpression)popOperator).getKind(), left, right));
						}
					} else if (popOperator instanceof UnaryOperatorInExpression) {
						Expression operand = valueStack.removeFirst();
						valueStack.addFirst(new UnaryOperator(((UnaryOperatorInExpression)popOperator).getKind(), operand));
					} else if (popOperator instanceof CastOperatorInExpression) {
						Expression operand = valueStack.removeFirst();
						valueStack.addFirst(new CastOperator(((CastOperatorInExpression)popOperator).getDestType(), operand));
					} else {
						throw new SystemLimitException("unknown operator type");
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
			if (expression.charAt(i) == ')') {
				if (!expectNumber) {
					// 引数や普通の式の後の閉じカッコ
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
						} else if (popOperator instanceof UnaryOperatorInExpression) {
							Expression operand = valueStack.removeFirst();
							valueStack.addFirst(new UnaryOperator(((UnaryOperatorInExpression)popOperator).getKind(), operand));
						} else if (popOperator instanceof CastOperatorInExpression) {
							Expression operand = valueStack.removeFirst();
							valueStack.addFirst(new CastOperator(((CastOperatorInExpression)popOperator).getDestType(), operand));
						} else {
							throw new SystemLimitException("unknown operator type");
						}
					}
					if (parenthesisFound == null) {
						throw new SyntaxException("parenthesis closed before opening");
					}
					if (parenthesisFound.getKind() != null) {
						// 関数呼び出しである
						Expression right = valueStack.removeFirst();
						Expression left = valueStack.removeFirst();
						valueStack.addFirst(new FunctionCallOperator(left, right));
					}
					expectNumber = false;
					functionNest = functionNestStack.removeFirst();
					continue;
				} else if (expStack.size() > 0 && expStack.peekFirst() == functionCall) {
					// 引数なしで関数を呼び出す時の閉じカッコ
					expStack.removeFirst();
					Expression operand = valueStack.removeFirst();
					valueStack.addFirst(new FunctionCallOperator(operand, null));
					expectNumber = false;
					functionNest = functionNestStack.removeFirst();
					continue;
				}
			}

			// 数値リテラルか?
			if ('0' <= expression.charAt(i) && expression.charAt(i) <= '9') {
				if (!expectNumber) {
					throw new SyntaxException("integer found where not expected");
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
				valueStack.addFirst(new IntegerLiteral(number, DataType.getSystemIntSize(), true));
				expectNumber = false;
				continue;
			} else if (expression.charAt(i) == '\'') {
				if (!expectNumber) {
					throw new SyntaxException("character literal found where not expected");
				}
				int j = i + 1;
				for (; j < expression.length(); j++) {
					if (expression.charAt(j) == '\'') break;
				}
				if (j >= expression.length()) {
					throw new SyntaxException("character literal not ended");
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
					throw new SyntaxException("string literal found where not expected");
				}
				int j = i + 1;
				for (; j < expression.length(); j++) {
					if (expression.charAt(j) == '"') break;
				}
				if (j >= expression.length()) {
					throw new SyntaxException("string literal not ended");
				}
				valueStack.addFirst(new StringLiteral(expression.substring(i + 1, j)));
				i = j;
				expectNumber = false;
				continue;
			}

			// 識別子か?
			if (expression.substring(i, i + 1).matches("\\A[_a-zA-Z]\\z")) {
				if (!expectNumber) {
					throw new SyntaxException("identifier found where not expected");
				}
				int j = i + 1;
				for (; j < expression.length(); j++) {
					if (!expression.substring(j, j + 1).matches("\\A[_a-zA-Z0-9]\\z")) break;
				}
				String identifierName = expression.substring(i, j);
				Identifier var = tableObject.lookupIdentifier(identifierName);
				if (var == null) {
					throw new SyntaxException("identifier " + identifierName + " is not declared");
				}
				valueStack.addFirst(new VariableAccess(var));
				i = j - 1;
				expectNumber = false;
				continue;
			}

			// その他
			throw new SyntaxException("illegal character " + expression.charAt(i) + " in expression");
		}

		if (expectNumber) {
			throw new SyntaxException("required number not found in expression");
		}

		// 残りの演算子を処理する
		while (expStack.size() > 0) {
			OperatorInExpression popOperator = expStack.removeFirst();
			if (popOperator instanceof BinaryOperatorInExpression) {
				if (((BinaryOperatorInExpression)popOperator).getKind() == BinaryOperator.Kind.OP_FUNCTION_CALL ||
				((BinaryOperatorInExpression)popOperator).getKind() == null) {
					throw new SyntaxException("parenthesis opened but not closed");
				}
				Expression right = valueStack.removeFirst();
				Expression left = valueStack.removeFirst();
				valueStack.addFirst(new BinaryOperator(((BinaryOperatorInExpression)popOperator).getKind(), left, right));
			} else if (popOperator instanceof UnaryOperatorInExpression) {
				Expression operand = valueStack.removeFirst();
				valueStack.addFirst(new UnaryOperator(((UnaryOperatorInExpression)popOperator).getKind(), operand));
			} else if (popOperator instanceof CastOperatorInExpression) {
				Expression operand = valueStack.removeFirst();
				valueStack.addFirst(new CastOperator(((CastOperatorInExpression)popOperator).getDestType(), operand));
			} else {
				throw new SystemLimitException("unknown operator type");
			}
		}
		return valueStack.removeFirst();
	}

	public abstract DataType getDataType();
	public abstract Expression evaluate();
}
