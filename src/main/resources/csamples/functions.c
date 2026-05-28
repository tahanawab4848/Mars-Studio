/* functions.c - Function calls and recursion for C-to-MIPS demo */
int add(int a, int b) {
    return a + b;
}

int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n - 1) + fibonacci(n - 2);
}

int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}

int main() {
    int s = add(3, 4);
    int f = factorial(5);
    int fib = fibonacci(6);
    return s + f + fib;
}
